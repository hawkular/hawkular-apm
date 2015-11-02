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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
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
     * @see org.hawkular.btm.api.services.ConfigurationService#getCollector(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public CollectorConfiguration getCollector(String tenantId, String host, String server) {
        CollectorConfiguration config = ConfigurationLoader.getConfiguration();

        try {
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
                    BusinessTxnConfig btc = mapper.readValue(searchHitFields.getSourceAsString(),
                            BusinessTxnConfig.class);
                    if (!btc.isDeleted()) {
                        config.getBusinessTransactions().put(searchHitFields.getId(), btc);
                    }
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transaction configurations have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to retrieve business transaction configs");
            }
        }

        return config;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#updateBusinessTransaction(java.lang.String,
     *              java.lang.String, org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public void updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
            throws Exception {
        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Update business transaction config with name[%s] config=%s", name, config);
        }

        // Set last updated time
        config.setLastUpdated(System.currentTimeMillis());

        IndexRequestBuilder builder = client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                BUSINESS_TXN_CONFIG_TYPE, name).setRouting(name)
                .setSource(mapper.writeValueAsString(config));

        builder.execute().actionGet();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransaction(java.lang.String,
     *                  java.lang.String)
     */
    @Override
    public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
        BusinessTxnConfig ret = null;

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get business transaction config with name[%s]", name);
        }

        try {
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

                    // Check if config was deleted
                    if (ret.isDeleted()) {
                        ret = null;
                    }
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }

        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transaction configurations have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to retrieve business transaction config [%s]", name);
            }
        }

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Get business transaction config with name[%s] is: %s", name, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransactionSummaries(java.lang.String)
     */
    @Override
    public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
        List<BusinessTxnSummary> ret = new ArrayList<BusinessTxnSummary>();
        String index = client.getIndex(tenantId);

        try {
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
                    BusinessTxnConfig config=mapper.readValue(searchHitFields.getSourceAsString(),
                            BusinessTxnConfig.class);
                    if (!config.isDeleted()) {
                        BusinessTxnSummary summary=new BusinessTxnSummary();
                        summary.setName(searchHitFields.getId());
                        summary.setDescription(config.getDescription());
                        summary.setLevel(config.getLevel());
                        ret.add(summary);
                    }
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transaction configurations have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to retrieve business transaction summaries");
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#getBusinessTransactions(java.lang.String, long)
     */
    @Override
    public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
        Map<String,BusinessTxnConfig> ret = new HashMap<String,BusinessTxnConfig>();
        String index = client.getIndex(tenantId);

        try {
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
                    BusinessTxnConfig btxn = mapper.readValue(searchHitFields.getSourceAsString(),
                            BusinessTxnConfig.class);
                    if ((updated == 0 && !btxn.isDeleted()) || (updated > 0 && btxn.getLastUpdated() > updated)) {
                        ret.put(searchHitFields.getId(), btxn);
                    }
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transaction configurations have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to retrieve business transaction names");
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.ConfigurationService#removeBusinessTransaction(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public void removeBusinessTransaction(String tenantId, String name) throws Exception {
        BusinessTxnConfig config = new BusinessTxnConfig();
        config.setDeleted(true);
        config.setLastUpdated(System.currentTimeMillis());

        IndexRequestBuilder builder = client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                BUSINESS_TXN_CONFIG_TYPE, name).setRouting(name)
                .setSource(mapper.writeValueAsString(config));

        builder.execute().actionGet();

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Remove business transaction config with name[%s]", name);
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
