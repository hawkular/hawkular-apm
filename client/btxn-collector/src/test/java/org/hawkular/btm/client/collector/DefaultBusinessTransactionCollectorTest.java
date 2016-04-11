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
package org.hawkular.btm.client.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.ReportingLevel;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.ConfigMessage;
import org.hawkular.btm.api.model.config.btxn.Filter;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.api.services.ConfigurationService;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.client.collector.internal.FragmentBuilder;
import org.junit.Test;

/**
 * @author gbrown
 */
public class DefaultBusinessTransactionCollectorTest {

    /**  */
    private static final String BTXN_PRINCIPAL = "BTxnPrincipal";
    /**  */
    private static final String BTXN_NAME = "BTxnName";
    /**  */
    private static final String TEST_TENANT = "TestTenant";
    /**  */
    private static final String TYPE = "TestType";
    /**  */
    private static final String URI = "TestURI";
    /**  */
    private static final String OP = "TestOP";

    @Test
    public void testSetStartTimeAndDuration() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setConfigurationService(new TestConfigurationService());

        collector.consumerStart(null, URI, TYPE, OP, null);

        // Delay, to provide a reasonable value for duration
        synchronized (this) {
            try {
                wait(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        collector.consumerEnd(null, URI, TYPE, OP);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals("Only 1 business transaction expected", 1, btxns.size());

        BusinessTransaction btxn = btxns.get(0);

        assertEquals("Single node", 1, btxn.getNodes().size());

        Node node = btxn.getNodes().get(0);

        assertTrue("Start time not set", node.getBaseTime() > 0);
        assertTrue("Duration not set", node.getDuration() > 0);
    }

    @Test
    public void testTenantIdSystemProperty() {
        System.setProperty("hawkular-btm.tenantId", TEST_TENANT);

        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setConfigurationService(new TestConfigurationService());

        collector.consumerStart(null, URI, TYPE, OP, null);

        collector.consumerEnd(null, URI, TYPE, OP);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Clear property
        System.getProperties().remove("hawkular-btm.tenantId");

        assertNotNull("TenantId should not be null", btxnService.getTenantId());

        assertEquals("TenantId incorrect", TEST_TENANT, btxnService.getTenantId());
    }

    @Test
    public void testIncludeHeaders() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setConfigurationService(new TestConfigurationService());

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        Map<String, String> respHeaders = new HashMap<String, String>();
        respHeaders.put("joe", "bloggs");

        collector.consumerStart(null, URI, TYPE, OP, null);
        collector.processIn(null, reqHeaders);
        collector.processOut(null, respHeaders);
        collector.consumerEnd(null, URI, TYPE, OP);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals("Only 1 business transaction expected", 1, btxns.size());

        BusinessTransaction btxn = btxns.get(0);

        assertEquals("Single node", 1, btxn.getNodes().size());

        Node node = btxn.getNodes().get(0);

        Consumer service = (Consumer) node;

        assertEquals(service.getIn().getHeaders().get("hello"), "world");
        assertEquals(service.getOut().getHeaders().get("joe"), "bloggs");
    }

    @Test
    public void testIncludeHeadersNotProcessedAgain() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setConfigurationService(new TestConfigurationService());

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        // Second headers will not be processed - only the first
        Map<String, String> reqHeaders2 = new HashMap<String, String>();
        reqHeaders2.put("joe", "bloggs");

        collector.consumerStart(null, URI, TYPE, OP, null);
        collector.processIn(null, reqHeaders);
        collector.processIn(null, reqHeaders2);
        collector.consumerEnd(null, URI, TYPE, OP);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals("Only 1 business transaction expected", 1, btxns.size());

        BusinessTransaction btxn = btxns.get(0);

        assertEquals("Single node", 1, btxn.getNodes().size());

        Node node = btxn.getNodes().get(0);

        Consumer service = (Consumer) node;

        assertEquals(service.getIn().getHeaders().get("hello"), "world");
        assertFalse(service.getIn().getHeaders().containsKey("joe"));
    }

