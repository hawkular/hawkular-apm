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
package org.hawkular.apm.tests.dist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.Severity;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.hawkular.apm.config.service.rest.client.ConfigurationServiceRESTClient;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ConfigurationServiceITest extends AbstractITest {

    private static final String DESCRIPTION1 = "Description 1";
    private static final String DESCRIPTION2 = "Description 2";
    private static final String TXNCONFIG1 = "btxnconfig1";
    private static final String TXNCONFIG2 = "btxnconfig2";

    private static ConfigurationServiceRESTClient configService;

    @BeforeClass
    public static void initClass() {
        configService = new ConfigurationServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
    }

    @Before
    public void initTest() {
        configService.clear(null);
    }

    @Test
    public void testGetJvmCollectorConfiguration() {
        CollectorConfiguration cc = configService.getCollector(null, "jvm", null, null);

        assertNotNull(cc);

        assertNotEquals(0, cc.getInstrumentation().size());
    }

    @Test
    public void testGetDefaultCollectorConfiguration() {
        CollectorConfiguration cc = configService.getCollector(null, null, null, null);

        assertNotNull(cc);

        assertNotEquals(0, cc.getInstrumentation().size());
    }

    @Test
    public void testAddUpdateDeleteBusinessTxnConfig() {
        // Check config not already defined
        assertNull(configService.getTransaction(null, TXNCONFIG1));

        TransactionConfig btxnconfig1 = new TransactionConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        configService.setTransaction(null, TXNCONFIG1, btxnconfig1);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getTransaction(null, TXNCONFIG1) != null);

        // Check config was added
        TransactionConfig btxnconfig2 = configService.getTransaction(null, TXNCONFIG1);

        assertNotNull(btxnconfig2);
        assertEquals(DESCRIPTION1, btxnconfig2.getDescription());

        // Update description
        btxnconfig2.setDescription(DESCRIPTION2);

        configService.setTransaction(null, TXNCONFIG1, btxnconfig2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getTransaction(null, TXNCONFIG1) != null);

        // Check config was updated
        TransactionConfig btxnconfig3 = configService.getTransaction(null, TXNCONFIG1);

        assertNotNull(btxnconfig3);
        assertEquals(DESCRIPTION2, btxnconfig3.getDescription());

        // Remove the config
        configService.removeTransaction(null, TXNCONFIG1);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getTransaction(null, TXNCONFIG1) == null);

        TransactionConfig btxnconfig4 = configService.getTransaction(null, TXNCONFIG1);

        assertNull(btxnconfig4);
    }

    @Test
    public void testGetCollectorConfigurationWithBusinessTxnConfigs() {
        // Check config not already defined
        assertNull(configService.getTransaction(null, TXNCONFIG1));
        assertNull(configService.getTransaction(null, TXNCONFIG2));

        TransactionConfig btxnconfig1 = new TransactionConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        TransactionConfig btxnconfig2 = new TransactionConfig();
        btxnconfig2.setDescription(DESCRIPTION2);
        btxnconfig2.setFilter(new Filter());
        btxnconfig2.getFilter().getInclusions().add("myfilter");

        configService.setTransaction(null, TXNCONFIG1, btxnconfig1);
        configService.setTransaction(null, TXNCONFIG2, btxnconfig2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getCollector(null, null, null, null) != null);

        // Get the collector configuration
        CollectorConfiguration cc = configService.getCollector(null, null, null, null);

        assertNotNull(cc);
        assertNotNull(cc.getTransactions().get(TXNCONFIG1));
        assertNotNull(cc.getTransactions().get(TXNCONFIG2));

        // Remove the config
        configService.removeTransaction(null, TXNCONFIG1);
        configService.removeTransaction(null, TXNCONFIG2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(
                () -> configService.getTransaction(null, TXNCONFIG1) == null
                        && configService.getTransaction(null, TXNCONFIG2) == null);

        assertNull(configService.getTransaction(null, TXNCONFIG1));
        assertNull(configService.getTransaction(null, TXNCONFIG2));
    }

    @Test
    public void testGetBusinessTxnConfigurations() throws InterruptedException {
        // Check config not already defined
        assertNull(configService.getTransaction(null, TXNCONFIG1));
        assertNull(configService.getTransaction(null, TXNCONFIG2));

        TransactionConfig btxnconfig1 = new TransactionConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        TransactionConfig btxnconfig2 = new TransactionConfig();
        btxnconfig2.setDescription(DESCRIPTION2);
        btxnconfig2.setFilter(new Filter());
        btxnconfig2.getFilter().getInclusions().add("myfilter");

        long midtime = 0;

        configService.setTransaction(null, TXNCONFIG1, btxnconfig1);

        // these waits are part of the business logic and shouldn't be changed
        synchronized (this) {
            wait(1000);
        }

        midtime = System.currentTimeMillis();

        synchronized (this) {
            wait(1000);
        }

        configService.setTransaction(null, TXNCONFIG2, btxnconfig2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getTransactionSummaries(null) != null);

        // Get the btxn names
        List<TransactionSummary> btns = configService.getTransactionSummaries(null);

        assertNotNull(btns);
        assertEquals(2, btns.size());

        assertEquals(TXNCONFIG1, btns.get(0).getName());
        assertEquals(TXNCONFIG2, btns.get(1).getName());

        // Get all the btxn configs
        Map<String, TransactionConfig> btcs = configService.getTransactions(null, 0);

        assertNotNull(btcs);
        assertEquals(2, btcs.size());

        assertTrue(btcs.containsKey(TXNCONFIG1));
        assertTrue(btcs.containsKey(TXNCONFIG2));

        // Get the btxn configs updated after the specified time
        Map<String, TransactionConfig> btcs2 = configService.getTransactions(null, midtime);

        assertNotNull(btcs2);
        assertEquals(1, btcs2.size());

        assertTrue(btcs2.containsKey(TXNCONFIG2));

        // Remove the config
        configService.removeTransaction(null, TXNCONFIG1);
        configService.removeTransaction(null, TXNCONFIG2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(
                () -> configService.getTransaction(null, TXNCONFIG1) == null
                        && configService.getTransaction(null, TXNCONFIG2) == null);

        assertNull(configService.getTransaction(null, TXNCONFIG1));
        assertNull(configService.getTransaction(null, TXNCONFIG2));
    }

    @Test
    public void testSetBusinessTxnConfigurations() {
        // Check config not already defined
        assertNull(configService.getTransaction(null, TXNCONFIG1));
        assertNull(configService.getTransaction(null, TXNCONFIG2));

        TransactionConfig btxnconfig1 = new TransactionConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        TransactionConfig btxnconfig2 = new TransactionConfig();
        btxnconfig2.setDescription(DESCRIPTION2);
        btxnconfig2.setFilter(new Filter());
        btxnconfig2.getFilter().getInclusions().add("myfilter");

        Map<String,TransactionConfig> configs = new HashMap<String,TransactionConfig>();
        configs.put(TXNCONFIG1, btxnconfig1);
        configs.put(TXNCONFIG2, btxnconfig2);

        configService.setTransactions(null, configs);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getTransactionSummaries(null) != null);

        // Get the btxn names
        List<TransactionSummary> btns = configService.getTransactionSummaries(null);

        assertNotNull(btns);
        assertEquals(2, btns.size());

        assertEquals(TXNCONFIG1, btns.get(0).getName());
        assertEquals(TXNCONFIG2, btns.get(1).getName());

        // Get all the btxn configs
        Map<String, TransactionConfig> btcs = configService.getTransactions(null, 0);

        assertNotNull(btcs);
        assertEquals(2, btcs.size());

        assertTrue(btcs.containsKey(TXNCONFIG1));
        assertTrue(btcs.containsKey(TXNCONFIG2));

        // Remove the config
        configService.removeTransaction(null, TXNCONFIG1);
        configService.removeTransaction(null, TXNCONFIG2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(
                () -> configService.getTransaction(null, TXNCONFIG1) == null
                        && configService.getTransaction(null, TXNCONFIG2) == null);

        assertNull(configService.getTransaction(null, TXNCONFIG1));
        assertNull(configService.getTransaction(null, TXNCONFIG2));
    }

    @Test
    public void testValidateBusinessTxnConfiguration() {
        TransactionConfig btxnconfig1 = new TransactionConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        TransactionConfig btxnconfig2 = new TransactionConfig();
        btxnconfig2.setDescription(DESCRIPTION2);

        List<ConfigMessage> messages1 = configService.validateTransaction(btxnconfig1);
        assertNotNull(messages1);
        assertEquals(0, messages1.size());

        List<ConfigMessage> messages2 = configService.validateTransaction(btxnconfig2);
        assertNotNull(messages2);
        assertEquals(1, messages2.size());
        assertEquals(Severity.Error, messages2.get(0).getSeverity());
    }
}
