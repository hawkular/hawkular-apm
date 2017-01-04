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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.InteractionNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanService;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.hawkular.apm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Pavol Loffay
 */
public class SpanServiceElasticsearch implements SpanService {

    private static final MsgLogger log = MsgLogger.LOGGER;

    public static final String SPAN_TYPE = "span";

    private static final ObjectMapper mapper = new ObjectMapper();

    private ElasticsearchClient client = ElasticsearchClient.getSingleton();


    @Override
    public Span getSpan(String tenantId, String id) {

        GetResponse response = null;
        try {
            response = client.getClient()
                    .prepareGet(client.getIndex(tenantId), SPAN_TYPE, id)
                    .setRouting(id)
                    .execute()
                    .actionGet();
        } catch (IndexMissingException ex) {
            log.errorf("Missing span index %s", tenantId);
            return null;
        }

        Span span = null;
        if (!response.isSourceEmpty()) {
            try {
                span = deserialize(response.getSourceAsString(), Span.class);

                /**
                 * Enrich server span with client span annotations
                 */
                if (span.serverSpan() && span.url() == null) {
                    Span clientSpan = getSpan(tenantId, SpanUniqueIdGenerator.getClientId(span.getId()));
                    if (clientSpan != null && clientSpan.url() != null) {
                        BinaryAnnotation httpURLAnnotation = new BinaryAnnotation();
                        httpURLAnnotation.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
                        httpURLAnnotation.setValue(clientSpan.url().toString());

                        List<BinaryAnnotation> binaryAnnotationsWithURL = new ArrayList<>(span.getBinaryAnnotations());
                        binaryAnnotationsWithURL.add(httpURLAnnotation);
                        span = new Span(span, binaryAnnotationsWithURL, span.getAnnotations());
                    }
                }
            } catch (IOException ex) {
                log.errorFailedToParse(ex);
            }
        }

        log.tracef("Get span with id[%s] is: %s", id, span);

        return span;
    }

