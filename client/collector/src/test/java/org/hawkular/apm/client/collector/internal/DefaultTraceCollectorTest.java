/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.apm.client.collector.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ConfigurationService;
import org.hawkular.apm.api.services.PublisherMetricHandler;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.client.api.recorder.BatchTraceRecorder;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class DefaultTraceCollectorTest {

    private static final String TRACE_ID = "traceId";
    private static final String TXN_NAME = "BTxnName";
    private static final String TEST_TENANT = "TestTenant";
    private static final String TYPE = "TestType";
    private static final String URI = "TestURI";
    private static final String OP = "TestOP";

    @Test
    public void testSetStartTimeAndDuration() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                    .withTracePublisher(traceService)
                    .build());

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

        Wait.until(() -> traceService.getTraces().size() == 1);

        List<Trace> traces = traceService.getTraces();

        assertEquals("Only 1 trace expected", 1, traces.size());

        Trace trace = traces.get(0);

        assertEquals("Single node", 1, trace.getNodes().size());

        Node node = trace.getNodes().get(0);

        assertTrue("Start time not set", node.getTimestamp() > 0);
        assertTrue("Duration not set", node.getDuration() > 0);
    }

    @Test
    public void testTenantIdSystemProperty() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                    .withTracePublisher(traceService)
                    .withTenantId(TEST_TENANT)
                    .build());

        collector.setConfigurationService(new TestConfigurationService());

        collector.consumerStart(null, URI, TYPE, OP, null);

        collector.consumerEnd(null, URI, TYPE, OP);

        Wait.until(() -> traceService.getTraces().size() == 1);
        // Clear property
        System.getProperties().remove("HAWKULAR_APM_TENANTID");

        assertNotNull("TenantId should not be null", traceService.getTenantId());

        assertEquals("TenantId incorrect", TEST_TENANT, traceService.getTenantId());
    }

    @Test
    public void testIncludeHeaders() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                    .withTracePublisher(traceService)
                    .build());

        collector.setConfigurationService(new TestConfigurationService());

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        Map<String, String> respHeaders = new HashMap<String, String>();
        respHeaders.put("joe", "bloggs");

        collector.consumerStart(null, URI, TYPE, OP, null);
        collector.processIn(null, reqHeaders);
        collector.processOut(null, respHeaders);
        collector.consumerEnd(null, URI, TYPE, OP);

        Wait.until(() -> traceService.getTraces().size() == 1);
        List<Trace> traces = traceService.getTraces();

        assertEquals("Only 1 trace expected", 1, traces.size());

        Trace trace = traces.get(0);

        assertEquals("Single node", 1, trace.getNodes().size());

        Node node = trace.getNodes().get(0);

        Consumer service = (Consumer) node;

        assertEquals(service.getIn().getHeaders().get("hello"), "world");
        assertEquals(service.getOut().getHeaders().get("joe"), "bloggs");
    }

    @Test
    public void testIncludeHeadersNotProcessedAgain() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                    .withTracePublisher(traceService)
                    .build());

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

        Wait.until(() -> traceService.getTraces().size() == 1);

        List<Trace> traces = traceService.getTraces();

        assertEquals("Only 1 trace expected", 1, traces.size());

        Trace trace = traces.get(0);

        assertEquals("Single node", 1, trace.getNodes().size());

        Node node = trace.getNodes().get(0);

        Consumer service = (Consumer) node;

        assertEquals(service.getIn().getHeaders().get("hello"), "world");
        assertFalse(service.getIn().getHeaders().containsKey("joe"));
    }

    @Test
    public void testIncludeHeadersSuppliedSecondCall() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        collector.setConfigurationService(new TestConfigurationService());

        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("hello", "world");

        collector.consumerStart(null, URI, TYPE, OP, null);
        collector.processIn(null, null);
        collector.processIn(null, reqHeaders);
        collector.consumerEnd(null, URI, TYPE, OP);

        Wait.until(() -> traceService.getTraces().size() == 1);
        List<Trace> traces = traceService.getTraces();

        assertEquals("Only 1 trace expected", 1, traces.size());

        Trace trace = traces.get(0);

        assertEquals("Single node", 1, trace.getNodes().size());

        Node node = trace.getNodes().get(0);

        Consumer service = (Consumer) node;

        assertEquals(service.getIn().getHeaders().get("hello"), "world");
    }

    @Test
    public void testIncludeID() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        collector.setConfigurationService(new TestConfigurationService());

        collector.consumerStart(null, null, null, null, "myid");

        collector.consumerEnd(null, null, null, null);

        Wait.until(() -> traceService.getTraces().size() == 1);
        List<Trace> traces = traceService.getTraces();

        assertEquals("Only 1 trace expected", 1, traces.size());

        Trace trace = traces.get(0);

        assertEquals("Single node", 1, trace.getNodes().size());

        Node node = trace.getNodes().get(0);

        assertTrue("Should be 1 correlation id", node.getCorrelationIds().size() == 1);

        CorrelationIdentifier cid = node.getCorrelationIds().iterator().next();
        assertEquals(CorrelationIdentifier.Scope.Interaction, cid.getScope());
        assertEquals("myid", cid.getValue());
    }

    @Test
    public void testReportingLevelNoneByFilter() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setLevel(ReportingLevel.None);
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", null);

        collector.consumerStart(null, "/test", null, null, null);

        collector.consumerEnd(null, null, null, null);

        List<Trace> traces = traceService.getTraces();

        assertEquals(0, traces.size());
    }

    @Test
    public void testReportingWithOpLevelNoneByFilter() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setLevel(ReportingLevel.None);
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", "op");

        collector.consumerStart(null, "/test", null, "op", null);

        collector.consumerEnd(null, null, null, null);

        List<Trace> traces = traceService.getTraces();

        assertEquals(0, traces.size());
    }

    @Test
    public void testReportingLevelNoneBySetter() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", null);

        collector.setLevel(null, "None");

        collector.consumerStart(null, "/test", null, null, null);

        collector.consumerEnd(null, null, null, null);

        List<Trace> traces = traceService.getTraces();

        assertEquals(0, traces.size());
    }

    @Test
    public void testReportingWithOpLevelNoneBySetter() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", "op");

        collector.setLevel(null, "None");

        collector.consumerStart(null, "/test", null, "op", null);

        collector.consumerEnd(null, null, null, null);

        List<Trace> traces = traceService.getTraces();

        assertEquals(0, traces.size());
    }

    @Test
    public void testReportingLevelAll() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", null);

        collector.consumerStart(null, "/test", null, null, null);

        collector.consumerEnd(null, null, null, null);

        Wait.until(() -> traceService.getTraces().size() == 1);
        List<Trace> traces = traceService.getTraces();

        assertEquals(1, traces.size());
    }

    @Test
    public void testReportingWithOpLevelAll() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService tcs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        tcs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(tcs);

        collector.activate("/test", "op");

        collector.consumerStart(null, "/test", null, "op", null);

        collector.consumerEnd(null, null, null, null);

        Wait.until(() -> traceService.getTraces().size() == 1);
        List<Trace> traces = traceService.getTraces();

        assertEquals(1, traces.size());
    }

    @Test
    public void testCorrelation() throws InterruptedException, ExecutionException {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        final FragmentBuilder fragmentBuilder = collector.getFragmentManager().getFragmentBuilder();

        collector.initiateCorrelation("TestLink");

        assertFalse(fragmentBuilder.getUncompletedCorrelationIds().isEmpty());

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                collector.completeCorrelation("TestLink", false);

                FragmentBuilder other = collector.getFragmentManager().getFragmentBuilder();

                assertEquals("Builders should be the same", fragmentBuilder, other);

                // Check link is no marked as unresolved
                assertTrue(other.getUncompletedCorrelationIds().isEmpty());
            }
        }).get();
    }

    @Test
    public void testIsActive() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.getFragmentManager().getFragmentBuilder();

        assertTrue(collector.isActive());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testDeactivate() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.getFragmentManager().getFragmentBuilder();

        assertTrue(collector.isActive());

        collector.deactivate();

        assertFalse(collector.isActive());
    }

    @Test
    public void testActivateUnknownURI() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.activate("/test", null);

        assertFalse(collector.isActive());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testActivateWithOpUnknownURI() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.activate("/test", "op");

        assertFalse(collector.isActive());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetNameNoFragmentManager() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        collector.setTransaction(null, "test");

        assertEquals("test", collector.getTransaction());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetNameWithFragmentManager() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        collector.setTransaction(null, "test");

        assertEquals("test", collector.getTransaction());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnInitialNode() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        assertTrue(collector.isActive());
        assertEquals("testapp", collector.getTransaction());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnSubsequentNodeInitialFragment() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Create top level node
        collector.activate("not relevant", null);
        collector.componentStart(null, "not relevant", Constants.COMPONENT_DATABASE, "query");

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        assertTrue(collector.isActive());
        assertEquals("testapp", collector.getTransaction());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnSubsequentNodeInitialFragmentWithOp() {
        TestTraceService traceService = new TestTraceService();
        DefaultTraceCollector collector = new DefaultTraceCollector(
                new BatchTraceRecorder.BatchTraceRecorderBuilder()
                        .withTracePublisher(traceService)
                        .build());

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test\\[op\\]");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Create top level node
        collector.activate("not relevant", null);
        collector.componentStart(null, "not relevant", Constants.COMPONENT_DATABASE, "query");

        // Cause a fragment builder to be created
        collector.activate("/test", "op");
        collector.producerStart(null, "/test", "HTTP", "op", null);

        assertTrue(collector.isActive());
        assertEquals("testapp", collector.getTransaction());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testNamedOnSubsequentNodeLaterFragment() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        TestConfigurationService cs = new TestConfigurationService();

        CollectorConfiguration cc = new CollectorConfiguration();
        cs.setCollectorConfiguration(cc);

        TransactionConfig btc = new TransactionConfig();
        btc.setFilter(new Filter());
        btc.getFilter().getInclusions().add("/test");
        cc.getTransactions().put("testapp", btc);

        collector.setConfigurationService(cs);

        assertFalse(collector.isActive());

        // Create top level node
        collector.activate("not relevant", null);
        collector.consumerStart(null, "not relevant", "HTTP", null, null);
        collector.getFragmentManager().getFragmentBuilder()
            .getTrace().getNodes().get(0).addInteractionCorrelationId("testId");

        // Cause a fragment builder to be created
        collector.activate("/test", null);
        collector.producerStart(null, "/test", "HTTP", null, null);

        assertTrue(collector.isActive());
        assertEquals("", collector.getTransaction());

        collector.getFragmentManager().clear();
    }

    @Test
    public void testSetPropertyCurrentNode() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        // Cause a fragment builder to be created
        FragmentBuilder builder = collector.getFragmentManager().getFragmentBuilder();

        collector.consumerStart(null, "testconsumer", "testtype", "testop", "testid");

        Node node = builder.getCurrentNode();

        assertNotNull(node);

        collector.setProperty(null, "testname", "testvalue");

        assertTrue(node.hasProperty("testname"));

        collector.getFragmentManager().clear();
    }

    @Test
    public void testMergeDuplicateProducers() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

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
        DefaultTraceCollector collector = new DefaultTraceCollector();

        FragmentBuilder parent = new FragmentBuilder();
        FragmentBuilder spawned = new FragmentBuilder();

        Trace parentTrace = parent.getTrace();
        Trace spawnedTrace = spawned.getTrace();

        parentTrace.setTransaction(TXN_NAME);

        // Create top level consumer in parent
        Consumer parentConsumer = new Consumer();
        parentConsumer.setUri(URI);
        parentConsumer.setOperation(OP);

        collector.push(null, parent, parentConsumer);

        collector.spawnFragment(parent, parentConsumer, -1, spawned);

        // Check that parent consumer has Producer as child
        assertEquals(1, parentConsumer.getNodes().size());
        assertTrue(parentConsumer.getNodes().get(0) instanceof Producer);

        Producer internalProducer=(Producer)parentConsumer.getNodes().get(0);
        assertEquals(URI, internalProducer.getUri());
        assertEquals(OP, internalProducer.getOperation());

        // Check that spawned fragment has Consumer as top level node
        assertEquals(1, spawnedTrace.getNodes().size());
        assertTrue(spawnedTrace.getNodes().get(0) instanceof Consumer);

        Consumer internalConsumer=(Consumer)spawnedTrace.getNodes().get(0);
        assertEquals(URI, internalConsumer.getUri());
        assertEquals(OP, internalConsumer.getOperation());

        // Check that internal producer and consumer share common interaction id
        List<CorrelationIdentifier> ipids = internalProducer.findCorrelationIds(Scope.ControlFlow);
        List<CorrelationIdentifier> icids = internalConsumer.findCorrelationIds(Scope.ControlFlow);

        assertEquals(1, ipids.size());
        assertEquals(1, icids.size());
        assertEquals(ipids.get(0), icids.get(0));

        // Check trace details transferred
        assertEquals(TXN_NAME, spawnedTrace.getTransaction());
    }

    @Test
    public void testSpawnFragmentSameTraceId() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        FragmentBuilder parent = new FragmentBuilder();
        FragmentBuilder spawned = new FragmentBuilder();

        Trace parentTrace = parent.getTrace();
        Trace spawnedTrace = spawned.getTrace();

        parentTrace.setTraceId(TRACE_ID);

        // Create top level consumer in parent
        Consumer parentConsumer = new Consumer();

        collector.push(null, parent, parentConsumer);

        collector.spawnFragment(parent, parentConsumer, -1, spawned);

        // Check trace details transferred
        assertEquals(TRACE_ID, spawnedTrace.getTraceId());
    }

    @Test
    public void testSpawnFragmentUsingInsertChild() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        FragmentBuilder parent = new FragmentBuilder();
        FragmentBuilder spawned = new FragmentBuilder();

        Trace parentTrace = parent.getTrace();
        Trace spawnedTrace = spawned.getTrace();

        parentTrace.setTransaction(TXN_NAME);

        // Create top level consumer in parent
        Consumer parentConsumer = new Consumer();
        parentConsumer.setUri(URI);
        parentConsumer.setOperation(OP);

        // Add existing children
        Component comp1 = new Component();
        parentConsumer.getNodes().add(comp1);

        Component comp2 = new Component();
        parentConsumer.getNodes().add(comp2);

        collector.push(null, parent, parentConsumer);

        collector.spawnFragment(parent, parentConsumer, 1, spawned);

        // Check that parent consumer has Producer as child
        assertEquals(3, parentConsumer.getNodes().size());
        assertTrue(parentConsumer.getNodes().get(1) instanceof Producer);

        Producer internalProducer=(Producer)parentConsumer.getNodes().get(1);
        assertEquals(URI, internalProducer.getUri());
        assertEquals(OP, internalProducer.getOperation());

        // Check that spawned fragment has Consumer as top level node
        assertEquals(1, spawnedTrace.getNodes().size());
        assertTrue(spawnedTrace.getNodes().get(0) instanceof Consumer);

        Consumer internalConsumer=(Consumer)spawnedTrace.getNodes().get(0);
        assertEquals(URI, internalConsumer.getUri());
        assertEquals(OP, internalConsumer.getOperation());

        // Check that internal producer and consumer share common interaction id
        List<CorrelationIdentifier> ipids = internalProducer.findCorrelationIds(Scope.ControlFlow);
        List<CorrelationIdentifier> icids = internalConsumer.findCorrelationIds(Scope.ControlFlow);

        assertEquals(1, ipids.size());
        assertEquals(1, icids.size());
        assertEquals(ipids.get(0), icids.get(0));

        // Check trace details transferred
        assertEquals(TXN_NAME, spawnedTrace.getTransaction());
    }

    @Test
    public void testSpawnFragmentUsingPush() {
        DefaultTraceCollector collector = new DefaultTraceCollector();

        FragmentBuilder parent = new FragmentBuilder();
        FragmentBuilder spawned = new FragmentBuilder();

        Trace parentTrace = parent.getTrace();
        Trace spawnedTrace = spawned.getTrace();

        parentTrace.setTransaction(TXN_NAME);

        // Create top level consumer in parent
        Consumer parentConsumer = new Consumer();
        parentConsumer.setUri(URI);
        parentConsumer.setOperation(OP);

        collector.push(null, parent, parentConsumer);

        collector.spawnFragment(parent, null, -1, spawned);

        // Check that parent consumer has Producer as child
        assertEquals(1, parentConsumer.getNodes().size());
        assertTrue(parentConsumer.getNodes().get(0) instanceof Producer);

        Producer internalProducer=(Producer)parentConsumer.getNodes().get(0);
        assertEquals(URI, internalProducer.getUri());
        assertEquals(OP, internalProducer.getOperation());

        // Check that spawned fragment has Consumer as top level node
        assertEquals(1, spawnedTrace.getNodes().size());
        assertTrue(spawnedTrace.getNodes().get(0) instanceof Consumer);

        Consumer internalConsumer=(Consumer)spawnedTrace.getNodes().get(0);
        assertEquals(URI, internalConsumer.getUri());
        assertEquals(OP, internalConsumer.getOperation());

        // Check that internal producer and consumer share common interaction id
        List<CorrelationIdentifier> ipids = internalProducer.findCorrelationIds(Scope.ControlFlow);
        List<CorrelationIdentifier> icids = internalConsumer.findCorrelationIds(Scope.ControlFlow);

        assertEquals(1, ipids.size());
        assertEquals(1, icids.size());
        assertEquals(ipids.get(0), icids.get(0));

        // Check trace details transferred
        assertEquals(TXN_NAME, spawnedTrace.getTransaction());
    }

    public static class TestTraceService implements TracePublisher {

        private List<Trace> traces = new ArrayList<>();
        private String tenantId;

        @Override
        public void publish(String tenantId, List<Trace> traces) throws Exception {
            this.tenantId = tenantId;
            this.traces.addAll(traces);
        }

        @Override
        public void publish(String tenantId, List<Trace> items, int retryCount, long delay)
                                    throws Exception {
            publish(tenantId, items);
        }

        @Override
        public void retry(String tenantId, List<Trace> items, String subscriber, int retryCount, long delay)
                throws Exception {
        }

        /**
         * @return the traces
         */
        public List<Trace> getTraces() {
            return traces;
        }

        /**
         * @param traces the traces to set
         */
        public void setTraces(List<Trace> traces) {
            this.traces = traces;
        }

        /**
         * @return the tenantId
         */
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public int getInitialRetryCount() {
            return 0;
        }

        @Override
        public void setMetricHandler(PublisherMetricHandler<Trace> handler) {
        }

    }

    public class TestConfigurationService implements ConfigurationService {

        private CollectorConfiguration config = new CollectorConfiguration();

        protected void setCollectorConfiguration(CollectorConfiguration cc) {
            config = cc;
        }

        @Override
        public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
            return config;
        }

        @Override
        public List<ConfigMessage> setTransaction(String tenantId, String name, TransactionConfig config) {
            return null;
        }

        @Override
        public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs)
                throws Exception {
            return null;
        }

        @Override
        public List<ConfigMessage> validateTransaction(TransactionConfig config) {
            return null;
        }

        @Override
        public TransactionConfig getTransaction(String tenantId, String name) {
            return null;
        }

        @Override
        public void removeTransaction(String tenantId, String name) {
        }

        @Override
        public List<TransactionSummary> getTransactionSummaries(String tenantId) {
            return null;
        }

        @Override
        public Map<String, TransactionConfig> getTransactions(String tenantId, long updated) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void clear(String tenantId) {
            // TODO Auto-generated method stub

        }
    }
}
