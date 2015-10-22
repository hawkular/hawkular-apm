/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.server.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

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
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the Elasticsearch implementation of the Business Transaction
 * Service.
 *
 * @author gbrown
 */
@Singleton
public class BusinessTransactionServiceElasticsearch implements BusinessTransactionService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    /**  */
    private static final String BUSINESS_TRANSACTION_TYPE = "businesstransaction";

    private static final ObjectMapper mapper = new ObjectMapper();

    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        client = new ElasticsearchClient();
        try {
            client.init();
        } catch (Exception e) {
            msgLog.errorFailedToInitialiseElasticsearchClient(e);
        }
    }

    /**
     * This method stores the supplied business transactions.
     *
     * @param tenantId The tenant id
     * @param btxns The list of transactions
     */
    protected void store(String tenantId, List<BusinessTransaction> btxns) {
        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < btxns.size(); i++) {
            BusinessTransaction btxn = btxns.get(i);
            try {
                bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                        BUSINESS_TRANSACTION_TYPE, btxn.getId()).setSource(mapper.writeValueAsString(btxn)));
            } catch (JsonProcessingException e) {
                msgLog.error("Failed to store business transaction", e);
            }
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {
            msgLog.error("Failed to store business transactions: " + bulkItemResponses.buildFailureMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String,java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        BusinessTransaction ret = null;

        GetResponse response = client.getElasticsearchClient().prepareGet(
                client.getIndex(tenantId), BUSINESS_TRANSACTION_TYPE, id).setRouting(id)
                .execute()
                .actionGet();
        if (!response.isSourceEmpty()) {
            try {
                ret = mapper.readValue(response.getSourceAsString(), BusinessTransaction.class);
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get business transaction with id[%s] is: %s", id, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
     *          org.hawkular.btm.api.services.BusinessTransactionQuery)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> ret = new ArrayList<BusinessTransaction>();

        String index = client.getIndex(tenantId);

        RefreshRequestBuilder refreshRequestBuilder =
                client.getElasticsearchClient().admin().indices().prepareRefresh(index);
        client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

        long startTime = criteria.getStartTime();
        long endTime = criteria.getEndTime();

        if (endTime == 0) {
            endTime = System.currentTimeMillis();
        }

        if (startTime == 0) {
            // Set to 1 hour before end time
            startTime = endTime - 3600000;
        }

        BoolQueryBuilder b2 = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("startTime").from(startTime).to(endTime));

        FilterBuilder filter = null;
        if (criteria.getName() != null) {
            if (criteria.getName().trim().length() == 0) {
                filter = FilterBuilders.missingFilter("name");
            } else {
                b2 = b2.must(QueryBuilders.termQuery("name", criteria.getName()));
            }
        }

        if (!criteria.getCorrelationIds().isEmpty()) {
            for (CorrelationIdentifier id : criteria.getCorrelationIds()) {
                b2.must(QueryBuilders.termQuery("value", id.getValue()));
                /* HWKBTM-186
                b2 = b2.must(QueryBuilders.nestedQuery("nodes.correlationIds", // Path
                        QueryBuilders.boolQuery()
                                .must(QueryBuilders.matchQuery("correlationIds.scope", id.getScope()))
                                .must(QueryBuilders.matchQuery("correlationIds.value", id.getValue()))));
                 */
            }
        }

        if (!criteria.getProperties().isEmpty()) {
            for (String key : criteria.getProperties().keySet()) {
                b2 = b2.must(QueryBuilders.matchQuery("properties." + key, criteria.getProperties().get(key)));
            }
        }

        SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(BUSINESS_TRANSACTION_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setSize(criteria.getMaxResponseSize())
                .setQuery(b2);

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
                        BusinessTransaction.class));
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Query business transactions with criteria[%s] is: %s", criteria, ret);
        }

        return ret;
    }

    /**
     * This method clears the Elasticsearch database, and is currently only intended for
     * testing purposes.
     *
     * @param tenantId The optional tenant id
     */
    protected void clear(String tenantId) {
        String index = client.getIndex(tenantId);

        client.getElasticsearchClient().admin().indices().prepareDelete(index).execute().actionGet();
    }

    /**
     * This method closes the Elasticsearch client.
     */
    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }

}
