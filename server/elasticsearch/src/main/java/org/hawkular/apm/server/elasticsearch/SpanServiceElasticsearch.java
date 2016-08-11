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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanService;
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
                    spans.add(deserialize(searchHitFields.getSourceAsString(), Span.class));
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
        String index = client.getIndex(tenantId);

        IndicesAdminClient indices = client.getClient().admin().indices();

        boolean indexExists = indices.prepareExists(index)
                .execute()
                .actionGet()
                .isExists();

        if (indexExists) {
            indices.prepareDelete(index)
                    .execute()
                    .actionGet();

            client.clear(tenantId);
        }
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
