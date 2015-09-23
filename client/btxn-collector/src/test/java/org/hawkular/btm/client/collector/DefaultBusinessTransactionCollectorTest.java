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

import org.hawkular.btm.api.model.admin.CollectorConfiguration;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.services.AdminService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.client.collector.internal.FragmentBuilder;
import org.junit.Test;

/**
 * @author gbrown
 */
public class DefaultBusinessTransactionCollectorTest {

    /**  */
    private static final String TEST_TENANT = "TestTenant";
    /**  */
    private static final String TYPE = "TestType";
    /**  */
    private static final String URI = "TestURI";

    @Test
    public void testSetStartTimeAndDuration() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();
        TestBTxnService btxnService = new TestBTxnService();
        collector.setBusinessTransactionPublisher(btxnService);
        collector.setAdminService(new AdminService() {
            @Override
            public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
                return new CollectorConfiguration();
            }
        });

        collector.consumerStart(null, URI, TYPE, null);

        // Delay, to provide a reasonable value for duration
        synchronized (this) {
            try {
                wait(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        collector.consumerEnd(null, URI, TYPE);

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
        collector.setAdminService(new AdminService() {
            @Override
            public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
                return new CollectorConfiguration();
            }
        });

        collector.consumerStart(null, URI, TYPE, null);

        collector.consumerEnd(null, URI, TYPE);

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
        collector.setAdminService(new AdminService() {
            @Override
            public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
                return new CollectorConfiguration();
            }
        });

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        Map<String, String> respHeaders = new HashMap<String, String>();
        respHeaders.put("joe", "bloggs");

        collector.consumerStart(null, URI, TYPE, null);
        collector.processIn(null, reqHeaders);
        collector.processOut(null, respHeaders);
        collector.consumerEnd(null, URI, TYPE);

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
        collector.setAdminService(new AdminService() {
            @Override
            public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
                return new CollectorConfiguration();
            }
        });

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        // Second headers will not be processed - only the first
        Map<String, String> reqHeaders2 = new HashMap<String, String>();
        reqHeaders2.put("joe", "bloggs");

        collector.consumerStart(null, URI, TYPE, null);
        collector.processIn(null, reqHeaders);
        collector.processIn(null, reqHeaders2);
        collector.consumerEnd(null, URI, TYPE);

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
        collector.setAdminService(new AdminService() {
            @Override
            public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
                return new CollectorConfiguration();
            }
        });

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        collector.consumerStart(null, URI, TYPE, null);
        collector.processIn(null, null);
        collector.processIn(null, reqHeaders);
        collector.consumerEnd(null, URI, TYPE);

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
        collector.setAdminService(new AdminService() {
            @Override
            public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
                return new CollectorConfiguration();
            }
        });

        collector.consumerStart(null, null, null, "myid");

        collector.consumerEnd(null, null, null);

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
    public void testCorrelation() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        final FragmentBuilder fragmentBuilder = collector.getFragmentManager().getFragmentBuilder();

        collector.initiateCorrelation("TestLink");

        assertFalse(fragmentBuilder.getUncompletedCorrelationIds().isEmpty());

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                collector.completeCorrelation("TestLink");

                FragmentBuilder other = collector.getFragmentManager().getFragmentBuilder();

                assertEquals("Builders should be the same", fragmentBuilder, other);

                // Check link is no marked as unresolved
                assertTrue(other.getUncompletedCorrelationIds().isEmpty());
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
    public void testSetDetailsCurrentNode() {
        DefaultBusinessTransactionCollector collector = new DefaultBusinessTransactionCollector();

        // Cause a fragment builder to be created
        FragmentBuilder builder = collector.getFragmentManager().getFragmentBuilder();

        collector.consumerStart(null, "testconsumer", "testtype", "testid");

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

        collector.consumerStart(null, "testconsumer", "testtype", "testid");

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

        collector.consumerStart(null, "testconsumer", "testcontype", "testconid");

        Consumer consumer = (Consumer) builder.getCurrentNode();

        assertNotNull(consumer);

        collector.componentStart(null, "testcomponent1", "testcomptype", "testcompop");

        Component component1 = (Component) builder.getCurrentNode();

        assertNotNull(component1);

        collector.componentStart(null, "testcomponent2", "testcomptype", "testcompop");

        Component component2 = (Component) builder.getCurrentNode();

        assertNotNull(component2);

        collector.producerStart(null, "testproducer", "testprodtype", "tesprodid");

        Producer producer = (Producer) builder.getCurrentNode();

        assertNotNull(producer);

        // Pop the producer and one of the components
        collector.producerEnd(null, "testproducer", "testprodtype");
        collector.componentEnd(null, "testcomponent2", "testcomptype", "testcompop");

        collector.setDetail(null, "testname", "testvalue", "Producer", false);

        assertTrue(producer.getDetails().containsKey("testname"));
        assertFalse(consumer.getDetails().containsKey("testname"));
        assertFalse(component1.getDetails().containsKey("testname"));

        collector.getFragmentManager().clear();
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
         * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
         */
        @Override
        public BusinessTransaction get(String tenantId, String id) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
         *              org.hawkular.btm.api.services.BusinessTransactionCriteria)
         */
        @Override
        public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
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

    }
}
