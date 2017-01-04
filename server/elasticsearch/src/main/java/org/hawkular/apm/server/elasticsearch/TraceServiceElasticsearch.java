/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.server.api.services.SpanService;
import org.hawkular.apm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * This class provides the Elasticsearch implementation of the Trace
 * Service.
 *
 * @author gbrown
 */
public class TraceServiceElasticsearch implements TraceService {

    private static final MsgLogger msgLog = MsgLogger.LOGGER;

    public static final String TRACE_TYPE = "trace";

    private static final int MAX_FRAGMENTS_PER_TRACE = 1000;

    private static final ObjectMapper mapper = new ObjectMapper();

    private SpanService spanService;

    private ElasticsearchClient client = ElasticsearchClient.getSingleton();

    public TraceServiceElasticsearch() {}

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Trace.class, new TraceSerializer());
        module.addDeserializer(Trace.class, new TraceDeserializer());
        mapper.registerModule(module);
    }

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

    protected List<Trace> getFragmentsForTraceId(String tenantId, String traceId) {
        List<Trace> ret = Collections.emptyList();

        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder = client.getClient().admin().indices().prepareRefresh(index);
            client.getClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = QueryBuilders.boolQuery()
                    .must(QueryBuilders.termQuery("traceId", traceId));

            SearchRequestBuilder request = client.getClient().prepareSearch(index)
                    .setTypes(TRACE_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setSize(MAX_FRAGMENTS_PER_TRACE)
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            ret = new ArrayList<Trace>((int)response.getHits().getTotalHits());

            for (SearchHit searchHitFields : response.getHits()) {
                try {
                    ret.add(mapper.readValue(searchHitFields.getSourceAsString(),
                            Trace.class));
                } catch (IOException e) {
                    msgLog.errorFailedToParse(e);
                }
            }

            msgLog.tracef("Query fragments with traceId[%s] is: %s", traceId, ret);
        } catch (org.elasticsearch.indices.IndexMissingException ime) {
            // Ignore, as means that no traces have
            // been stored yet
            msgLog.tracef("No index found, so unable to retrieve traces");
        } catch (org.elasticsearch.action.search.SearchPhaseExecutionException spee) {
            // Ignore, as occurs when mapping not established (i.e. empty
            // repository) and performing query with a sort order
            msgLog.tracef("Failed to get fragments", spee);
        }

        return ret;
    }

    @Override
    public Trace getTrace(String tenantId, String id) {
        List<Trace> fragments = getFragmentsForTraceId(tenantId, id);
        Trace ret = fragments.stream().filter(f -> f.getFragmentId().equals(id)).findFirst().orElse(null);

        if (ret != null) {
            for (int i=0; i < ret.getNodes().size(); i++) {
                Node node = ret.getNodes().get(i);
                processConnectedNode(fragments, ret, node, new StringBuilder(ret.getFragmentId()).append(':').append(i));
            }
        } else if (spanService != null) {
            ret = spanService.getTrace(tenantId, id);
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get trace with id[%s] is: %s", id, ret);
        }

        return ret;
    }

    /**
     * This method recursively processes the supplied node to identify
     * other trace fragments that are related, building up the end to
     * end trace as it goes.
     *
     * @param fragments The list of fragments for the traceId
     * @param trace The trace being constructed
     * @param root The node
     * @param nodePath The node's path
     */
    protected void processConnectedNode(List<Trace> fragments, Trace trace, Node node, StringBuilder nodePath) {

        if (node.containerNode()) {

            for (int i=0; i < ((ContainerNode)node).getNodes().size(); i++) {
                Node n = ((ContainerNode)node).getNodes().get(i);
                processConnectedNode(fragments, trace, n, new StringBuilder(nodePath).append(':').append(i));
            }

            // Check if node has been referenced by one or more 'caused by' links
            CorrelationIdentifier cid = new CorrelationIdentifier(Scope.CausedBy, nodePath.toString());
            List<Trace> causedByFragments = fragments.stream().filter(f -> {
                return !f.getNodes().isEmpty() && f.getNodes().get(0).getCorrelationIds().contains(cid);
            }).collect(Collectors.toList());

            ContainerNode anchor = (ContainerNode)node;

            for (Trace tf : causedByFragments) {
                for (int i=0; i < tf.getNodes().size(); i++) {
                    Node n = tf.getNodes().get(i);
                    if (anchor.getClass() != Producer.class) {
                        Producer p=new Producer();
                        anchor.getNodes().add(p);
                        p.getNodes().add(n);
                    } else {
                        anchor.getNodes().add(n);
                    }
                    processConnectedNode(fragments, trace, n, new StringBuilder(tf.getFragmentId()).append(':').append(i));
                }
            }
        }

        if (node.getClass() == Producer.class && !node.getCorrelationIds().isEmpty()) {
            // Enable unrestricted time search for now - may need to restrict if becomes too inefficient
            List<Trace> correlatedFragments = fragments.stream().filter(f -> {
                return !f.getNodes().isEmpty() && f.getNodes().get(0).getCorrelationIds().stream().filter(
                        cid -> node.getCorrelationIds().contains(cid)).count() > 0;
            }).collect(Collectors.toList());

            for (Trace tf : correlatedFragments) {
                // Ensure we don't process top level trace again, if contains just a Producer
                for (int i=0; !tf.getFragmentId().equals(trace.getFragmentId()) && i < tf.getNodes().size(); i++) {
                    Node n = tf.getNodes().get(i);
                    ((Producer)node).getNodes().add(n);
                    processConnectedNode(fragments, trace, n, new StringBuilder(tf.getFragmentId()).append(':').append(i));
                }
            }
        }
    }

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

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria,
                    ElasticsearchUtil.TRANSACTION_FIELD, Trace.class);

            SearchRequestBuilder request = client.getClient().prepareSearch(index)
                    .setTypes(TRACE_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query)
                    .addSort(ElasticsearchUtil.TIMESTAMP_FIELD, SortOrder.ASC);

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
                        TRACE_TYPE, trace.getFragmentId()).setSource(json));
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

    @Override
    public void clear(String tenantId) {
        client.clearTenant(tenantId);
    }

    public static class TraceSerializer extends JsonSerializer<Trace> {

        @Override
        public void serialize(Trace trace, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField(ElasticsearchUtil.TRANSACTION_FIELD, trace.getTransaction());
            jgen.writeStringField(ElasticsearchUtil.HOST_ADDRESS_FIELD, trace.getHostAddress());
            jgen.writeStringField(ElasticsearchUtil.HOST_NAME_FIELD, trace.getHostName());
            jgen.writeStringField(ElasticsearchUtil.FRAGMENT_ID_FIELD, trace.getFragmentId());
            jgen.writeNumberField(ElasticsearchUtil.TIMESTAMP_FIELD, trace.getTimestamp());
            jgen.writeStringField(ElasticsearchUtil.TRACE_ID_FIELD, trace.getTraceId());
            jgen.writeArrayFieldStart(ElasticsearchUtil.NODES_FIELD);
            for (Node n : trace.getNodes()) {
                jgen.writeObject(n);
            }
            jgen.writeEndArray();
            Set<Property> properties = trace.allProperties();
            jgen.writeArrayFieldStart(ElasticsearchUtil.PROPERTIES_FIELD);
            for (Property p : properties) {
                jgen.writeObject(p);
            }
            jgen.writeEndArray();
            jgen.writeEndObject();
        }
    }

    public static class TraceDeserializer extends JsonDeserializer<Trace> {

        @Override
        public Trace deserialize(JsonParser parser, DeserializationContext context)
                throws IOException, JsonProcessingException {
            Trace trace = new Trace();
            String field = parser.nextFieldName();
            while (field != null) {
                if (field.equals(ElasticsearchUtil.PROPERTIES_FIELD)) {
                    parser.nextValue(); // Consume START_ARRAY

                    while (parser.nextValue() == JsonToken.START_OBJECT) {
                        // Ignore, just consume Property instance, as Trace class does not
                        // retain this field, it is only used for searching in Elasticsearch
                        context.readValue(parser, Property.class);
                    }

                    parser.nextValue(); // Consume END_ARRAY
                } else if (field.equals(ElasticsearchUtil.TRANSACTION_FIELD)) {
                    trace.setTransaction(parser.nextTextValue());
                } else if (field.equals(ElasticsearchUtil.HOST_ADDRESS_FIELD)) {
                    trace.setHostAddress(parser.nextTextValue());
                } else if (field.equals(ElasticsearchUtil.HOST_NAME_FIELD)) {
                    trace.setHostName(parser.nextTextValue());
                } else if (field.equals(ElasticsearchUtil.FRAGMENT_ID_FIELD)) {
                    trace.setFragmentId(parser.nextTextValue());
                } else if (field.equals(ElasticsearchUtil.NODES_FIELD)) {
                    parser.nextValue(); // Consume START_ARRAY

                    while (parser.nextValue() == JsonToken.START_OBJECT) {
                        trace.getNodes().add(context.readValue(parser, Node.class));
                    }

                    parser.nextValue(); // Consume END_ARRAY
                } else if (field.equals(ElasticsearchUtil.TIMESTAMP_FIELD)) {
                    trace.setTimestamp(parser.nextLongValue(0));
                } else if (field.equals(ElasticsearchUtil.TRACE_ID_FIELD)) {
                    trace.setTraceId(parser.nextTextValue());
                }
                field = parser.nextFieldName();
            }

            return trace;
        }

    }
}
