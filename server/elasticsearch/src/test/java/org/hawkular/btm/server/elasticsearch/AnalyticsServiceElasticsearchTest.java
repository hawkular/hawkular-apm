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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.analytics.CompletionTime;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.Filter;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AnalyticsServiceElasticsearchTest {

    private BusinessTransactionServiceElasticsearch bts;

    private AnalyticsServiceElasticsearch analytics;

    @BeforeClass
    public static void initClass() {
        System.setProperty("hawkular-btm.data.dir", "target");
    }

    @Before
    public void beforeTest() {
        analytics = new AnalyticsServiceElasticsearch();
        bts = new BusinessTransactionServiceElasticsearch();
        bts.init();
        analytics.setElasticsearchClient(bts.getElasticsearchClient());
    }

    @After
    public void afterTest() {
        bts.clear(null);
        bts.close();
    }

    @Test
    public void testAllDistinctUnboundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri4");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri5");

        btxn2.getNodes().add(c2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        java.util.Map<String, java.util.List<String>> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(2, uris.size());

        assertTrue(uris.containsKey("uri1"));
        assertTrue(uris.containsKey("uri5"));

        assertEquals(4, uris.get("uri1").size());
        assertEquals(1, uris.get("uri5").size());
    }

    @Test
    public void testAllDuplicationUnboundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri3");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri2");

        btxn2.getNodes().add(c2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        java.util.Map<String, java.util.List<String>> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(2, uris.size());

        assertTrue(uris.containsKey("uri1"));
        assertTrue(uris.containsKey("uri2"));

        assertEquals(3, uris.get("uri1").size());
        assertEquals(1, uris.get("uri2").size());
    }

    @Test
    public void testUnboundThenBoundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri3");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setStartTime(2000);
        btxn2.setName("A Name");
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri1");
        btxn2.getNodes().add(c2);

        Component t3 = new Component();
        t3.setUri("uri4");
        c2.getNodes().add(t3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        java.util.Map<String, java.util.List<String>> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(0, uris.size());
    }

    @Test
    public void testBoundThenUnboundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxn1.setName("A Name");
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri3");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri1");
        btxn2.getNodes().add(c2);

        Component t3 = new Component();
        t3.setUri("uri4");
        c2.getNodes().add(t3);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        java.util.Map<String, java.util.List<String>> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(1, uris.size());

        assertTrue(uris.containsKey("uri1"));

        assertEquals(2, uris.get("uri1").size());
    }

    @Test
    public void testUnboundURIsExcludeBTxnConfig() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        analytics.setConfigurationService(new ConfigurationService() {
            @Override
            public CollectorConfiguration getCollector(String tenantId, String host, String server) {
                return null;
            }

            @Override
            public void updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
                    throws Exception {
            }

            @Override
            public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
                return null;
            }

            @Override
            public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
                Map<String, BusinessTxnConfig> ret = new HashMap<String, BusinessTxnConfig>();
                BusinessTxnConfig btc = new BusinessTxnConfig();
                btc.setFilter(new Filter());
                btc.getFilter().getInclusions().add("uri1");
                ret.put("btc1", btc);
                return ret;
            }

            @Override
            public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
                return null;
            }

            @Override
            public void removeBusinessTransaction(String tenantId, String name) throws Exception {
            }
        });

        java.util.Map<String, java.util.List<String>> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(0, uris.size());
    }

    @Test
    public void testUnboundURIsExcludeBTxnConfigRegex() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("{myns}ns");
        btxn1.getNodes().add(c1);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        analytics.setConfigurationService(new ConfigurationService() {
            @Override
            public CollectorConfiguration getCollector(String tenantId, String host, String server) {
                return null;
            }

            @Override
            public void updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
                    throws Exception {
            }

            @Override
            public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
                return null;
            }

            @Override
            public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
                Map<String, BusinessTxnConfig> ret = new HashMap<String, BusinessTxnConfig>();
                BusinessTxnConfig btc = new BusinessTxnConfig();
                btc.setFilter(new Filter());
                btc.getFilter().getInclusions().add("^\\{myns\\}ns$");
                ret.put("btc1", btc);
                return ret;
            }

            @Override
            public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
                return null;
            }

            @Override
            public void removeBusinessTransaction(String tenantId, String name) throws Exception {
            }
        });

        java.util.Map<String, java.util.List<String>> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(0, uris.size());
    }

    @Test
    public void testGetCompletionCount() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1 = new CompletionTime();
        ct1.setBusinessTransaction("testapp");
        ct1.setTimestamp(1000);
        cts.add(ct1);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2000);
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        assertEquals(2, analytics.getCompletionCount(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(100).setEndTime(0)));
    }

    @Test
    public void testGetCompletionFaultCount() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1 = new CompletionTime();
        ct1.setBusinessTransaction("testapp");
        ct1.setTimestamp(1000);
        ct1.setFault("Failed");
        cts.add(ct1);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2000);
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        assertEquals(1, analytics.getCompletionFaultCount(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(100).setEndTime(0)));
    }

}
