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
package org.hawkular.btm.server.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BusinessTransactionServiceElasticsearchTest {

    private BusinessTransactionServiceElasticsearch bts;

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
        bts = new BusinessTransactionServiceElasticsearch();
        bts.setElasticsearchClient(client);
    }

    @After
    public void afterTest() {
        bts.clear(null);
        client.close();
    }

    @Test
    public void testQueryBTxnName() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setName("btxn1");
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setName("btxn2");
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        BusinessTransaction btxn3 = new BusinessTransaction();
        btxn3.setId("id3");
        btxn3.setStartTime(3000);
        btxns.add(btxn3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);
        criteria.setBusinessTransaction("btxn1");

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
        assertEquals("btxn1", result1.get(0).getName());
    }

    @Test
    public void testQueryNoBTxnName() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setName("btxn1");
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setName("btxn2");
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        BusinessTransaction btxn3 = new BusinessTransaction();
        btxn3.setId("id3");
        btxn3.setStartTime(3000);
        btxns.add(btxn3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);
        criteria.setBusinessTransaction("");

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id3", result1.get(0).getId());
        assertNull(result1.get(0).getName());
    }

    @Test
    public void testQuerySinglePropertyAndValueIncluded() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setStartTime(1000);
        btxn1.getProperties().put("prop1", "value1");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setStartTime(2000);
        btxn2.getProperties().put("prop2", "value2");
        btxns.add(btxn2);

        BusinessTransaction btxn3 = new BusinessTransaction();
        btxn3.setId("id3");
        btxn3.setStartTime(3000);
        btxn3.getProperties().put("prop1", "value3");
        btxns.add(btxn3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", false);

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
    }

    @Test
    public void testQuerySinglePropertyAndValueExcluded() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setStartTime(1000);
        btxn1.getProperties().put("prop1", "value1");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setStartTime(2000);
        btxn2.getProperties().put("prop2", "value2");
        btxns.add(btxn2);

        BusinessTransaction btxn3 = new BusinessTransaction();
        btxn3.setId("id3");
        btxn3.setStartTime(3000);
        btxn3.getProperties().put("prop1", "value3");
        btxns.add(btxn3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", true);

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertTrue((result1.get(0).getId().equals("id2") && result1.get(1).getId().equals("id3"))
                || (result1.get(0).getId().equals("id3") && result1.get(1).getId().equals("id2")));
    }

    @Test
    public void testQuerySinglePropertyAndMultiValueIncluded() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setStartTime(1000);
        btxn1.getProperties().put("prop1", "value1");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setStartTime(2000);
        btxn2.getProperties().put("prop2", "value2");
        btxns.add(btxn2);

        BusinessTransaction btxn3 = new BusinessTransaction();
        btxn3.setId("id3");
        btxn3.setStartTime(3000);
        btxn3.getProperties().put("prop3", "value3");
        btxns.add(btxn3);

        BusinessTransaction btxn4 = new BusinessTransaction();
        btxn4.setId("id4");
        btxn4.setStartTime(4000);
        btxn4.getProperties().put("prop1", "value1");
        btxn4.getProperties().put("prop3", "value3");
        btxns.add(btxn4);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", false);
        criteria.addProperty("prop3", "value3", false);

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id4", result1.get(0).getId());
    }

    @Test
    public void testQuerySinglePropertyAndMultiValueExcluded() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setStartTime(1000);
        btxn1.getProperties().put("prop1", "value1");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setStartTime(2000);
        btxn2.getProperties().put("prop2", "value2");
        btxns.add(btxn2);

        BusinessTransaction btxn3 = new BusinessTransaction();
        btxn3.setId("id3");
        btxn3.setStartTime(3000);
        btxn3.getProperties().put("prop1", "value3");
        btxns.add(btxn3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", true);
        criteria.addProperty("prop1", "value3", true);

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id2", result1.get(0).getId());
    }
}
