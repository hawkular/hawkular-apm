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
package org.hawkular.apm.performance.server;

import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.hawkular.apm.api.services.ConfigurationService;

/**
 * @author gbrown
 */
public class TestConfigurationService implements ConfigurationService {

    private CollectorConfiguration collectorConfiguration = new CollectorConfiguration();

    public void setCollectorConfiguration(CollectorConfiguration config) {
        collectorConfiguration = config;
    }

    @Override
    public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
        return collectorConfiguration;
    }

    @Override
    public List<ConfigMessage> setTransaction(String tenantId, String name, TransactionConfig config)
            throws Exception {
        return null;
    }

    @Override
    public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs)
            throws Exception {
        return null;
    }

    @Override
    public List<ConfigMessage> validateTransaction(TransactionConfig config) {
        return null;
    }

    @Override
    public TransactionConfig getTransaction(String tenantId, String name) {
        return null;
    }

    @Override
    public Map<String, TransactionConfig> getTransactions(String tenantId, long updated) {
        return null;
    }

    @Override
    public List<TransactionSummary> getTransactionSummaries(String tenantId) {
        return null;
    }

    @Override
    public void removeTransaction(String tenantId, String name) throws Exception {
    }

    @Override
    public void clear(String tenantId) {
    }

}
