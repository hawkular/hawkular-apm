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
package org.hawkular.apm.performance.server;

import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.apm.api.model.config.btxn.ConfigMessage;
import org.hawkular.apm.api.services.ConfigurationService;

/**
 * @author gbrown
 */
public class TestConfigurationService implements ConfigurationService {

    private CollectorConfiguration collectorConfiguration = new CollectorConfiguration();

    public void setCollectorConfiguration(CollectorConfiguration config) {
        collectorConfiguration = config;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getCollector(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
        return collectorConfiguration;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#setBusinessTransaction(java.lang.String, java.lang.String, org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public List<ConfigMessage> setBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
            throws Exception {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#setBusinessTransactions(java.lang.String, java.util.Map)
     */
    @Override
    public List<ConfigMessage> setBusinessTransactions(String tenantId, Map<String, BusinessTxnConfig> configs)
            throws Exception {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#validateBusinessTransaction(org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public List<ConfigMessage> validateBusinessTransaction(BusinessTxnConfig config) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getBusinessTransaction(java.lang.String, java.lang.String)
     */
    @Override
    public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getBusinessTransactions(java.lang.String, long)
     */
    @Override
    public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getBusinessTransactionSummaries(java.lang.String)
     */
    @Override
    public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#removeBusinessTransaction(java.lang.String, java.lang.String)
     */
    @Override
    public void removeBusinessTransaction(String tenantId, String name) throws Exception {
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
    }

}
