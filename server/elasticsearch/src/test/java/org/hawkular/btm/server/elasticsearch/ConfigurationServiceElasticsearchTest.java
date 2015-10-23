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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ConfigurationServiceElasticsearchTest {

    private ConfigurationServiceElasticsearch bts;

    @BeforeClass
    public static void initClass() {
        System.setProperty("hawkular-btm.data.dir", "target");
    }

    @Before
    public void beforeTest() {
        bts = new ConfigurationServiceElasticsearch();
        bts.init();
    }

    @After
    public void afterTest() {
        bts.clear(null);
        bts.close();
    }

    @Test
    public void testGetBusinessTransactions() {
        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            bts.updateBusinessTransaction(null, "btc1", btc1);
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
            bts.updateBusinessTransaction(null, "btc2", btc2);
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

        Map<String, BusinessTxnConfig> res1 = bts.getBusinessTransactions(null, 0);

        assertNotNull(res1);
        assertEquals(2, res1.size());

        Map<String, BusinessTxnConfig> res2 = bts.getBusinessTransactions(null, midtime);

        assertNotNull(res2);
        assertEquals(1, res2.size());
        assertTrue(res2.containsKey("btc2"));
    }

}
