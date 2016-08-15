/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.apm.server.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.server.api.services.SpanService;
import org.hawkular.apm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the Elasticsearch implementation of the Trace
 * Service.
 *
 * @author gbrown
 */
public class TraceServiceElasticsearch implements TraceService {

    private static final MsgLogger msgLog = MsgLogger.LOGGER;

    /**  */
    public static final String TRACE_TYPE = "trace";

    private static final ObjectMapper mapper = new ObjectMapper();

    private SpanService spanService;

    private ElasticsearchClient client = ElasticsearchClient.getSingleton();

    public TraceServiceElasticsearch() {}

    @Inject
    public TraceServiceElasticsearch(SpanService spanService) {
        this.spanService = spanService;
    }

    /**
     * This method gets the elasticsearch client.
     *
     * @return The elasticsearch client
     */
    public ElasticsearchClient getElasticsearchClient() {
        return client;
    }

    /**
     * This method sets the elasticsearch client.
     *
     * @param client The elasticsearch client
     */
    public void setElasticsearchClient(ElasticsearchClient client) {
        this.client = client;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#getFragment(java.lang.String, java.lang.String)
     */
    @Override
    public Trace getFragment(String tenantId, String id) {
        Trace ret = null;

        GetResponse response = client.getClient().prepareGet(
                client.getIndex(tenantId), TRACE_TYPE, id).setRouting(id)
                .execute()
                .actionGet();
        if (!response.isSourceEmpty()) {
            try {
                ret = mapper.readValue(response.getSourceAsString(), Trace.class);
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get fragment with id[%s] is: %s", id, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#getTrace(java.lang.String, java.lang.String)
     */
    @Override
    public Trace getTrace(String tenantId, String id) {
        Trace ret = getFragment(tenantId, id);

        if (ret != null) {
            processConnectedFragment(tenantId, ret, ret, null);
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get trace with id[%s] is: %s", id, ret);
        }

        if (ret == null && spanService != null) {
            ret = spanService.getTrace(tenantId, id);
        }

        return ret;
    }

    /**
     * This method aggregates enhances the root trace, representing the
     * complete end to end instance view, with the details available from
     * a linked trace fragment that should be attached to the supplied
     * Producer node. If the producer node is null then don't merge
     * (as fragment is root) and just recursively process the fragment
     * to identify additional linked fragments.
     *
     * @param tenantId The tenant id
     * @param root The root trace
     * @param fragment The fragment to be processed
     * @param producer The producer node, or null if processing the top level fragment
     */
    protected void processConnectedFragment(String tenantId, Trace root, Trace fragment, Producer producer) {
        if (producer != null) {
            // Merge the properties associated with the fragment into the root
            root.getProperties().addAll(fragment.getProperties());

            // Attach the fragment root nodes to the producer
            producer.getNodes().addAll(fragment.getNodes());
        }

        List<Producer> producers = new ArrayList<Producer>();
        NodeUtil.findNodes(fragment.getNodes(), Producer.class, producers);

        for (Producer p : producers) {
            if (!p.getCorrelationIds().isEmpty()) {
                // Enable unrestricted time search for now - may need to restrict if becomes too inefficient
                Criteria criteria = new Criteria().setStartTime(100);
                criteria.getCorrelationIds().addAll(p.getCorrelationIds());
                List<Trace> fragments = searchFragments(tenantId, criteria);

                for (Trace tf : fragments) {
                    processConnectedFragment(tenantId, root, tf, p);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#searchFragments(java.lang.String,
     *          org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<Trace> searchFragments(String tenantId, Criteria criteria) {
        return internalQuery(client, tenantId, criteria);
    }

    /**
     * This method performs the query.
     *
     * @param client The elasticsearch client
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The list of traces
     */
    protected static List<Trace> internalQuery(ElasticsearchClient client, String tenantId,
            Criteria criteria) {
        List<Trace> ret = new ArrayList<Trace>();

        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder = client.getClient().admin().indices().prepareRefresh(index);
            client.getClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "startTime", "businessTransaction",
                    Trace.class);

            SearchRequestBuilder request = client.getClient().prepareSearch(index)
                    .setTypes(TRACE_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query)
                    .addSort("startTime", SortOrder.ASC);

            FilterBuilder filter = ElasticsearchUtil.buildFilter(criteria);
            if (filter != null) {
                request.setPostFilter(filter);
            }

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            for (SearchHit searchHitFields : response.getHits()) {
                try {
                    ret.add(mapper.readValue(searchHitFields.getSourceAsString(),
                            Trace.class));
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Query traces with criteria[%s] is: %s", criteria, ret);
            }
        } catch (org.elasticsearch.indices.IndexMissingException ime) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to retrieve traces");
            }
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException spee) {
            // Ignore, as occurs when mapping not established (i.e. empty
            // repository) and performing query with a sort order
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Failed to get fragments", spee);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#storeTraces(java.lang.String,
     *                              java.util.List)
     */
    @Override
    public void storeFragments(String tenantId, List<Trace> traces)
            throws StoreException {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getClient().prepareBulk();

        try {
            for (int i = 0; i < traces.size(); i++) {
                Trace trace = traces.get(i);
                String json = mapper.writeValueAsString(trace);

                if (msgLog.isTraceEnabled()) {
                    msgLog.tracef("Storing trace: %s", json);
                }

                bulkRequestBuilder.add(client.getClient().prepareIndex(client.getIndex(tenantId),
                        TRACE_TYPE, trace.getId()).setSource(json));
            }
        } catch (JsonProcessingException e) {
            throw new StoreException(e);
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store traces to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }

            throw new StoreException(bulkItemResponses.buildFailureMessage());

        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing traces to elasticsearch");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        String index = client.getIndex(tenantId);

        try {
            client.getClient().admin().indices().prepareDelete(index).execute().actionGet();
            client.clear(tenantId);
        } catch (IndexMissingException ime) {
            // Ignore
        }
    }

}