    @Test
    public void testIncludeHeadersSuppliedSecondCall() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setConfigurationService(new TestConfigurationService());

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        collector.consumerStart(null, URI, TYPE, OP, null);
        collector.processIn(null, null);
        collector.processIn(null, reqHeaders);
        collector.consumerEnd(null, URI, TYPE, OP);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals("Only 1 business transaction expected", 1, btxns.size());

        BusinessTransaction btxn = btxns.get(0);

        assertEquals("Single node", 1, btxn.getNodes().size());

        Node node = btxn.getNodes().get(0);

        Consumer service = (Consumer) node;

        assertEquals(service.getIn().getHeaders().get("hello"), "world");
    }

    @Test
    public void testIncludeBTMID() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setConfigurationService(new TestConfigurationService());

        collector.consumerStart(null, null, null, null, "myid");

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals("Only 1 business transaction expected", 1, btxns.size());

        BusinessTransaction btxn = btxns.get(0);

        assertEquals("Single node", 1, btxn.getNodes().size());

        Node node = btxn.getNodes().get(0);

        assertTrue("Should be 1 correlation id", node.getCorrelationIds().size() == 1);

        CorrelationIdentifier cid = node.getCorrelationIds().iterator().next();
        assertEquals(CorrelationIdentifier.Scope.Interaction, cid.getScope());
        assertEquals("myid", cid.getValue());
    }

    @Test
    public void testReportingLevelNoneByFilter() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setLevel(ReportingLevel.None);
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", null);

        collector.consumerStart(null, "/test", null, null, null);

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals(0, btxns.size());
    }

    @Test
    public void testReportingWithOpLevelNoneByFilter() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setLevel(ReportingLevel.None);
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", "op");

        collector.consumerStart(null, "/test", null, "op", null);

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals(0, btxns.size());
    }

    @Test
    public void testReportingLevelNoneBySetter() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", null);

        collector.setLevel(null, "None");

        collector.consumerStart(null, "/test", null, null, null);

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals(0, btxns.size());
    }

    @Test
    public void testReportingWithOpLevelNoneBySetter() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", "op");

        collector.setLevel(null, "None");

        collector.consumerStart(null, "/test", null, "op", null);

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals(0, btxns.size());
    }

    @Test
    public void testReportingLevelAll() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", null);

        collector.consumerStart(null, "/test", null, null, null);

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals(1, btxns.size());
    }

    @Test
    public void testReportingWithOpLevelAll() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", "op");

        collector.consumerStart(null, "/test", null, "op", null);

        collector.consumerEnd(null, null, null, null);

        // Delay necessary as reporting the business transaction is performed in a separate
        // thread
        synchronized (this) {
            try {
                wait(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<BusinessTransaction> btxns = btxnService.getBusinessTransactions();

        assertEquals(1, btxns.size());
    }

    @Test
    public void testCorrelation() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        final FragmentBuilder fragmentBuilder = collector.getFragmentManager().getFragmentBuilder();

        collector.initiateCorrelation("TestLink");

        assertFalse(fragmentBuilder.getUncompletedCorrelationIdsNodeMap().isEmpty());

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                collector.completeCorrelation("TestLink", true);

                FragmentBuilder other = collector.getFragmentManager().getFragmentBuilder();

                assertEquals("Builders should be the same", fragmentBuilder, other);

                // Check link is no marked as unresolved
                assertTrue(other.getUncompletedCorrelationIdsNodeMap().isEmpty());
            }
        });
    }

    @Test
    public void testIsActive() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.getFragmentManager().getFragmentBuilder();

        assertTrue(collector.isActive());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testActivateUnknownURI() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.activate("/test", null);

        assertFalse(collector.isActive());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testActivateWithOpUnknownURI() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.activate("/test", "op");

        assertFalse(collector.isActive());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetNameNoFragmentManager() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        collector.setName(null, "test");

        assertEquals("test", collector.getName());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetNameWithFragmentManager() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        collector.setName(null, "test");

        assertEquals("test", collector.getName());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetPrincipalNoFragmentManager() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        collector.setPrincipal(null, "test");

        assertEquals("", collector.getPrincipal());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetPrincipalWithFragmentManager() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        collector.setPrincipal(null, "test");

        assertEquals("test", collector.getPrincipal());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnInitialNode() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        assertTrue(collector.isActive());
        assertEquals("testapp", collector.getName());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnSubsequentNodeInitialFragment() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Create top level node
        collector.activate("not relevant", null);
        collector.componentStart(null, "not relevant", "Database", "query");

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        assertTrue(collector.isActive());
        assertEquals("testapp", collector.getName());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnSubsequentNodeInitialFragmentWithOp() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Create top level node
        collector.activate("not relevant", null);
        collector.componentStart(null, "not relevant", "Database", "query");

        // Cause a fragment builder to be created
        collector.activate("/test", "op");
        collector.producerStart(null, "/test", "HTTP", "op", null);

        assertTrue(collector.isActive());
        assertEquals("testapp", collector.getName());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnSubsequentNodeLaterFragment() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        BusinessTxnConfig btc = new BusinessTxnConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getBusinessTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Create top level node
        collector.activate("not relevant", null);
        collector.consumerStart(null, "not relevant", "HTTP", null, null);
        collector.getFragmentManager().getFragmentBuilder()
            .getBusinessTransaction().getNodes().get(0).addInteractionId("testId");

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        assertTrue(collector.isActive());
        assertEquals("", collector.getName());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetDetailsCurrentNode() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        // Cause a fragment builder to be created
        FragmentBuilder builder = collector.getFragmentManager().getFragmentBuilder();

        collector.consumerStart(null, "testconsumer", "testtype", "testop", "testid");

        Node node = builder.getCurrentNode();

        assertNotNull(node);

        collector.setDetail(null, "testname", "testvalue", null, true);

        assertTrue(node.getDetails().containsKey("testname"));

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetDetailsOnStack() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        // Cause a fragment builder to be created
        FragmentBuilder builder = collector.getFragmentManager().getFragmentBuilder();

        collector.consumerStart(null, "testconsumer", "testtype", "testop", "testid");

        Consumer consumer = (Consumer) builder.getCurrentNode();

        assertNotNull(consumer);

        collector.componentStart(null, "testcomponent", "testcomptype", "testcompop");

        Component component = (Component) builder.getCurrentNode();

        assertNotNull(component);

        collector.setDetail(null, "testname", "testvalue", "Consumer", true);

        assertTrue(consumer.getDetails().containsKey("testname"));
        assertFalse(component.getDetails().containsKey("testname"));

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetDetailsPoppedNode() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        // Cause a fragment builder to be created
        FragmentBuilder builder = collector.getFragmentManager().getFragmentBuilder();

        collector.consumerStart(null, "testconsumer", "testcontype", "testop", "testconid");

        Consumer consumer = (Consumer) builder.getCurrentNode();

        assertNotNull(consumer);

        collector.componentStart(null, "testcomponent1", "testcomptype", "testcompop");

        Component component1 = (Component) builder.getCurrentNode();

        assertNotNull(component1);

        collector.componentStart(null, "testcomponent2", "testcomptype", "testcompop");

        Component component2 = (Component) builder.getCurrentNode();

        assertNotNull(component2);

        collector.producerStart(null, "testproducer", "testprodtype", "testop", "tesprodid");

        Producer producer = (Producer) builder.getCurrentNode();

        assertNotNull(producer);

        // Pop the producer and one of the components
        collector.producerEnd(null, "testproducer", "testprodtype", "testop");
        collector.componentEnd(null, "testcomponent2", "testcomptype", "testcompop");

        collector.setDetail(null, "testname", "testvalue", "Producer", false);

        assertTrue(producer.getDetails().containsKey("testname"));
        assertFalse(consumer.getDetails().containsKey("testname"));
        assertFalse(component1.getDetails().containsKey("testname"));

        collector.getFragmentManager().clear();
    }

    @Test
    public void testMergeDuplicateProducers() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        // Cause a fragment builder to be created
        FragmentBuilder builder = collector.getFragmentManager().getFragmentBuilder();

        collector.consumerStart(null, "testconsumer", "testcontype", "testop", "testconid");

        Consumer consumer = (Consumer) builder.getCurrentNode();

        assertNotNull(consumer);

        collector.producerStart(null, "testproducer", "testprodtype", "testop", "testprodid1");

        Producer producerOuter = (Producer) builder.getCurrentNode();

        assertNotNull(producerOuter);

        collector.producerStart(null, "testproducer", "testprodtype", "testop", "testprodid2");

        Producer producerInner = (Producer) builder.getCurrentNode();

        assertNotNull(producerInner);

        // Before merge
        assertTrue(producerOuter.getNodes().contains(producerInner));
        assertFalse(producerOuter.getCorrelationIds().isEmpty());
        assertTrue(producerOuter.getCorrelationIds().get(0).getScope() == Scope.Interaction);
        assertTrue(producerOuter.getCorrelationIds().get(0).getValue().equals("testprodid1"));

        // Pop the producer and one of the components
        collector.producerEnd(null, "testproducer", "testprodtype", "testop");
        collector.producerEnd(null, "testproducer", "testprodtype", "testop");

        // After merge
        assertFalse(producerOuter.getNodes().contains(producerInner));
        assertFalse(producerOuter.getCorrelationIds().isEmpty());
        assertTrue(producerOuter.getCorrelationIds().get(0).getScope() == Scope.Interaction);
        assertTrue(producerOuter.getCorrelationIds().get(0).getValue().equals("testprodid2"));

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSpawnFragmentUsingAddChild() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        FragmentBuilder parent = new FragmentBuilder();
        FragmentBuilder spawned = new FragmentBuilder();

        BusinessTransaction parentBTxn = parent.getBusinessTransaction();
        BusinessTransaction spawnedBTxn = spawned.getBusinessTransaction();

        parentBTxn.setName(BTXN_NAME);
        parentBTxn.setPrincipal(BTXN_PRINCIPAL);

        // Create top level consumer in parent
        Consumer parentConsumer = new Consumer();
        parentConsumer.setUri(URI);
        parentConsumer.setOperation(OP);

        collector.push(null, parent, parentConsumer);

        collector.spawnFragment(parent, parentConsumer, spawned);

        // Check that parent consumer has Producer as child
        assertEquals(1, parentConsumer.getNodes().size());
        assertTrue(parentConsumer.getNodes().get(0) instanceof Producer);

        Producer internalProducer=(Producer)parentConsumer.getNodes().get(0);
        assertEquals(URI, internalProducer.getUri());
        assertEquals(OP, internalProducer.getOperation());

        // Check that spawned fragment has Consumer as top level node
        assertEquals(1, spawnedBTxn.getNodes().size());
        assertTrue(spawnedBTxn.getNodes().get(0) instanceof Consumer);

        Consumer internalConsumer=(Consumer)spawnedBTxn.getNodes().get(0);
        assertEquals(URI, internalConsumer.getUri());
        assertEquals(OP, internalConsumer.getOperation());

        // Check that internal producer and consumer share common interaction id
        List<CorrelationIdentifier> ipids = internalProducer.getCorrelationIds(Scope.Interaction);
        List<CorrelationIdentifier> icids = internalConsumer.getCorrelationIds(Scope.Interaction);

        assertEquals(1, ipids.size());
        assertEquals(1, icids.size());
        assertEquals(ipids.get(0), icids.get(0));

        // Check business transaction details transferred
        assertEquals(BTXN_NAME, spawnedBTxn.getName());
        assertEquals(BTXN_PRINCIPAL, spawnedBTxn.getPrincipal());
    }

    @Test
    public void testSpawnFragmentUsingPush() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        FragmentBuilder parent = new FragmentBuilder();
        FragmentBuilder spawned = new FragmentBuilder();

        BusinessTransaction parentBTxn = parent.getBusinessTransaction();
        BusinessTransaction spawnedBTxn = spawned.getBusinessTransaction();

        parentBTxn.setName(BTXN_NAME);
        parentBTxn.setPrincipal(BTXN_PRINCIPAL);

        // Create top level consumer in parent
        Consumer parentConsumer = new Consumer();
        parentConsumer.setUri(URI);
        parentConsumer.setOperation(OP);

        collector.push(null, parent, parentConsumer);

        collector.spawnFragment(parent, null, spawned);

        // Check that parent consumer has Producer as child
        assertEquals(1, parentConsumer.getNodes().size());
        assertTrue(parentConsumer.getNodes().get(0) instanceof Producer);

        Producer internalProducer=(Producer)parentConsumer.getNodes().get(0);
        assertEquals(URI, internalProducer.getUri());
        assertEquals(OP, internalProducer.getOperation());

        // Check that spawned fragment has Consumer as top level node
        assertEquals(1, spawnedBTxn.getNodes().size());
        assertTrue(spawnedBTxn.getNodes().get(0) instanceof Consumer);

        Consumer internalConsumer=(Consumer)spawnedBTxn.getNodes().get(0);
        assertEquals(URI, internalConsumer.getUri());
        assertEquals(OP, internalConsumer.getOperation());

        // Check that internal producer and consumer share common interaction id
        List<CorrelationIdentifier> ipids = internalProducer.getCorrelationIds(Scope.Interaction);
        List<CorrelationIdentifier> icids = internalConsumer.getCorrelationIds(Scope.Interaction);

        assertEquals(1, ipids.size());
        assertEquals(1, icids.size());
        assertEquals(ipids.get(0), icids.get(0));

        // Check business transaction details transferred
        assertEquals(BTXN_NAME, spawnedBTxn.getName());
        assertEquals(BTXN_PRINCIPAL, spawnedBTxn.getPrincipal());
    }

    public static class TestBTxnService implements BusinessTransactionService, BusinessTransactionPublisher {

        private List<BusinessTransaction> businessTransactions = new ArrayList<BusinessTransaction>();
        private String tenantId;

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionPublisher#publish(java.lang.String, java.util.List)
         */
        @Override
        public void publish(String tenantId, List<BusinessTransaction> btxns) throws Exception {
            this.tenantId = tenantId;
            businessTransactions.addAll(btxns);
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.Publisher#publish(java.lang.String, java.util.List, int, long)
         */
        @Override
        public void publish(String tenantId, List<BusinessTransaction> items, int retryCount, long delay)
                                    throws Exception {
            publish(tenantId, items);
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
         */
        @Override
        public BusinessTransaction get(String tenantId, String id) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
         *              org.hawkular.btm.api.services.Criteria)
         */
        @Override
        public List<BusinessTransaction> query(String tenantId, Criteria criteria) {
            return null;
        }

        /**
         * @return the businessTransactions
         */
        public List<BusinessTransaction> getBusinessTransactions() {
            return businessTransactions;
        }

        /**
         * @param businessTransactions the businessTransactions to set
         */
        public void setBusinessTransactions(List<BusinessTransaction> businessTransactions) {
            this.businessTransactions = businessTransactions;
        }

        /**
         * @return the tenantId
         */
        public String getTenantId() {
            return tenantId;
        }

        /**
         * @param tenantId the tenantId to set
         */
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#storeBusinessTransactions(java.lang.String,
         *                              java.util.List)
         */
        @Override
        public void storeBusinessTransactions(String tenantId, List<BusinessTransaction> businessTransactions)
                throws Exception {
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#clear(java.lang.String)
         */
        @Override
        public void clear(String tenantId) {
            // TODO Auto-generated method stub

        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.Publisher#getInitialRetryCount()
         */
        @Override
        public int getInitialRetryCount() {
            return 0;
        }

    }

    public class TestConfigurationService implements ConfigurationService {

        private CollectorConfiguration config = new CollectorConfiguration();

        protected void setCollectorConfiguration(CollectorConfiguration cc) {
            config = cc;
        }

        @Override
        public CollectorConfiguration getCollector(String tenantId, String host, String server) {
            return config;
        }

        @Override
        public List<ConfigMessage> updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config) {
            return null;
        }

        @Override
        public List<ConfigMessage> validateBusinessTransaction(BusinessTxnConfig config) {
            return null;
        }

        @Override
        public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
            return null;
        }

        @Override
        public void removeBusinessTransaction(String tenantId, String name) {
        }

        @Override
        public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
            return null;
        }

        @Override
        public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.ConfigurationService#clear(java.lang.String)
         */
        @Override
        public void clear(String tenantId) {
            // TODO Auto-generated method stub

        }
    }
}
