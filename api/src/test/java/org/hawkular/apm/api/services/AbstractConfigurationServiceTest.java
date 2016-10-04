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
package org.hawkular.apm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.internal.actions.ExpressionBasedActionHandler;
import org.hawkular.apm.api.internal.actions.SetPropertyActionHandler;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.apm.api.model.config.btxn.ConfigMessage;
import org.hawkular.apm.api.model.config.btxn.Filter;
import org.hawkular.apm.api.model.config.btxn.Processor;
import org.hawkular.apm.api.model.config.btxn.SetPropertyAction;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AbstractConfigurationServiceTest {

    @Test
    public void testValidateNoFilters() {
        TestConfigurationService cs = new TestConfigurationService();

        BusinessTxnConfig config = new BusinessTxnConfig();

        List<ConfigMessage> messages = cs.validateBusinessTransaction(config);

        assertNotNull(messages);
        assertFalse(messages.isEmpty());
    }

    @Test
    public void testValidateValid() {
        TestConfigurationService cs = new TestConfigurationService();

        BusinessTxnConfig config = new BusinessTxnConfig();
        config.setFilter(new Filter());
        config.getFilter().getInclusions().add("myfilter");

        List<ConfigMessage> messages = cs.validateBusinessTransaction(config);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testValidateSetPropertyMissingNameAndExpression() {
        TestConfigurationService cs = new TestConfigurationService();

        BusinessTxnConfig config = new BusinessTxnConfig();
        config.setFilter(new Filter());
        config.getFilter().getInclusions().add("myfilter");

        Processor processor = new Processor();
        processor.setDescription("processor1");
        config.getProcessors().add(processor);

        SetPropertyAction action = new SetPropertyAction();
        action.setDescription("action1");
        processor.getActions().add(action);

        List<ConfigMessage> messages = cs.validateBusinessTransaction(config);

        assertNotNull(messages);
        assertEquals(2, messages.size());

        assertEquals(processor.getDescription(), messages.get(0).getProcessor());
        assertEquals(processor.getDescription(), messages.get(1).getProcessor());
        assertEquals(action.getDescription(), messages.get(0).getAction());
        assertEquals(action.getDescription(), messages.get(1).getAction());
        assertEquals(SetPropertyActionHandler.NAME_MUST_BE_SPECIFIED, messages.get(0).getMessage());
        assertEquals(ExpressionBasedActionHandler.EXPRESSION_HAS_NOT_BEEN_DEFINED, messages.get(1).getMessage());
    }

    public class TestConfigurationService extends AbstractConfigurationService {

        @Override
        public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<ConfigMessage> setBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
                throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<ConfigMessage> setBusinessTransactions(String tenantId, Map<String, BusinessTxnConfig> configs)
                throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void removeBusinessTransaction(String tenantId, String name) throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
        public void clear(String tenantId) {
            // TODO Auto-generated method stub

        }

    }
}
