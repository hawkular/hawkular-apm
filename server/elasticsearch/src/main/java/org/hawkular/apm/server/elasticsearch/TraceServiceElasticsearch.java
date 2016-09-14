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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    /**  */
    private static final String START_TIME_FIELD = "startTime";
    /**  */
    private static final String NODES_FIELD = "nodes";
    /**  */
    private static final String PRINCIPAL_FIELD = "principal";
    /**  */
    private static final String ID_FIELD = "id";
    /**  */
    private static final String HOST_NAME_FIELD = "hostName";
    /**  */
    private static final String HOST_ADDRESS_FIELD = "hostAddress";
    /**  */
    private static final String BUSINESS_TRANSACTION_FIELD = "businessTransaction";
    /**  */
    private static final String PROPERTIES_FIELD = "properties";

    /**  */
    public static final String TRACE_TYPE = "trace";

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
            for (int i=0; i < ret.getNodes().size(); i++) {
                Node node = ret.getNodes().get(i);
                processConnectedNode(tenantId, node, new StringBuilder(ret.getId()).append(':').append(i));
            }
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
     * This method recursively processes the supplied node to identify
     * other trace fragments that are related, building up the end to
     * end trace as it goes.
     *
     * @param tenantId The tenant id
     * @param root The node
     * @param nodePath The node's path
     */
    protected void processConnectedNode(String tenantId, Node node, StringBuilder nodePath) {

        if (node.containerNode()) {

            for (int i=0; i < ((ContainerNode)node).getNodes().size(); i++) {
                Node n = ((ContainerNode)node).getNodes().get(i);
                processConnectedNode(tenantId, n, new StringBuilder(nodePath).append(':').append(i));
            }

            // Check if node has been referenced by one or more 'caused by' links
            Criteria criteria = new Criteria().setStartTime(100);
            criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, nodePath.toString()));
            List<Trace> fragments = searchFragments(tenantId, criteria);

            ContainerNode anchor = (ContainerNode)node;

            for (Trace tf : fragments) {
                for (int i=0; i < tf.getNodes().size(); i++) {
                    Node n = tf.getNodes().get(i);
                    if (anchor.getClass() != Producer.class) {
                        Producer p=new Producer();
                        anchor.getNodes().add(p);
                        p.getNodes().add(n);
                    } else {
                        anchor.getNodes().add(n);
                    }
                    processConnectedNode(tenantId, n, new StringBuilder(tf.getId()).append(':').append(i));
                }
            }
        }

        if (node.getClass() == Producer.class && !node.getCorrelationIds().isEmpty()) {
            // Enable unrestricted time search for now - may need to restrict if becomes too inefficient
            Criteria criteria = new Criteria().setStartTime(100);
            criteria.getCorrelationIds().addAll(node.getCorrelationIds());
            List<Trace> fragments = searchFragments(tenantId, criteria);

            for (Trace tf : fragments) {
                for (int i=0; i < tf.getNodes().size(); i++) {
                    Node n = tf.getNodes().get(i);
                    ((Producer)node).getNodes().add(n);
                    processConnectedNode(tenantId, n, new StringBuilder(tf.getId()).append(':').append(i));
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

    public static class TraceSerializer extends JsonSerializer<Trace> {

        /* (non-Javadoc)
         * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object,
         *          com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(Trace trace, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField(BUSINESS_TRANSACTION_FIELD, trace.getBusinessTransaction());
            jgen.writeStringField(HOST_ADDRESS_FIELD, trace.getHostAddress());
            jgen.writeStringField(HOST_NAME_FIELD, trace.getHostName());
            jgen.writeStringField(ID_FIELD, trace.getId());
            jgen.writeStringField(PRINCIPAL_FIELD, trace.getPrincipal());
            jgen.writeNumberField(START_TIME_FIELD, trace.getStartTime());
            jgen.writeArrayFieldStart(NODES_FIELD);
            for (Node n : trace.getNodes()) {
                jgen.writeObject(n);
            }
            jgen.writeEndArray();
            Set<Property> properties = trace.allProperties();
            jgen.writeArrayFieldStart(PROPERTIES_FIELD);
            for (Property p : properties) {
                jgen.writeObject(p);
            }
            jgen.writeEndArray();
            jgen.writeEndObject();
        }
    }

    public static class TraceDeserializer extends JsonDeserializer<Trace> {

        /* (non-Javadoc)
         * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
         */
        @Override
        public Trace deserialize(JsonParser parser, DeserializationContext context)
                throws IOException, JsonProcessingException {
            Trace trace = new Trace();
            String field = parser.nextFieldName();
            while (field != null) {
                if (field.equals(PROPERTIES_FIELD)) {
                    parser.nextValue(); // Consume START_ARRAY

                    while (parser.nextValue() == JsonToken.START_OBJECT) {
                        // Ignore, just consume Property instance, as Trace class does not
                        // retain this field, it is only used for searching in Elasticsearch
                        context.readValue(parser, Property.class);
                    }

                    parser.nextValue(); // Consume END_ARRAY
                } else if (field.equals(BUSINESS_TRANSACTION_FIELD)) {
                    trace.setBusinessTransaction(parser.nextTextValue());
                } else if (field.equals(HOST_ADDRESS_FIELD)) {
                    trace.setHostAddress(parser.nextTextValue());
                } else if (field.equals(HOST_NAME_FIELD)) {
                    trace.setHostName(parser.nextTextValue());
                } else if (field.equals(ID_FIELD)) {
                    trace.setId(parser.nextTextValue());
                } else if (field.equals(PRINCIPAL_FIELD)) {
                    trace.setPrincipal(parser.nextTextValue());
                } else if (field.equals(NODES_FIELD)) {
                    parser.nextValue(); // Consume START_ARRAY

                    while (parser.nextValue() == JsonToken.START_OBJECT) {
                        trace.getNodes().add(context.readValue(parser, Node.class));
                    }

                    parser.nextValue(); // Consume END_ARRAY
                } else if (field.equals(START_TIME_FIELD)) {
                    trace.setStartTime(parser.nextLongValue(0));
                }
                field = parser.nextFieldName();
            }

            return trace;
        }

    }
}