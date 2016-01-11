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
package org.hawkular.btm.server.elasticsearch;

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
import org.elasticsearch.search.SearchHit;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the Elasticsearch implementation of the Business Transaction
 * Service.
 *
 * @author gbrown
 */
public class BusinessTransactionServiceElasticsearch implements BusinessTransactionService {

    private static final MsgLogger msgLog = MsgLogger.LOGGER;

    /**  */
    public static final String BUSINESS_TRANSACTION_TYPE = "businesstransaction";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private ElasticsearchClient client;

    protected ElasticsearchClient getElasticsearchClient() {
        return client;
    }

    protected void setElasticsearchClient(ElasticsearchClient client) {
        this.client = client;
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
        return internalQuery(client, tenantId, criteria);
    }

    /**
     * This method performs the query.
     *
     * @param client The elasticsearch client
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The list of business transactions
     */
    protected static List<BusinessTransaction> internalQuery(ElasticsearchClient client, String tenantId,
            BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> ret = new ArrayList<BusinessTransaction>();

        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "startTime", "name");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(BUSINESS_TRANSACTION_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

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
                            BusinessTransaction.class));
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Query business transactions with criteria[%s] is: %s", criteria, ret);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to retrieve business transactions");
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#storeBusinessTransactions(java.lang.String,
     *                              java.util.List)
     */
    @Override
    public void storeBusinessTransactions(String tenantId, List<BusinessTransaction> businessTransactions)
            throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < businessTransactions.size(); i++) {
            BusinessTransaction btxn = businessTransactions.get(i);
            String json=mapper.writeValueAsString(btxn);

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing business transaction: %s", json);
            }

            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    BUSINESS_TRANSACTION_TYPE, btxn.getId()).setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store business transactions: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store business transactions to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing business transactions to elasticsearch");
            }
        }
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

}
