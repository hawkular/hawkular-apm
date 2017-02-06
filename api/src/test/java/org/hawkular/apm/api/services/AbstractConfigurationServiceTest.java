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
package org.hawkular.apm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.SetPropertyAction;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AbstractConfigurationServiceTest {

    @Test
    public void testValidateNoFilters() {
        TestConfigurationService cs = new TestConfigurationService();

        TransactionConfig config = new TransactionConfig();

        List<ConfigMessage> messages = cs.validateTransaction(config);

        assertNotNull(messages);
        assertFalse(messages.isEmpty());
    }

    @Test
    public void testValidateValid() {
        TestConfigurationService cs = new TestConfigurationService();

        TransactionConfig config = new TransactionConfig();
        config.setFilter(new Filter());
        config.getFilter().getInclusions().add("myfilter");

        List<ConfigMessage> messages = cs.validateTransaction(config);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testValidateSetPropertyMissingNameAndExpression() {
        TestConfigurationService cs = new TestConfigurationService();

        TransactionConfig config = new TransactionConfig();
        config.setFilter(new Filter());
        config.getFilter().getInclusions().add("myfilter");

        Processor processor = new Processor();
        processor.setDescription("processor1");
        config.getProcessors().add(processor);

        SetPropertyAction action = new SetPropertyAction();
        action.setDescription("action1");
        processor.getActions().add(action);

        List<ConfigMessage> messages = cs.validateTransaction(config);
        assertEquals(2, messages.size());
        assertEquals(processor.getDescription(), messages.get(0).getProcessor());
        assertEquals(processor.getDescription(), messages.get(1).getProcessor());
        assertEquals(action.getDescription(), messages.get(0).getAction());
        assertEquals(action.getDescription(), messages.get(1).getAction());
    }

    public class TestConfigurationService extends AbstractConfigurationService {

        @Override
        public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<ConfigMessage> setTransaction(String tenantId, String name, TransactionConfig config)
                throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs)
                throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public TransactionConfig getTransaction(String tenantId, String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, TransactionConfig> getTransactions(String tenantId, long updated) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<TransactionSummary> getTransactionSummaries(String tenantId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void removeTransaction(String tenantId, String name) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void clear(String tenantId) {
            // TODO Auto-generated method stub

        }

    }
}
