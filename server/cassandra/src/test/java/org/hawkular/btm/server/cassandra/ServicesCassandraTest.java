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
package org.hawkular.btm.server.cassandra;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cassandra.service.CassandraDaemon;
import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.EndpointInfo;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.Filter;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.api.model.events.NodeDetails;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.api.services.Criteria.FaultCriteria;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ServicesCassandraTest {

    /**  */
    private static final String VALID_DESCRIPTION = "Valid description";

    /**  */
    private static final String INVALID_DESCRIPTION = "Invalid description";

    private ConfigurationServiceCassandra cfgs;

    private BusinessTransactionServiceCassandra bts;

    private AnalyticsServiceCassandra analytics;

    private CassandraClient client;

    private static CassandraDaemon cassandraDaemon = null;

    @BeforeClass
    public static void init() {
        System.setProperty("cassandra.config", "cassandra/cassandra.yaml");
        System.setProperty("cassandra-foreground", "true");
        System.setProperty("cassandra.native.epoll.enabled", "false"); // JNA doesnt cope with relocated netty

        final CountDownLatch startupLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                cassandraDaemon = new CassandraDaemon();
                cassandraDaemon.activate();
                startupLatch.countDown();
            }
        });
        try {
            if (!startupLatch.await(30000, MILLISECONDS)) {
                throw new AssertionError("Cassandra daemon did not start within timeout");
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            executor.shutdown();
        }
    }

    @Before
    public void beforeTest() {
        client = new CassandraClient();
        try {
            client.init();
        } catch (Exception e) {
            fail("Failed to initialise Cassandra client: " + e);
        }

        // BusinessTransactionService related
        bts = new BusinessTransactionServiceCassandra();
        bts.setClient(client);
        bts.init();

        client.getSession().execute("TRUNCATE hawkular_btm.businesstransactions;");

        // ConfigurationService related
        cfgs = new ConfigurationServiceCassandra();
        cfgs.setClient(client);
        cfgs.init();

        client.getSession().execute("TRUNCATE hawkular_btm.businesstxnconfig;");
        client.getSession().execute("TRUNCATE hawkular_btm.businesstxnconfiginvalid;");

        // AnalyticsService related
        analytics = new AnalyticsServiceCassandra();
        analytics.setClient(client);
        analytics.setBusinessTransactionService(bts);
        analytics.init();

        client.getSession().execute("TRUNCATE hawkular_btm.nodedetails;");
        client.getSession().execute("TRUNCATE hawkular_btm.completiontimes;");
    }

    @After
    public void afterTest() {
        //bts.clear(null);
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
            e.printStackTrace();
            fail("Failed to store@ " + e);
        }

        Criteria criteria = new Criteria();
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

        Criteria criteria = new Criteria();
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

        Criteria criteria = new Criteria();
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

        Criteria criteria = new Criteria();
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

        Criteria criteria = new Criteria();
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

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", true);
        criteria.addProperty("prop1", "value3", true);

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id2", result1.get(0).getId());
    }

    @Test
    public void testQueryCorrelationId() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setStartTime(1000);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.addGlobalId("gid1");
        btxn1.getNodes().add(c1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
        btxn2.setStartTime(2000);
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.addGlobalId("gid2");
        btxn2.getNodes().add(c2);

        try {
            bts.storeBusinessTransactions(null, btxns);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "gid1"));

        List<BusinessTransaction> result1 = bts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
    }

    // ConfigurationService related tests

    @Test
    public void testGetBusinessTransactionsUpdated() {
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
        btc2.setFilter(new Filter());
        btc2.getFilter().getInclusions().add("myfilter");

        try {
            cfgs.setBusinessTransaction(null, "btc2", btc2);
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
    public void testGetBusinessTransactionsInvalid() {
        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            // Updating invalid config
            cfgs.setBusinessTransaction(null, "btc1", btc1);
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
        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription(VALID_DESCRIPTION);
        btc1.setFilter(new Filter());
        btc1.getFilter().getInclusions().add("myfilter");

        try {
            // Updating valid config
            cfgs.setBusinessTransaction(null, "btc1", btc1);
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

        btc1.setDescription(INVALID_DESCRIPTION);
        btc1.setFilter(null);

        try {
            // Updating with invalid config
            cfgs.setBusinessTransaction(null, "btc1", btc1);
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
    }

    @Test
    public void testGetBusinessTransactionsAfterRemove() {
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

    @Test
    public void testGetBusinessTransactionsAfterRemoveInvalid() {
        BusinessTxnConfig btc1first = cfgs.getBusinessTransaction(null, "btc1");
        assertNull("Should be null at beginning of test", btc1first);

        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        btc1.setDescription("btc1");

        try {
            cfgs.setBusinessTransaction(null, "btc1", btc1);
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

        try {
            cfgs.removeBusinessTransaction(null, "btc1");
        } catch (Exception e) {
            fail("Failed to remove btc1: " + e);
        }

        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        BusinessTxnConfig btc1again = cfgs.getBusinessTransaction(null, "btc1");
        assertNull(btc1again);
    }

    // AnalyticService tests

    @Test
    public void testBoundURIs() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
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
        btxn2.setId("id2");
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
            fail("Failed to wait: " + e);
        }

        java.util.List<EndpointInfo> uris1 = analytics.getBoundEndpoints(null, "btxn1", 100, 0);

        assertNotNull(uris1);
        assertEquals(3, uris1.size());
        assertTrue(uris1.contains(new EndpointInfo("uri1")));
        assertTrue(uris1.contains(new EndpointInfo("uri2")));
        assertTrue(uris1.contains(new EndpointInfo("uri3")));

        java.util.List<EndpointInfo> uris2 = analytics.getBoundEndpoints(null, "btxn2", 100, 0);

        assertNotNull(uris2);
        assertEquals(1, uris2.size());
        assertTrue(uris2.contains(new EndpointInfo("uri4")));
    }

    @Test
    public void testPropertyInfo() {
        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("id1");
        btxn1.setName("btxn1");
        btxn1.setStartTime(1000);
        btxn1.getProperties().put("prop1", "value1");
        btxn1.getProperties().put("prop2", "value2");
        btxns.add(btxn1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("id2");
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
    public void testGetNodeTimeseriesStatistics() {
        List<NodeDetails> nds = new ArrayList<NodeDetails>();

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setBusinessTransaction("testapp");
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType("Database");
        nds.add(ct1_1);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setBusinessTransaction("testapp");
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setComponentType("Database");
        ct1_2.setType(NodeType.Component);
        nds.add(ct1_2);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setBusinessTransaction("testapp");
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setComponentType("EJB");
        ct1_3.setType(NodeType.Component);
        nds.add(ct1_3);

        NodeDetails ct2 = new NodeDetails();
        ct2.setBusinessTransaction("testapp");
        ct2.setTimestamp(2100);
        ct2.setActual(500);
        ct2.setComponentType("Database");
        ct2.setType(NodeType.Component);
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

}
