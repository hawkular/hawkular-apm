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
package org.hawkular.btm.tests.performance;

import org.hawkular.btm.server.elasticsearch.AnalyticsServiceElasticsearch;
import org.hawkular.btm.server.elasticsearch.BusinessTransactionServiceElasticsearch;
import org.hawkular.btm.server.elasticsearch.ConfigurationServiceElasticsearch;
import org.hawkular.btm.server.elasticsearch.ElasticsearchClient;

/**
 * @author gbrown
 */
public class ServicesManagerElasticsearch implements ServicesManager {

    private ElasticsearchClient client;
    private AnalyticsServiceElasticsearch analyticsService;
    private ConfigurationServiceElasticsearch configService;
    private BusinessTransactionServiceElasticsearch businessTransactionService;

    /* (non-Javadoc)
     * @see org.hawkular.btm.tests.performance.ServicesManager#getName()
     */
    @Override
    public String getName() {
        return "Elasticsearch";
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.tests.performance.ServicesManager#init()
     */
    @Override
    public ServicesManager init() throws Exception {
        if (!System.getProperties().containsKey("hawkular-btm.data.dir")) {
            System.setProperty("hawkular-btm.data.dir", "target/data");
        }

        client = new ElasticsearchClient();
        client.init();

        configService = new ConfigurationServiceElasticsearch();
        configService.setElasticsearchClient(client);

        analyticsService = new AnalyticsServiceElasticsearch();
        analyticsService.setElasticsearchClient(client);
        analyticsService.setConfigurationService(configService);

        businessTransactionService = new BusinessTransactionServiceElasticsearch();
        businessTransactionService.setElasticsearchClient(client);

        return this;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.tests.performance.ServicesManager#getAnalyticsService()
     */
    @Override
    public AnalyticsService getAnalyticsService() {
        return analyticsService;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.tests.performance.ServicesManager#getBusinessTransactionService()
     */
    @Override
    public BusinessTransactionService getTraceService() {
        return businessTransactionService;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.tests.performance.ServicesManager#clear()
     */
    @Override
    public void clear() throws Exception {
        analyticsService.clear(null);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.tests.performance.ServicesManager#close()
     */
    @Override
    public void close() throws Exception {
        client.close();
    }

}
