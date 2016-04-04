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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.ConfigMessage;
import org.hawkular.btm.api.model.config.btxn.Filter;
import org.hawkular.btm.api.model.events.CommunicationDetails;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.api.model.events.NodeDetails;
import org.hawkular.btm.api.services.ConfigurationService;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.api.services.Criteria.FaultCriteria;
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
            fail("Failed to initialise Elasticsearch client: " + e);
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

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0, false);

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

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0, false);

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

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0, false);

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
            public List<ConfigMessage> updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
                    throws Exception {
                return null;
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

            @Override
            public List<ConfigMessage> validateBusinessTransaction(BusinessTxnConfig config) {
                return null;
            }

            @Override
            public void clear(String tenantId) {
                // TODO Auto-generated method stub

            }
        });

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0, false);

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
            public List<ConfigMessage> updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
                    throws Exception {
                return null;
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

            @Override
            public List<ConfigMessage> validateBusinessTransaction(BusinessTxnConfig config) {
                return null;
            }

            @Override
            public void clear(String tenantId) {
                // TODO Auto-generated method stub

            }
        });

        java.util.List<URIInfo> uris = analytics.getUnboundURIs(null, 100, 0, false);

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

        Criteria criteria=new Criteria()
            .setBusinessTransaction("btxn1")
            .setStartTime(100)
            .setEndTime(0);

        java.util.List<PropertyInfo> pis = analytics.getPropertyInfo(null, criteria);

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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(100).setEndTime(0);

        assertEquals(2, analytics.getCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCountForFault() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1 = new CompletionTime();
        ct1.setBusinessTransaction("testapp");
        ct1.setTimestamp(1000);
        cts.add(ct1);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2000);
        ct2.setFault("TestFault");
        cts.add(ct2);

        try {
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.getFaults().add(new FaultCriteria("TestFault", false));
        criteria.setBusinessTransaction("testapp").setStartTime(100).setEndTime(0);

        assertEquals(1, analytics.getCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCountForNotFault() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1 = new CompletionTime();
        ct1.setBusinessTransaction("testapp");
        ct1.setTimestamp(1000);
        cts.add(ct1);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2000);
        ct2.setFault("TestFault1");
        cts.add(ct2);

        CompletionTime ct3 = new CompletionTime();
        ct3.setBusinessTransaction("testapp");
        ct3.setTimestamp(2000);
        ct3.setFault("TestFault2");
        cts.add(ct3);

        try {
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.getFaults().add(new FaultCriteria("TestFault1", true));
        criteria.setBusinessTransaction("testapp").setStartTime(100).setEndTime(0);

        assertEquals(2, analytics.getCompletionCount(null, criteria));
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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(100).setEndTime(0);

        assertEquals(1, analytics.getCompletionFaultCount(null, criteria));
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() {
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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria,
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
    public void testGetCompletionTimeseriesStatisticsWithLowerBound() {
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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setLowerBound(200);
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(2000, stats.get(1).getTimestamp());

        assertEquals(1, stats.get(0).getCount());
        assertEquals(1, stats.get(1).getCount());

        assertTrue(stats.get(0).getMin() == 300);
        assertTrue(stats.get(0).getAverage() == 300);
        assertTrue(stats.get(0).getMax() == 300);

        assertTrue(stats.get(1).getMin() == 500);
        assertTrue(stats.get(1).getAverage() == 500);
        assertTrue(stats.get(1).getMax() == 500);

        assertEquals(0, stats.get(0).getFaultCount());
        assertEquals(0, stats.get(1).getFaultCount());
    }

    @Test
    public void testGetCompletionTimeseriesStatisticsWithUpperBound() {
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(500);
        cts.add(ct1_2);

        CompletionTime ct2 = new CompletionTime();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setDuration(300);
        cts.add(ct2);

        try {
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setUpperBound(400);
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(2000, stats.get(1).getTimestamp());

        assertEquals(1, stats.get(0).getCount());
        assertEquals(1, stats.get(1).getCount());

        assertTrue(stats.get(0).getMin() == 100);
        assertTrue(stats.get(0).getAverage() == 100);
        assertTrue(stats.get(0).getMax() == 100);

        assertTrue(stats.get(1).getMin() == 300);
        assertTrue(stats.get(1).getAverage() == 300);
        assertTrue(stats.get(1).getMax() == 300);

        assertEquals(0, stats.get(0).getFaultCount());
        assertEquals(0, stats.get(1).getFaultCount());
    }

    @Test
    public void testGetCompletionTimeseriesStatisticsWithFaults() {
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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria,
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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<Cardinality> cards1 = analytics.getCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(2, cards1.size());

        assertEquals("value1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
        assertEquals("value2", cards1.get(1).getValue());
        assertEquals(2, cards1.get(1).getCount());

        List<Cardinality> cards2 = analytics.getCompletionPropertyDetails(null, criteria,
                "prop2");

        assertNotNull(cards2);
        assertEquals(2, cards2.size());

        assertEquals("value3", cards2.get(0).getValue());
        assertEquals(1, cards2.get(0).getCount());
        assertEquals("value4", cards2.get(1).getValue());
        assertEquals(1, cards2.get(1).getCount());
    }

    @Test
    public void testGetCompletionPropertyDetailsForFault() {
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
        ct1_2.setFault("TestFault");
        ct1_2.getProperties().put("prop1", "value2");
        cts.add(ct1_2);

        try {
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.getFaults().add(new FaultCriteria("TestFault", false));
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<Cardinality> cards1 = analytics.getCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("value2", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());

    }

    @Test
    public void testGetCompletionPropertyDetailsForExcludedFault() {
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
        ct1_2.setFault("TestFault");
        ct1_2.getProperties().put("prop1", "value2");
        cts.add(ct1_2);

        try {
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.getFaults().add(new FaultCriteria("TestFault", true));
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<Cardinality> cards1 = analytics.getCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("value1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());

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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<Cardinality> cards1 = analytics.getCompletionFaultDetails(null, criteria);

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
            analytics.storeBTxnCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(1000).setEndTime(10000);

        List<Cardinality> cards1 = analytics.getCompletionFaultDetails(null, criteria);

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("fault1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

    @Test
    public void testGetNodeTimeseriesStatistics() {
        List<NodeDetails> nds = new ArrayList<NodeDetails>();

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setComponentType("Database");
        nds.add(ct1_1);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setComponentType("Database");
        nds.add(ct1_2);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setComponentType("EJB");
        nds.add(ct1_3);

        NodeDetails ct2 = new NodeDetails();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setActual(500);
        ct2.setComponentType("Database");
        nds.add(ct2);

        try {
            analytics.storeNodeDetails(null, nds);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(1000).setEndTime(10000);

        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(2000, stats.get(1).getTimestamp());

        assertEquals(2, stats.get(0).getComponentTypes().size());
        assertEquals(1, stats.get(1).getComponentTypes().size());

        assertTrue(stats.get(0).getComponentTypes().containsKey("Database"));
        assertTrue(stats.get(0).getComponentTypes().containsKey("EJB"));
        assertTrue(stats.get(1).getComponentTypes().containsKey("Database"));

        assertTrue(stats.get(0).getComponentTypes().get("Database").getDuration() == 200);
        assertTrue(stats.get(0).getComponentTypes().get("Database").getCount() == 2);
        assertTrue(stats.get(0).getComponentTypes().get("EJB").getDuration() == 150);
        assertTrue(stats.get(0).getComponentTypes().get("EJB").getCount() == 1);
        assertTrue(stats.get(1).getComponentTypes().get("Database").getDuration() == 500);
        assertTrue(stats.get(1).getComponentTypes().get("Database").getCount() == 1);
    }

    @Test
    public void testGetNodeSummaryStatistics() {
        List<NodeDetails> nds = new ArrayList<NodeDetails>();

        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setBusinessTransaction("testapp");
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");
        nds.add(ct1_0);

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType("Database");
        ct1_1.setUri("jdbc");
        nds.add(ct1_1);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType("Database");
        ct1_2.setUri("jdbc");
        nds.add(ct1_2);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType("EJB");
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");
        nds.add(ct1_3);

        try {
            analytics.storeNodeDetails(null, nds);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(1000).setEndTime(10000);

        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(3, stats.size());

        NodeSummaryStatistics dbstat = null;
        NodeSummaryStatistics ejbstat = null;
        NodeSummaryStatistics consumerstat = null;

        for (NodeSummaryStatistics nss : stats) {
            if (nss.getComponentType().equalsIgnoreCase("Database")) {
                dbstat = nss;
            } else if (nss.getComponentType().equalsIgnoreCase("EJB")) {
                ejbstat = nss;
            } else if (nss.getComponentType().equalsIgnoreCase("Consumer")) {
                consumerstat = nss;
            }
        }

        assertTrue(dbstat.getUri().equalsIgnoreCase("jdbc"));
        assertNull(dbstat.getOperation());
        assertEquals(2, dbstat.getCount());
        assertTrue(dbstat.getActual() == 200.0);
        assertTrue(dbstat.getElapsed() == 400.0);

        assertTrue(ejbstat.getUri().equalsIgnoreCase("BookingService"));
        assertTrue(ejbstat.getOperation().equalsIgnoreCase("createBooking"));
        assertEquals(1, ejbstat.getCount());
        assertTrue(ejbstat.getActual() == 150.0);
        assertTrue(ejbstat.getElapsed() == 300.0);

        assertTrue(consumerstat.getUri().equalsIgnoreCase("hello"));
        assertNull(consumerstat.getOperation());
        assertEquals(1, consumerstat.getCount());
        assertTrue(consumerstat.getActual() == 100.0);
        assertTrue(consumerstat.getElapsed() == 200.0);
    }

    @Test
    public void testGetNodeSummaryStatisticsWithBlankHostNameFilter() {
        List<NodeDetails> nds = new ArrayList<NodeDetails>();

        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setBusinessTransaction("testapp");
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");
        ct1_0.setHostName("hostA");
        nds.add(ct1_0);

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType("Database");
        ct1_1.setUri("jdbc");
        ct1_1.setHostName("hostA");
        nds.add(ct1_1);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType("Database");
        ct1_2.setUri("jdbc");
        ct1_2.setHostName("hostB");
        nds.add(ct1_2);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType("EJB");
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");
        ct1_3.setHostName("hostB");
        nds.add(ct1_3);

        try {
            analytics.storeNodeDetails(null, nds);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(1000).setEndTime(10000).setHostName("");

        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(3, stats.size());

        NodeSummaryStatistics dbstat = null;
        NodeSummaryStatistics ejbstat = null;
        NodeSummaryStatistics consumerstat = null;

        for (NodeSummaryStatistics nss : stats) {
            if (nss.getComponentType().equalsIgnoreCase("Database")) {
                dbstat = nss;
            } else if (nss.getComponentType().equalsIgnoreCase("EJB")) {
                ejbstat = nss;
            } else if (nss.getComponentType().equalsIgnoreCase("Consumer")) {
                consumerstat = nss;
            }
        }

        assertTrue(dbstat.getComponentType().equalsIgnoreCase("Database"));
        assertTrue(dbstat.getUri().equalsIgnoreCase("jdbc"));
        assertNull(dbstat.getOperation());
        assertEquals(2, dbstat.getCount());
        assertTrue(dbstat.getActual() == 200.0);
        assertTrue(dbstat.getElapsed() == 400.0);
        assertTrue(ejbstat.getComponentType().equalsIgnoreCase("EJB"));
        assertTrue(ejbstat.getUri().equalsIgnoreCase("BookingService"));
        assertTrue(ejbstat.getOperation().equalsIgnoreCase("createBooking"));
        assertEquals(1, ejbstat.getCount());
        assertTrue(ejbstat.getActual() == 150.0);
        assertTrue(ejbstat.getElapsed() == 300.0);
        assertTrue(consumerstat.getComponentType().equalsIgnoreCase("Consumer"));
        assertTrue(consumerstat.getUri().equalsIgnoreCase("hello"));
        assertNull(consumerstat.getOperation());
        assertEquals(1, consumerstat.getCount());
        assertTrue(consumerstat.getActual() == 100.0);
        assertTrue(consumerstat.getElapsed() == 200.0);
    }

    @Test
    public void testGetNodeSummaryStatisticsWithHostNameFilter() {
        List<NodeDetails> nds = new ArrayList<NodeDetails>();

        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setBusinessTransaction("testapp");
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");
        ct1_0.setHostName("hostA");
        nds.add(ct1_0);

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType("Database");
        ct1_1.setUri("jdbc");
        ct1_1.setHostName("hostA");
        nds.add(ct1_1);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType("Database");
        ct1_2.setUri("jdbc");
        ct1_2.setHostName("hostB");
        nds.add(ct1_2);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType("EJB");
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");
        ct1_3.setHostName("hostB");
        nds.add(ct1_3);

        try {
            analytics.storeNodeDetails(null, nds);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(1000).setEndTime(10000).setHostName("hostA");

        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        NodeSummaryStatistics dbstat = null;
        NodeSummaryStatistics consumerstat = null;

        for (NodeSummaryStatistics nss : stats) {
            if (nss.getComponentType().equalsIgnoreCase("Database")) {
                dbstat = nss;
            } else if (nss.getComponentType().equalsIgnoreCase("Consumer")) {
                consumerstat = nss;
            }
        }

        assertTrue(dbstat.getComponentType().equalsIgnoreCase("Database"));
        assertTrue(dbstat.getUri().equalsIgnoreCase("jdbc"));
        assertNull(dbstat.getOperation());
        assertEquals(1, dbstat.getCount());
        assertTrue(dbstat.getActual() == 100.0);
        assertTrue(dbstat.getElapsed() == 200.0);
        assertTrue(consumerstat.getComponentType().equalsIgnoreCase("Consumer"));
        assertTrue(consumerstat.getUri().equalsIgnoreCase("hello"));
        assertNull(consumerstat.getOperation());
        assertEquals(1, consumerstat.getCount());
        assertTrue(consumerstat.getActual() == 100.0);
        assertTrue(consumerstat.getElapsed() == 200.0);
    }

    @Test
    public void testGetCommunicationSummaryStatisticsWithoutOps() {
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setUri("in1");
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setUri("out1.1");
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        cts.add(ct1_2);

        CompletionTime ct1_3 = new CompletionTime();
        ct1_3.setUri("out1.2");
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1600);
        ct1_3.setDuration(200);
        cts.add(ct1_3);

        CompletionTime ct2_1 = new CompletionTime();
        ct2_1.setUri("in2");
        ct2_1.setBusinessTransaction("testapp");
        ct2_1.setTimestamp(1600);
        ct2_1.setDuration(500);
        cts.add(ct2_1);

        CompletionTime ct2_2 = new CompletionTime();
        ct2_2.setUri("out2.1");
        ct2_2.setBusinessTransaction("testapp");
        ct2_2.setTimestamp(1700);
        ct2_2.setDuration(400);
        cts.add(ct2_2);

        CompletionTime ct2_3 = new CompletionTime();
        ct2_3.setUri("in2");
        ct2_3.setBusinessTransaction("testapp");
        ct2_3.setTimestamp(1700);
        ct2_3.setDuration(600);
        cts.add(ct2_3);

        try {
            analytics.storeFragmentCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("cd1");
        cd1.setBusinessTransaction("testapp");
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource("in1");
        cd1.setTarget("out1.1");
        cds.add(cd1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setId("cd2");
        cd2.setBusinessTransaction("testapp");
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource("in1");
        cd2.setTarget("out1.2");
        cds.add(cd2);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setId("cd3");
        cd3.setBusinessTransaction("testapp");
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource("in2");
        cd3.setTarget("out2.1");
        cds.add(cd3);

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setId("cd4");
        cd4.setBusinessTransaction("testapp");
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource("in1");
        cd4.setTarget("out1.2");
        cds.add(cd4);

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setId("cd5");
        cd5.setBusinessTransaction("testapp");
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource("in2");
        cd5.setTarget("out2.1");
        cds.add(cd5);

        try {
            analytics.storeCommunicationDetails(null, cds);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(1000).setEndTime(10000);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(5, stats.size());

        CommunicationSummaryStatistics in1 = null;
        CommunicationSummaryStatistics in2 = null;
        CommunicationSummaryStatistics out1_1 = null;
        CommunicationSummaryStatistics out1_2 = null;
        CommunicationSummaryStatistics out2_1 = null;

        for (CommunicationSummaryStatistics css : stats) {
            if (css.getId().equals("in1")) {
                in1 = css;
            } else if (css.getId().equals("in2")) {
                in2 = css;
            } else if (css.getId().equals("out1.1")) {
                out1_1 = css;
            } else if (css.getId().equals("out1.2")) {
                out1_2 = css;
            } else if (css.getId().equals("out2.1")) {
                out2_1 = css;
            } else {
                fail("Unexpected uri: " + css.getId());
            }
        }

        assertNotNull(in1);
        assertNotNull(in2);
        assertNotNull(out1_1);
        assertNotNull(out1_2);
        assertNotNull(out2_1);

        assertEquals(1, in1.getCount());
        assertEquals(2, in2.getCount());
        assertEquals(1, out1_1.getCount());
        assertEquals(1, out1_2.getCount());
        assertEquals(1, out2_1.getCount());

        // Only check in2 node, as others only have single completion time
        assertTrue(500 == in2.getMinimumDuration());
        assertTrue(550 == in2.getAverageDuration());
        assertTrue(600 == in2.getMaximumDuration());

        assertEquals(2, in1.getOutbound().size());
        assertEquals(1, in2.getOutbound().size());
        assertEquals(0, out1_1.getOutbound().size());
        assertEquals(0, out1_2.getOutbound().size());
        assertEquals(0, out2_1.getOutbound().size());

        assertTrue(in1.getOutbound().containsKey("out1.1"));
        assertTrue(in1.getOutbound().containsKey("out1.2"));
        assertTrue(in2.getOutbound().containsKey("out2.1"));

        assertTrue(100 == in1.getOutbound().get("out1.1").getMinimumLatency());
        assertTrue(100 == in1.getOutbound().get("out1.1").getAverageLatency());
        assertTrue(100 == in1.getOutbound().get("out1.1").getMaximumLatency());
        assertTrue(200 == in1.getOutbound().get("out1.2").getMinimumLatency());
        assertTrue(250 == in1.getOutbound().get("out1.2").getAverageLatency());
        assertTrue(300 == in1.getOutbound().get("out1.2").getMaximumLatency());
        assertTrue(300 == in2.getOutbound().get("out2.1").getMinimumLatency());
        assertTrue(400 == in2.getOutbound().get("out2.1").getAverageLatency());
        assertTrue(500 == in2.getOutbound().get("out2.1").getMaximumLatency());

        assertEquals(1, in1.getOutbound().get("out1.1").getCount());
        assertEquals(2, in1.getOutbound().get("out1.2").getCount());
        assertEquals(2, in2.getOutbound().get("out2.1").getCount());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsWithOps() {
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();
        List<CompletionTime> cts = new ArrayList<CompletionTime>();

        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setUri("in1");
        ct1_1.setOperation("op1");
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        cts.add(ct1_1);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setUri("out1.1");
        ct1_2.setOperation("op1.1");
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        cts.add(ct1_2);

        CompletionTime ct1_3 = new CompletionTime();
        ct1_3.setUri("out1.2");
        ct1_3.setOperation("op1.2");
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1600);
        ct1_3.setDuration(200);
        cts.add(ct1_3);

        CompletionTime ct2_1 = new CompletionTime();
        ct2_1.setUri("in2");
        ct2_1.setOperation("op2");
        ct2_1.setBusinessTransaction("testapp");
        ct2_1.setTimestamp(1600);
        ct2_1.setDuration(500);
        cts.add(ct2_1);

        CompletionTime ct2_2 = new CompletionTime();
        ct2_2.setUri("out2.1");
        ct2_2.setOperation("op2.1");
        ct2_2.setBusinessTransaction("testapp");
        ct2_2.setTimestamp(1700);
        ct2_2.setDuration(400);
        cts.add(ct2_2);

        CompletionTime ct2_3 = new CompletionTime();
        ct2_3.setUri("in2");
        ct2_3.setOperation("op2");
        ct2_3.setBusinessTransaction("testapp");
        ct2_3.setTimestamp(1700);
        ct2_3.setDuration(600);
        cts.add(ct2_3);

        try {
            analytics.storeFragmentCompletionTimes(null, cts);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("cd1");
        cd1.setBusinessTransaction("testapp");
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource("in1[op1]");
        cd1.setTarget("out1.1[op1.1]");
        cds.add(cd1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setId("cd2");
        cd2.setBusinessTransaction("testapp");
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource("in1[op1]");
        cd2.setTarget("out1.2[op1.2]");
        cds.add(cd2);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setId("cd3");
        cd3.setBusinessTransaction("testapp");
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource("in2[op2]");
        cd3.setTarget("out2.1[op2.1]");
        cds.add(cd3);

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setId("cd4");
        cd4.setBusinessTransaction("testapp");
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource("in1[op1]");
        cd4.setTarget("out1.2[op1.2]");
        cds.add(cd4);

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setId("cd5");
        cd5.setBusinessTransaction("testapp");
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource("in2[op2]");
        cd5.setTarget("out2.1[op2.1]");
        cds.add(cd5);

        try {
            analytics.storeCommunicationDetails(null, cds);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store: " + e);
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(1000).setEndTime(10000);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(5, stats.size());

        CommunicationSummaryStatistics in1 = null;
        CommunicationSummaryStatistics in2 = null;
        CommunicationSummaryStatistics out1_1 = null;
        CommunicationSummaryStatistics out1_2 = null;
        CommunicationSummaryStatistics out2_1 = null;

        for (CommunicationSummaryStatistics css : stats) {
            if (css.getId().equals("in1[op1]")) {
                in1 = css;
            } else if (css.getId().equals("in2[op2]")) {
                in2 = css;
            } else if (css.getId().equals("out1.1[op1.1]")) {
                out1_1 = css;
            } else if (css.getId().equals("out1.2[op1.2]")) {
                out1_2 = css;
            } else if (css.getId().equals("out2.1[op2.1]")) {
                out2_1 = css;
            } else {
                fail("Unexpected id: " + css.getId());
            }
        }

        assertNotNull(in1);
        assertNotNull(in2);
        assertNotNull(out1_1);
        assertNotNull(out1_2);
        assertNotNull(out2_1);

        assertEquals(1, in1.getCount());
        assertEquals(2, in2.getCount());
        assertEquals(1, out1_1.getCount());
        assertEquals(1, out1_2.getCount());
        assertEquals(1, out2_1.getCount());

        // Only check in2 node, as others only have single completion time
        assertTrue(500 == in2.getMinimumDuration());
        assertTrue(550 == in2.getAverageDuration());
        assertTrue(600 == in2.getMaximumDuration());

        assertEquals(2, in1.getOutbound().size());
        assertEquals(1, in2.getOutbound().size());
        assertEquals(0, out1_1.getOutbound().size());
        assertEquals(0, out1_2.getOutbound().size());
        assertEquals(0, out2_1.getOutbound().size());

        assertTrue(in1.getOutbound().containsKey("out1.1[op1.1]"));
        assertTrue(in1.getOutbound().containsKey("out1.2[op1.2]"));
        assertTrue(in2.getOutbound().containsKey("out2.1[op2.1]"));

        assertTrue(100 == in1.getOutbound().get("out1.1[op1.1]").getMinimumLatency());
        assertTrue(100 == in1.getOutbound().get("out1.1[op1.1]").getAverageLatency());
        assertTrue(100 == in1.getOutbound().get("out1.1[op1.1]").getMaximumLatency());
        assertTrue(200 == in1.getOutbound().get("out1.2[op1.2]").getMinimumLatency());
        assertTrue(250 == in1.getOutbound().get("out1.2[op1.2]").getAverageLatency());
        assertTrue(300 == in1.getOutbound().get("out1.2[op1.2]").getMaximumLatency());
        assertTrue(300 == in2.getOutbound().get("out2.1[op2.1]").getMinimumLatency());
        assertTrue(400 == in2.getOutbound().get("out2.1[op2.1]").getAverageLatency());
        assertTrue(500 == in2.getOutbound().get("out2.1[op2.1]").getMaximumLatency());

        assertEquals(1, in1.getOutbound().get("out1.1[op1.1]").getCount());
        assertEquals(2, in1.getOutbound().get("out1.2[op1.2]").getCount());
        assertEquals(2, in2.getOutbound().get("out2.1[op2.1]").getCount());
    }

    @Test
    public void testHostNames() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(1000);
        btxn1.setHostName("hostA");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setStartTime(2000);
        btxn2.setHostName("hostB");
        btxns.add(btxn2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        java.util.List<String> hostnames = analytics.getHostNames(null, new Criteria().setStartTime(100));

        assertNotNull(hostnames);
        assertEquals(2, hostnames.size());

        assertEquals("hostA", hostnames.get(0));
        assertEquals("hostB", hostnames.get(1));
    }

}
