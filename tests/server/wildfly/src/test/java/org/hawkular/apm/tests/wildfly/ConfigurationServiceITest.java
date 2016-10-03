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
package org.hawkular.apm.tests.wildfly;

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
import org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.apm.api.model.config.btxn.ConfigMessage;
import org.hawkular.apm.api.model.config.btxn.Filter;
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
    private static final String BTXNCONFIG1 = "btxnconfig1";
    private static final String BTXNCONFIG2 = "btxnconfig2";

    private static ConfigurationServiceRESTClient configService;

    @BeforeClass
    public static void initClass() {
        configService = new ConfigurationServiceRESTClient();
        configService.setUsername(HAWKULAR_APM_USERNAME);
        configService.setPassword(HAWKULAR_APM_PASSWORD);
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
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));

        BusinessTxnConfig btxnconfig1 = new BusinessTxnConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        configService.setBusinessTransaction(null, BTXNCONFIG1, btxnconfig1);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getBusinessTransaction(null, BTXNCONFIG1) != null);

        // Check config was added
        BusinessTxnConfig btxnconfig2 = configService.getBusinessTransaction(null, BTXNCONFIG1);

        assertNotNull(btxnconfig2);
        assertEquals(DESCRIPTION1, btxnconfig2.getDescription());

        // Update description
        btxnconfig2.setDescription(DESCRIPTION2);

        configService.setBusinessTransaction(null, BTXNCONFIG1, btxnconfig2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getBusinessTransaction(null, BTXNCONFIG1) != null);

        // Check config was updated
        BusinessTxnConfig btxnconfig3 = configService.getBusinessTransaction(null, BTXNCONFIG1);

        assertNotNull(btxnconfig3);
        assertEquals(DESCRIPTION2, btxnconfig3.getDescription());

        // Remove the config
        configService.removeBusinessTransaction(null, BTXNCONFIG1);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getBusinessTransaction(null, BTXNCONFIG1) == null);

        BusinessTxnConfig btxnconfig4 = configService.getBusinessTransaction(null, BTXNCONFIG1);

        assertNull(btxnconfig4);
    }

    @Test
    public void testGetCollectorConfigurationWithBusinessTxnConfigs() {
        // Check config not already defined
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG2));

        BusinessTxnConfig btxnconfig1 = new BusinessTxnConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        BusinessTxnConfig btxnconfig2 = new BusinessTxnConfig();
        btxnconfig2.setDescription(DESCRIPTION2);
        btxnconfig2.setFilter(new Filter());
        btxnconfig2.getFilter().getInclusions().add("myfilter");

        configService.setBusinessTransaction(null, BTXNCONFIG1, btxnconfig1);
        configService.setBusinessTransaction(null, BTXNCONFIG2, btxnconfig2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getCollector(null, null, null, null) != null);

        // Get the collector configuration
        CollectorConfiguration cc = configService.getCollector(null, null, null, null);

        assertNotNull(cc);
        assertNotNull(cc.getBusinessTransactions().get(BTXNCONFIG1));
        assertNotNull(cc.getBusinessTransactions().get(BTXNCONFIG2));

        // Remove the config
        configService.removeBusinessTransaction(null, BTXNCONFIG1);
        configService.removeBusinessTransaction(null, BTXNCONFIG2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(
                () -> configService.getBusinessTransaction(null, BTXNCONFIG1) == null
                        && configService.getBusinessTransaction(null, BTXNCONFIG2) == null);

        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG2));
    }

    @Test
    public void testGetBusinessTxnConfigurations() throws InterruptedException {
        // Check config not already defined
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG2));

        BusinessTxnConfig btxnconfig1 = new BusinessTxnConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        BusinessTxnConfig btxnconfig2 = new BusinessTxnConfig();
        btxnconfig2.setDescription(DESCRIPTION2);
        btxnconfig2.setFilter(new Filter());
        btxnconfig2.getFilter().getInclusions().add("myfilter");

        long midtime = 0;

        configService.setBusinessTransaction(null, BTXNCONFIG1, btxnconfig1);

        // these waits are part of the business logic and shouldn't be changed
        synchronized (this) {
            wait(1000);
        }

        midtime = System.currentTimeMillis();

        synchronized (this) {
            wait(1000);
        }

        configService.setBusinessTransaction(null, BTXNCONFIG2, btxnconfig2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getBusinessTransactionSummaries(null) != null);

        // Get the btxn names
        List<BusinessTxnSummary> btns = configService.getBusinessTransactionSummaries(null);

        assertNotNull(btns);
        assertEquals(2, btns.size());

        assertEquals(BTXNCONFIG1, btns.get(0).getName());
        assertEquals(BTXNCONFIG2, btns.get(1).getName());

        // Get all the btxn configs
        Map<String, BusinessTxnConfig> btcs = configService.getBusinessTransactions(null, 0);

        assertNotNull(btcs);
        assertEquals(2, btcs.size());

        assertTrue(btcs.containsKey(BTXNCONFIG1));
        assertTrue(btcs.containsKey(BTXNCONFIG2));

        // Get the btxn configs updated after the specified time
        Map<String, BusinessTxnConfig> btcs2 = configService.getBusinessTransactions(null, midtime);

        assertNotNull(btcs2);
        assertEquals(1, btcs2.size());

        assertTrue(btcs2.containsKey(BTXNCONFIG2));

        // Remove the config
        configService.removeBusinessTransaction(null, BTXNCONFIG1);
        configService.removeBusinessTransaction(null, BTXNCONFIG2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(
                () -> configService.getBusinessTransaction(null, BTXNCONFIG1) == null
                        && configService.getBusinessTransaction(null, BTXNCONFIG2) == null);

        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG2));
    }

    @Test
    public void testSetBusinessTxnConfigurations() {
        // Check config not already defined
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG2));

        BusinessTxnConfig btxnconfig1 = new BusinessTxnConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        BusinessTxnConfig btxnconfig2 = new BusinessTxnConfig();
        btxnconfig2.setDescription(DESCRIPTION2);
        btxnconfig2.setFilter(new Filter());
        btxnconfig2.getFilter().getInclusions().add("myfilter");

        Map<String,BusinessTxnConfig> configs = new HashMap<String,BusinessTxnConfig>();
        configs.put(BTXNCONFIG1, btxnconfig1);
        configs.put(BTXNCONFIG2, btxnconfig2);

        configService.setBusinessTransactions(null, configs);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(() -> configService.getBusinessTransactionSummaries(null) != null);

        // Get the btxn names
        List<BusinessTxnSummary> btns = configService.getBusinessTransactionSummaries(null);

        assertNotNull(btns);
        assertEquals(2, btns.size());

        assertEquals(BTXNCONFIG1, btns.get(0).getName());
        assertEquals(BTXNCONFIG2, btns.get(1).getName());

        // Get all the btxn configs
        Map<String, BusinessTxnConfig> btcs = configService.getBusinessTransactions(null, 0);

        assertNotNull(btcs);
        assertEquals(2, btcs.size());

        assertTrue(btcs.containsKey(BTXNCONFIG1));
        assertTrue(btcs.containsKey(BTXNCONFIG2));

        // Remove the config
        configService.removeBusinessTransaction(null, BTXNCONFIG1);
        configService.removeBusinessTransaction(null, BTXNCONFIG2);

        // Need to make sure change applied, for cases where non-transactional
        // config repo (e.g. elasticsearch) is used.
        Wait.until(
                () -> configService.getBusinessTransaction(null, BTXNCONFIG1) == null
                        && configService.getBusinessTransaction(null, BTXNCONFIG2) == null);

        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG1));
        assertNull(configService.getBusinessTransaction(null, BTXNCONFIG2));
    }

    @Test
    public void testValidateBusinessTxnConfiguration() {
        BusinessTxnConfig btxnconfig1 = new BusinessTxnConfig();
        btxnconfig1.setDescription(DESCRIPTION1);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        BusinessTxnConfig btxnconfig2 = new BusinessTxnConfig();
        btxnconfig2.setDescription(DESCRIPTION2);

        List<ConfigMessage> messages1 = configService.validateBusinessTransaction(btxnconfig1);
        assertNotNull(messages1);
        assertEquals(0, messages1.size());

        List<ConfigMessage> messages2 = configService.validateBusinessTransaction(btxnconfig2);
        assertNotNull(messages2);
        assertEquals(1, messages2.size());
        assertEquals(Severity.Error, messages2.get(0).getSeverity());
    }
}
