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
package org.hawkular.apm.server.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.Severity;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.apm.api.model.config.btxn.ConfigMessage;
import org.hawkular.apm.api.model.config.btxn.Filter;
import org.hawkular.apm.tests.common.Wait;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author gbrown
 */
public class ConfigurationServiceElasticsearchTest {

    /**  */
    private static final String VALID_DESCRIPTION = "Valid description";

    /**  */
    private static final String INVALID_DESCRIPTION = "Invalid description";
    private ConfigurationServiceElasticsearch cfgs;
    private Clock clock = Mockito.mock(Clock.class);

    @BeforeClass
    public static void initClass() {
        System.setProperty("HAWKULAR_APM_CONFIG_DIR", "target");
    }

    @Before
    public void beforeTest() {
        cfgs = new ConfigurationServiceElasticsearch(clock);
    }

    @After
    public void afterTest() {
        cfgs.clear(null);
    }

    @Test
    public void testGetBusinessTransactionsUpdated() {
        long initialTime = clock.millis();
        long midTime = initialTime + 1000;
        when(clock.millis()).thenReturn(initialTime, midTime+1000, midTime+1000);

        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");
        btc1.setFilter(new Filter());
        btc1.getFilter().getInclusions().add("myfilter");

        try {
            cfgs.setBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        BusinessTxnConfig btc2 = new BusinessTxnConfig();
        btc2.setDescription("btc2");
        btc2.setFilter(new Filter());
        btc2.getFilter().getInclusions().add("myfilter");

        try {
            cfgs.setBusinessTransaction(null, "btc2", btc2);
        } catch (Exception e) {
            fail("Failed to update btc2: " + e);
        }

        Wait.until(() -> cfgs.getBusinessTransactions(null, 0).size() == 2);
        Map<String, BusinessTxnConfig> res1 = cfgs.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(2, res1.size());

        Wait.until(() -> cfgs.getBusinessTransactions(null, midTime).size() == 1);
        Map<String, BusinessTxnConfig> res2 = cfgs.getBusinessTransactions(null, midTime);

        assertNotNull(res2);
        assertEquals(1, res2.size());
        assertTrue(res2.containsKey("btc2"));

        // Check summaries
        List<BusinessTxnSummary> summaries = cfgs.getBusinessTransactionSummaries(null);
        assertNotNull(summaries);
        assertEquals(2, summaries.size());
    }

    @Test
    public void testGetBusinessTransactionsInvalid() {
        long initialTime = clock.millis();
        when(clock.millis()).thenReturn(initialTime, initialTime+1000);

        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            // Updating invalid config
            cfgs.setBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        // Check invalid config can still be retrieved
        BusinessTxnConfig config = cfgs.getBusinessTransaction(null, "btc1");

        assertNotNull(config);

        // Make sure not returned in list of updated configs
        Map<String, BusinessTxnConfig> res1 = cfgs.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(0, res1.size());

        // Check summaries - should include invalid config entries
        List<BusinessTxnSummary> summaries = cfgs.getBusinessTransactionSummaries(null);
        assertNotNull(summaries);
        assertEquals(1, summaries.size());
    }

    @Test
    public void testGetBusinessTransactionsValidThenInvalid() {
        long initialTime = clock.millis();
        when(clock.millis()).thenReturn(initialTime, initialTime+1000, initialTime+2000, initialTime+3000);

        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription(VALID_DESCRIPTION);
        btc1.setFilter(new Filter());
        btc1.getFilter().getInclusions().add("myfilter");

        try {
            // Updating valid config
            List<ConfigMessage> messages=cfgs.setBusinessTransaction(null, "btc1", btc1);
            assertNotNull(messages);
            assertEquals(1, messages.size());
            assertEquals(Severity.Info, messages.get(0).getSeverity());
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        btc1.setDescription(INVALID_DESCRIPTION);
        btc1.setFilter(null);

        try {
            // Updating with invalid config
            cfgs.setBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        // Check invalid config can still be retrieved
        BusinessTxnConfig invalid = cfgs.getBusinessTransaction(null, "btc1");
        assertNotNull(invalid);

        assertEquals(INVALID_DESCRIPTION, invalid.getDescription());

        // Get valid business txns
        Map<String, BusinessTxnConfig> res1 = cfgs.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(1, res1.size());

        BusinessTxnConfig valid = res1.get("btc1");
        assertNotNull(valid);

        assertEquals(VALID_DESCRIPTION, valid.getDescription());

        // Check that once description is fixed, the valid one is returned
        btc1.setDescription(VALID_DESCRIPTION);
        btc1.setFilter(new Filter());
        btc1.getFilter().getInclusions().add("myfilter");

        try {
            // Updating with valid config
            List<ConfigMessage> messages=cfgs.setBusinessTransaction(null, "btc1", btc1);
            assertNotNull(messages);
            assertEquals(1, messages.size());
            assertEquals(Severity.Info, messages.get(0).getSeverity());
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        BusinessTxnConfig valid2 = cfgs.getBusinessTransaction(null, "btc1");
        assertNotNull(valid2);

        assertEquals(VALID_DESCRIPTION, valid2.getDescription());
    }

    @Test
    public void testGetBusinessTransactionsAfterRemove() {
        long initialTime = clock.millis();
        long midTime = initialTime + 1000;
        when(clock.millis()).thenReturn(initialTime, midTime+1000, midTime+2000);

        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");
        btc1.setFilter(new Filter());
        btc1.getFilter().getInclusions().add("myfilter");

        try {
            cfgs.setBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        try {
            cfgs.removeBusinessTransaction(null, "btc2");
        } catch (Exception e) {
            fail("Failed to remove btc2: " + e);
        }

        Map<String, BusinessTxnConfig> res1 = cfgs.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(1, res1.size());
        assertTrue(res1.containsKey("btc1"));

        Map<String, BusinessTxnConfig> res2 = cfgs.getBusinessTransactions(null, midTime);

        assertNotNull(res2);
        assertEquals(1, res2.size());
        assertTrue(res2.containsKey("btc2"));

        BusinessTxnConfig btc2 = cfgs.getBusinessTransaction(null, "btc2");
        assertNull(btc2);
    }

    @Test
    public void testGetBusinessTransactionsAfterRemoveInvalid() {
        long initialTime = clock.millis();
        when(clock.millis()).thenReturn(initialTime, initialTime+1000, initialTime+2000);

        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            cfgs.setBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        try {
            cfgs.removeBusinessTransaction(null, "btc1");
        } catch (Exception e) {
            fail("Failed to remove btc1: " + e);
        }

        BusinessTxnConfig btc1again = cfgs.getBusinessTransaction(null, "btc1");
        assertNull(btc1again);
    }

}
