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

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.services.ConfigurationLoader;
import org.hawkular.btm.api.services.ConfigurationService;
import org.hawkular.btm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the Elasticsearch implementation of the Administration
 * Service.
 *
 * @author gbrown
 */
@Singleton
public class ConfigurationServiceElasticsearch implements ConfigurationService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    /**  */
    private static final String BUSINESS_TXN_CONFIG_TYPE = "businesstxnconfig";

    private static final ObjectMapper mapper = new ObjectMapper();

    private ElasticsearchClient client;

    /**  */
    private static int DEFAULT_RESPONSE_SIZE = 100000;

    /**  */
    private static long DEFAULT_TIMEOUT = 10000L;

    private long timeout = DEFAULT_TIMEOUT;

    private int maxResponseSize = DEFAULT_RESPONSE_SIZE;

    @PostConstruct
    public void init() {
        client = new ElasticsearchClient();
        try {
            client.init();
        } catch (Exception e) {
            msgLog.errorFailedToInitialiseElasticsearchClient(e);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getCollectorConfiguration(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public CollectorConfiguration getCollectorConfiguration(String tenantId, String host, String server) {
        CollectorConfiguration config = ConfigurationLoader.getConfiguration();
        String index = client.getIndex(tenantId);

        RefreshRequestBuilder refreshRequestBuilder =
                client.getElasticsearchClient().admin().indices().prepareRefresh(index);
        client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

        SearchResponse response = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(BUSINESS_TXN_CONFIG_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTimeout(TimeValue.timeValueMillis(timeout))
                .setSize(maxResponseSize)
                .setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
        }

        for (SearchHit searchHitFields : response.getHits()) {
            try {
                config.getBusinessTransactions().put(searchHitFields.getId(),
                        mapper.readValue(searchHitFields.getSourceAsString(),
                                BusinessTxnConfig.class));
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return config;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#updateBusinessTransactionConfig(java.lang.String,
     *              java.lang.String, org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public void updateBusinessTransactionConfig(String tenantId, String name, BusinessTxnConfig config)
            throws Exception {
        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Update business transaction config with name[%s] config=%s", name, config);
        }

        IndexRequestBuilder builder = client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                BUSINESS_TXN_CONFIG_TYPE, name).setRouting(name)
                .setSource(mapper.writeValueAsString(config));

        builder.execute().actionGet();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransactionConfig(java.lang.String,
     *                  java.lang.String)
     */
    @Override
    public BusinessTxnConfig getBusinessTransactionConfig(String tenantId, String name) {
        BusinessTxnConfig ret = null;

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get business transaction config with name[%s]", name);
        }

        String index = client.getIndex(tenantId);

        RefreshRequestBuilder refreshRequestBuilder =
                client.getElasticsearchClient().admin().indices().prepareRefresh(index);
        client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

        GetResponse response = client.getElasticsearchClient().prepareGet(
                index, BUSINESS_TXN_CONFIG_TYPE, name).setRouting(name)
                .execute()
                .actionGet();
        if (!response.isSourceEmpty()) {
            try {
                ret = mapper.readValue(response.getSourceAsString(), BusinessTxnConfig.class);
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get business transaction config with name[%s] is: %s", name, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#removeBusinessTransactionConfig(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public void removeBusinessTransactionConfig(String tenantId, String name) throws Exception {
        DeleteResponse response = client.getElasticsearchClient().prepareDelete(client.getIndex(tenantId),
                BUSINESS_TXN_CONFIG_TYPE, name).setRouting(name)
                .execute()
                .actionGet();
        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Remove business transaction config with name[%s]: %s", name, response.isFound());
        }
    }

}
