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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ConfigurationServiceElasticsearchTest {

    private ConfigurationServiceElasticsearch cfgs;

    private ElasticsearchClient client;

    @BeforeClass
    public static void initClass() {
        System.setProperty("hawkular-btm.data.dir", "target");
    }

    @Before
    public void beforeTest() {
        client = new ElasticsearchClient();
        try {
            client.init();
        } catch (Exception e) {
            fail("Failed to initialise Elasticsearch client: "+e);
        }
        cfgs = new ConfigurationServiceElasticsearch();
        cfgs.setElasticsearchClient(client);
    }

    @After
    public void afterTest() {
        cfgs.clear(null);
        client.close();
    }

    @Test
    public void testGetBusinessTransactions() {
        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            cfgs.updateBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        long midtime = System.currentTimeMillis();

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        BusinessTxnConfig btc2 = new BusinessTxnConfig();
        btc2.setDescription("btc2");

        try {
            cfgs.updateBusinessTransaction(null, "btc2", btc2);
        } catch (Exception e) {
            fail("Failed to update btc2: " + e);
        }

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        Map<String, BusinessTxnConfig> res1 = cfgs.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(2, res1.size());

        Map<String, BusinessTxnConfig> res2 = cfgs.getBusinessTransactions(null, midtime);

        assertNotNull(res2);
        assertEquals(1, res2.size());
        assertTrue(res2.containsKey("btc2"));

        // Check summaries
        List<BusinessTxnSummary> summaries = cfgs.getBusinessTransactionSummaries(null);
        assertNotNull(summaries);
        assertEquals(2, summaries.size());
    }


    @Test
    public void testGetBusinessTransactionsAfterRemove() {
        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            cfgs.updateBusinessTransaction(null, "btc1", btc1);
        } catch (Exception e) {
            fail("Failed to update btc1: " + e);
        }

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        long midtime = System.currentTimeMillis();

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        try {
            cfgs.removeBusinessTransaction(null, "btc2");
        } catch (Exception e) {
            fail("Failed to remove btc2: " + e);
        }

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        Map<String, BusinessTxnConfig> res1 = cfgs.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(1, res1.size());
        assertTrue(res1.containsKey("btc1"));

        Map<String, BusinessTxnConfig> res2 = cfgs.getBusinessTransactions(null, midtime);

        assertNotNull(res2);
        assertEquals(1, res2.size());
        assertTrue(res2.containsKey("btc2"));

        BusinessTxnConfig btc2 = cfgs.getBusinessTransaction(null, "btc2");
        assertNull(btc2);
    }

}
