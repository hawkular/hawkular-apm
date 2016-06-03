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
package org.hawkular.apm.tests.performance;

import org.hawkular.apm.api.services.AnalyticsService;
import org.hawkular.apm.server.cassandra.AnalyticsServiceCassandra;
import org.hawkular.apm.server.cassandra.CassandraClient;
import org.hawkular.apm.server.cassandra.ConfigurationServiceCassandra;

/**
 * @author gbrown
 */
public class ServicesManagerCassandra implements ServicesManager {

    private CassandraClient client;
    private AnalyticsServiceCassandra analyticsService;
    private ConfigurationServiceCassandra configService;
    private BusinessTransactionServiceCassandra businessTransactionService;

    /* (non-Javadoc)
     * @see org.hawkular.apm.tests.performance.ServicesManager#getName()
     */
    @Override
    public String getName() {
        return "Cassandra";
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.tests.performance.ServicesManager#init()
     */
    @Override
    public ServicesManager init() throws Exception {
        client = new CassandraClient();
        client.init();

        configService = new ConfigurationServiceCassandra();
        configService.setClient(client);
        configService.init();

        analyticsService = new AnalyticsServiceCassandra();
        analyticsService.setClient(client);
        analyticsService.setConfigurationService(configService);
        analyticsService.init();

        businessTransactionService = new BusinessTransactionServiceCassandra();
        businessTransactionService.setClient(client);
        businessTransactionService.init();

        return this;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.tests.performance.ServicesManager#getAnalyticsService()
     */
    @Override
    public AnalyticsService getAnalyticsService() {
        return analyticsService;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.tests.performance.ServicesManager#getBusinessTransactionService()
     */
    @Override
    public BusinessTransactionService getTraceService() {
        return businessTransactionService;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.tests.performance.ServicesManager#clear()
     */
    @Override
    public void clear() throws Exception {
        analyticsService.clear(null);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.tests.performance.ServicesManager#close()
     */
    @Override
    public void close() throws Exception {
        client.close();
    }

}