    @Override
    public List<Span> getChildren(String tenantId, String id) {

        List<Span> spans = new ArrayList<>();
        final String index = client.getIndex(tenantId);
        try {

            RefreshRequestBuilder refreshRequestBuilder = client.getClient()
                    .admin()
                    .indices()
                    .prepareRefresh(index);

            client.getClient()
                    .admin()
                    .indices()
                    .refresh(refreshRequestBuilder.request())
                    .actionGet();

            QueryBuilder query = QueryBuilders.termQuery("parentId", id);

            SearchRequestBuilder request = client.getClient()
                    .prepareSearch(index)
                    .setTypes(SPAN_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setQuery(query);

            SearchResponse response = request.execute()
                    .actionGet();

            for (SearchHit searchHitFields : response.getHits()) {
                try {
                    Span span = deserialize(searchHitFields.getSourceAsString(), Span.class);
                    if (!span.serverSpan()) {
                        spans.add(span);
                    }
                } catch (IOException ex) {
                    log.errorFailedToParse(ex);
                }
            }
        } catch (IndexMissingException ex) {
            log.errorf("No index[%s] found, so unable to retrieve spans", index);
        }

        log.tracef("Get children with parentId[%s] is: %s", id, spans);
        return spans;
    }

    @Override
    public void storeSpan(String tenantId, List<Span> spans) throws StoreException {
        storeSpan(tenantId, spans, span -> span.getId());
    }

    @Override
    public void storeSpan(String tenantId, List<Span> spans, Function<Span, String> spanIdSupplier)
            throws StoreException {

        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getClient().prepareBulk();

        for (Span span : spans) {
            String json;
            try {
                json = serialize(span);
            } catch(IOException ex){
                log.errorf("Failed to serialize span %s", span);
                throw new StoreException(ex);
            }

            log.tracef("Storing span: %s", json);
            // modified id is used in index
            final String modifiedId = spanIdSupplier.apply(span);

            bulkRequestBuilder.add(client.getClient()
                    .prepareIndex(client.getIndex(tenantId), SPAN_TYPE, modifiedId)
                    .setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {
            log.tracef("Failed to store spans to elasticsearch: %s", bulkItemResponses.buildFailureMessage());
            throw new StoreException(bulkItemResponses.buildFailureMessage());
        }

        log.trace("Success storing spans to elasticsearch");
    }

    @Override
    public void clear(String tenantId) {
        client.clearTenant(tenantId);
    }

    @Override
    public Trace getTraceFragment(String tenantId, String id) {
        Span span = getSpan(tenantId, id);

        if (span == null) {
            return null;
        }

        InteractionNode interactionNode = spanToNode(span);
        interactionNode.setNodes(recursiveTraceFragment(tenantId, span));

        Trace trace = spanToTrace(span);
        trace.getNodes().add(interactionNode);

        return trace;
    }

    @Override
    public Trace getTrace(String tenantId, String id) {
        Span span = getSpan(tenantId, id);
        if (span == null || span.serverSpan()) {
            // We need to check if there is a client span for the same id
            String clientId = SpanUniqueIdGenerator.getClientId(id);
            if (getSpan(tenantId, clientId) != null) {
                // Replace the top level id we are interested in with the
                // client side version
                id = clientId;
            }
        }

        Trace trace = getTraceFragment(tenantId, id);

        if (trace != null) {
            processConnectedFragment(tenantId, trace);
        }

        return trace;
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
     * @param fragment The fragment to be processed
     */
    protected void processConnectedFragment(String tenantId, Trace fragment) {

        List<Producer> producers = NodeUtil.findNodes(fragment.getNodes(), Producer.class);

        for (Producer producer : producers) {
            if (!producer.getCorrelationIds().isEmpty()) {
                List<Trace> fragments = getTraceFragments(tenantId, producer.getCorrelationIds().stream()
                        .map(correlationIdentifier -> correlationIdentifier.getValue())
                        .collect(Collectors.toList()));

                for (Trace descendant : fragments) {
                    // Attach the fragment root nodes to the producer
                    producer.getNodes().addAll(descendant.getNodes());
                    processConnectedFragment(tenantId, descendant);
                }
            }
        }
    }

    private List<Trace> getTraceFragments(String tenantId, List<String> ids) {
        List<Trace> traces = new ArrayList<>();

        for (String id: ids) {
            Trace traceFragment = getTraceFragment(tenantId, id);
            if (traceFragment != null) {
                traces.add(traceFragment);
            }
        }

        return traces;
    }

    private List<Node> recursiveTraceFragment(String tenantId, Span parent) {
        List<Span> spanChildren = getChildren(tenantId, parent.getId());

        if (spanChildren == null) {
            return Collections.emptyList();
        }

        List<Node> nodes = new ArrayList<>();

        for (Span child: spanChildren) {
            InteractionNode node = spanToNode(child);

            if (!parent.clientSpan()) {
                nodes.add(node);
            }

            if (!child.clientSpan()) {
                node.setNodes(recursiveTraceFragment(tenantId, child));
            }
        }

        return nodes;
    }

    private Trace spanToTrace(Span span) {
        if (span == null) {
            throw new NullPointerException();
        }

        Trace trace = new Trace();
        trace.setTraceId(span.getTraceId());
        trace.setFragmentId(span.getId());
        trace.setTimestamp(span.getTimestamp() != null ? span.getTimestamp() : 0);
        trace.setHostAddress(span.ipv4());

        return trace;
    }

    private InteractionNode spanToNode(Span span) {
        String url = span.url() != null ? span.url().getPath() : null;

        InteractionNode node;
        if (span.binaryAnnotationMapping().getComponentType() != null) {
            node = new Component(url, span.binaryAnnotationMapping().getComponentType());
        } else if (span.serverSpan()) {
            node = new Consumer(url, span.binaryAnnotationMapping().getEndpointType());
            node.addInteractionCorrelationId(span.getId());
        }else if (span.clientSpan()) {
            node = new Producer(url, span.binaryAnnotationMapping().getEndpointType());
            node.addInteractionCorrelationId(span.getId());
        } else {
            node = new Component(url, null);
        }

        node.setProperties(new HashSet<>(span.binaryAnnotationMapping().getProperties()));
        node.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, span.service()));

        if (span.getTimestamp() != null) {
            node.setTimestamp(span.getTimestamp());
        }
        if (span.getDuration() != null) {
            node.setDuration(span.getDuration());
        }

        return node;
    }

    private <T> T deserialize(String json, Class<T> type) throws IOException {
        JsonParser parser = mapper.getFactory().createParser(json);

        return parser.readValueAs(type);
    }

    private String serialize(Object object) throws IOException {
        StringWriter out = new StringWriter();

        JsonGenerator gen = mapper.getFactory().createGenerator(out);
        gen.writeObject(object);

        gen.close();
        out.close();

        return out.toString();
    }
}
