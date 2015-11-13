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

import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CompletionTime;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.analytics.Statistics;
import org.hawkular.btm.api.model.analytics.URIInfo;
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
        analytics = new AnalyticsServiceElasticsearch();
        bts = new BusinessTransactionServiceElasticsearch();
        analytics.setElasticsearchClient(client);
        bts.setElasticsearchClient(client);
    }

    @After
    public void afterTest() {
        bts.clear(null);
        client.close();
    }

    @Test
    public void testAllDistinctUnboundURIsConsumer() {
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

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(2, uris.size());

        assertEquals("uri1", uris.get(0).getUri());
        assertEquals("uri5", uris.get(1).getUri());
    }

    @Test
    public void testAllDistinctUnboundURIsProducer() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Component c1 = new Component();
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
        t1.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("uri5");
        t2.getNodes().add(p2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(2, uris.size());

        assertEquals("uri4", uris.get(0).getUri());
        assertEquals("uri5", uris.get(1).getUri());
    }

    @Test
    public void testAllDuplicationUnboundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Component c1 = new Component();
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
        c2.setUri("uri3");

        btxn2.getNodes().add(c2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(1, uris.size());

        assertEquals("uri3", uris.get(0).getUri());
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

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0);

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

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0);

        assertNotNull(uris);
        assertEquals(0, uris.size());
    }

    @Test
    public void testBoundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setName("btxn1");
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
        p1.setUri("uri2");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setName("btxn2");
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri4");

        btxn2.getNodes().add(c2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        java.util.List<String> uris1 = analytics.getBoundURIs(null, "btxn1", 100, 0);

        assertNotNull(uris1);
        assertEquals(3, uris1.size());
        assertTrue(uris1.contains("uri1"));
        assertTrue(uris1.contains("uri2"));
        assertTrue(uris1.contains("uri3"));

        java.util.List<String> uris2 = analytics.getBoundURIs(null, "btxn2", 100, 0);

        assertNotNull(uris2);
        assertEquals(1, uris2.size());
        assertTrue(uris2.contains("uri4"));
    }

    @Test
    public void testPropertyInfo() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setName("btxn1");
        btxn1.setStartTime(1000);
        btxn1.getProperties().put("prop1", "value1");
        btxn1.getProperties().put("prop2", "value2");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setName("btxn1");
        btxn2.setStartTime(2000);
        btxn2.getProperties().put("prop3", "value3");
        btxn2.getProperties().put("prop2", "value2");
        btxns.add(btxn2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        java.util.List<PropertyInfo> pis = analytics.getPropertyInfo(null, "btxn1", 100, 0);

        assertNotNull(pis);
        assertEquals(3, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
        assertTrue(pis.get(1).getName().equals("prop2"));
        assertTrue(pis.get(2).getName().equals("prop3"));
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

    @Test
    public void testGetCompletionStatistics() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        cts.add(ct1_2);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        List<Statistics> stats = analytics.getCompletionStatistics(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(1000).setEndTime(10000),
                1000);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(2000, stats.get(1).getTimestamp());

        assertEquals(2, stats.get(0).getCount());
        assertEquals(1, stats.get(1).getCount());

        assertTrue(stats.get(0).getMin() == 100);
        assertTrue(stats.get(0).getAverage() == 200);
        assertTrue(stats.get(0).getMax() == 300);

        assertTrue(stats.get(1).getMin() == 500);
        assertTrue(stats.get(1).getAverage() == 500);
        assertTrue(stats.get(1).getMax() == 500);

        assertEquals(0, stats.get(0).getFaultCount());
        assertEquals(0, stats.get(1).getFaultCount());
    }

    @Test
    public void testGetCompletionStatisticsWithFaults() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.setFault("fault1");
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        cts.add(ct1_2);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        ct2.setFault("fault2");
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        List<Statistics> stats = analytics.getCompletionStatistics(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(1000).setEndTime(10000),
                1000);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(2000, stats.get(1).getTimestamp());

        assertEquals(2, stats.get(0).getCount());
        assertEquals(1, stats.get(1).getCount());

        assertTrue(stats.get(0).getMin() == 100);
        assertTrue(stats.get(0).getAverage() == 200);
        assertTrue(stats.get(0).getMax() == 300);

        assertTrue(stats.get(1).getMin() == 500);
        assertTrue(stats.get(1).getAverage() == 500);
        assertTrue(stats.get(1).getMax() == 500);

        assertEquals(1, stats.get(0).getFaultCount());
        assertEquals(1, stats.get(1).getFaultCount());
    }

    @Test
    public void testGetCompletionPropertyDetails() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().put("prop1", "value1");
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().put("prop1", "value2");
        ct1_2.getProperties().put("prop2", "value3");
        cts.add(ct1_2);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        ct2.getProperties().put("prop1", "value2");
        ct2.getProperties().put("prop2", "value4");
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        List<Cardinality> cards1 = analytics.getCompletionPropertyDetails(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(1000).setEndTime(10000),
                "prop1");

        assertNotNull(cards1);
        assertEquals(2, cards1.size());

        assertEquals("value1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
        assertEquals("value2", cards1.get(1).getValue());
        assertEquals(2, cards1.get(1).getCount());

        List<Cardinality> cards2 = analytics.getCompletionPropertyDetails(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(1000).setEndTime(10000),
                "prop2");

        assertNotNull(cards2);
        assertEquals(2, cards2.size());

        assertEquals("value3", cards2.get(0).getValue());
        assertEquals(1, cards2.get(0).getCount());
        assertEquals("value4", cards2.get(1).getValue());
        assertEquals(1, cards2.get(1).getCount());
    }

    @Test
    public void testGetCompletionFaultDetails() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.setFault("fault1");
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.setFault("fault2");
        cts.add(ct1_2);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        ct2.setFault("fault2");
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        List<Cardinality> cards1 = analytics.getCompletionFaultDetails(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(1000).setEndTime(10000));

        assertNotNull(cards1);
        assertEquals(2, cards1.size());

        assertEquals("fault2", cards1.get(0).getValue());
        assertEquals(2, cards1.get(0).getCount());
        assertEquals("fault1", cards1.get(1).getValue());
        assertEquals(1, cards1.get(1).getCount());
    }


    @Test
    public void testGetCompletionFaultDetailsNotAllFaults() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.setFault("fault1");
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        cts.add(ct1_2);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        cts.add(ct2);

        try {
            analytics.storeCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        List<Cardinality> cards1 = analytics.getCompletionFaultDetails(null,
                new BusinessTransactionCriteria().setName("testapp").setStartTime(1000).setEndTime(10000));

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("fault1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

}
