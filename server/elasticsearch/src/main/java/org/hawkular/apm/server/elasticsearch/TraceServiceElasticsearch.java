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
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apmserver.elasticsearch.log.MsgLogger;

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

    @Inject
    private ElasticsearchClient client;

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
     * @see org.hawkular.apm.api.services.BusinessTransactionService#get(java.lang.String,java.lang.String)
     */
    @Override
    public Trace get(String tenantId, String id) {
        Trace ret = null;

        GetResponse response = client.getElasticsearchClient().prepareGet(
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
            msgLog.tracef("Get trace with id[%s] is: %s", id, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.BusinessTransactionService#query(java.lang.String,
     *          org.hawkular.apm.api.services.BusinessTransactionQuery)
     */
    @Override
    public List<Trace> query(String tenantId, Criteria criteria) {
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
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "startTime", "businessTransaction");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
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
    public void storeTraces(String tenantId, List<Trace> traces)
            throws StoreException {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        try {
            for (int i = 0; i < traces.size(); i++) {
                Trace trace = traces.get(i);
                String json = mapper.writeValueAsString(trace);

                if (msgLog.isTraceEnabled()) {
                    msgLog.tracef("Storing trace: %s", json);
                }

                bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
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
            client.getElasticsearchClient().admin().indices().prepareDelete(index).execute().actionGet();
            client.clear(tenantId);
        } catch (IndexMissingException ime) {
            // Ignore
        }
    }

}
