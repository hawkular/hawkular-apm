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
package org.hawkular.apm.server.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.common.collect.Sets;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.PropertyType;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.EndpointInfo;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.PropertyInfo;
import org.hawkular.apm.api.model.analytics.TimeseriesStatistics;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ConfigurationService;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AnalyticsServiceElasticsearchTest {

    private static final String TXN = "testapp";

    private static final String OP2_1 = "op2.1";
    private static final String OUT2_1 = "out2.1";
    private static final String OP2 = "op2";
    private static final String IN2 = "in2";
    private static final String OP1_2 = "op1.2";
    private static final String OUT1_2 = "out1.2";
    private static final String OP1_1 = "op1.1";
    private static final String OUT1_1 = "out1.1";
    private static final String OP1 = "op1";
    private static final String IN1 = "in1";

    private static final String EP_INOP1 = EndpointUtil.encodeEndpoint(IN1,OP1);
    private static final String EP_INOP2 = EndpointUtil.encodeEndpoint(IN2,OP2);
    private static final String EP_OUTOP1_1 = EndpointUtil.encodeEndpoint(OUT1_1,OP1_1);
    private static final String EP_OUTOP1_2 = EndpointUtil.encodeEndpoint(OUT1_2,OP1_2);
    private static final String EP_OUTOP2_1 = EndpointUtil.encodeEndpoint(OUT2_1,OP2_1);

    private static final String EP_IN1_OP = EndpointUtil.encodeEndpoint(null, IN1);
    private static final String EP_IN2_OP = EndpointUtil.encodeEndpoint(null, IN2);
    private static final String EP_OUT1_1_OP = EndpointUtil.encodeEndpoint(null, OUT1_1);
    private static final String EP_OUT1_2_OP = EndpointUtil.encodeEndpoint(null, OUT1_2);
    private static final String EP_OUT2_1_OP = EndpointUtil.encodeEndpoint(null, OUT2_1);

    private TraceServiceElasticsearch bts;

    private AnalyticsServiceElasticsearch analytics;

    @BeforeClass
    public static void initClass() {
        System.setProperty("HAWKULAR_APM_CONFIG_DIR", "target");
    }

    @Before
    public void beforeTest() {
        analytics = new AnalyticsServiceElasticsearch();
        bts = new TraceServiceElasticsearch();
    }

    @After
    public void afterTest() {
        bts.clear(null);
        analytics.clear(null);
    }

    @Test
    public void testAllDistinctUnboundEndpointsConsumer() throws StoreException {
        List<Trace> traces = new ArrayList<>();

        Trace trace1 = new Trace().setTraceId("1").setFragmentId("1");
        trace1.setTimestamp(1000);
        traces.add(trace1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        trace1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri4");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace().setTraceId("2").setFragmentId("2");
        trace2.setTimestamp(2000);
        traces.add(trace2);

        Consumer c2 = new Consumer();
        c2.setUri("uri5");

        trace2.getNodes().add(c2);

        bts.storeFragments(null, traces);

        Wait.until(() -> analytics.getUnboundEndpoints(null, 1, 0, false).size() == 2);
        java.util.List<EndpointInfo> uris = analytics.getUnboundEndpoints(null, 1, 0, false);

        assertNotNull(uris);
        assertEquals(2, uris.size());

        assertEquals("uri1", uris.get(0).getEndpoint());
        assertEquals("uri5", uris.get(1).getEndpoint());
    }

    @Test
    public void testAllDistinctUnboundEndpointsProducer() throws StoreException {
        Trace trace1 = new Trace().setTraceId("1").setFragmentId("1");
        trace1.setTimestamp(1000);

        Component c1 = new Component();
        c1.setUri("uri1");
        trace1.getNodes().add(c1);

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

        bts.storeFragments(null, Collections.singletonList(trace1));

        Wait.until(() -> analytics.getUnboundEndpoints(null, 1, 0, false).size() == 2);
        java.util.List<EndpointInfo> uris = analytics.getUnboundEndpoints(null, 1, 0, false);

        assertNotNull(uris);
        assertEquals(2, uris.size());

        assertEquals("uri4", uris.get(0).getEndpoint());
        assertEquals("uri5", uris.get(1).getEndpoint());
    }

    @Test
    public void testAllDuplicationUnboundEndpoints() throws StoreException {
        Trace trace1 = new Trace().setTraceId("1").setFragmentId("1");
        trace1.setTimestamp(1000);

        Component c1 = new Component();
        c1.setUri("uri1");
        trace1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri3");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace().setTraceId("1").setFragmentId("2");
        trace2.setTimestamp(2000);

        Consumer c2 = new Consumer();
        c2.setUri("uri3");

        trace2.getNodes().add(c2);

        bts.storeFragments(null, Arrays.asList(trace1, trace2));

        Wait.until(() -> analytics.getUnboundEndpoints(null, 1, 0, false).size() == 1);
        java.util.List<EndpointInfo> uris = analytics.getUnboundEndpoints(null, 1, 0, false);

        assertNotNull(uris);
        assertEquals(1, uris.size());

        assertEquals("uri3", uris.get(0).getEndpoint());
    }

    @Test
    public void testUnboundEndpointsExcludeBTxnConfig() throws StoreException {
        Trace trace1 = new Trace().setTraceId("1").setFragmentId("1");
        trace1.setTimestamp(1000);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        trace1.getNodes().add(c1);

        bts.storeFragments(null, Collections.singletonList(trace1));

        analytics.setConfigurationService(new ConfigurationService() {
            @Override
            public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
                return null;
            }

            @Override
            public List<ConfigMessage> setTransaction(String tenantId, String name, TransactionConfig config)
                    throws Exception {
                return null;
            }

            @Override
            public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs)
                    throws Exception {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public TransactionConfig getTransaction(String tenantId, String name) {
                return null;
            }

            @Override
            public Map<String, TransactionConfig> getTransactions(String tenantId, long updated) {
                Map<String, TransactionConfig> ret = new HashMap<>();
                TransactionConfig btc = new TransactionConfig();
                btc.setFilter(new Filter());
                btc.getFilter().getInclusions().add("uri1");
                ret.put("btc1", btc);
                return ret;
            }

            @Override
            public List<TransactionSummary> getTransactionSummaries(String tenantId) {
                return null;
            }

            @Override
            public void removeTransaction(String tenantId, String name) throws Exception {
            }

            @Override
            public List<ConfigMessage> validateTransaction(TransactionConfig config) {
                return null;
            }

            @Override
            public void clear(String tenantId) {
                // TODO Auto-generated method stub

            }
        });

        java.util.List<EndpointInfo> uris = analytics.getUnboundEndpoints(null, 1, 0, false);

        assertNotNull(uris);
        assertEquals(0, uris.size());
    }

    @Test
    public void testUnboundEndpointsExcludeBTxnConfigRegex() throws StoreException {
        Trace trace1 = new Trace().setTraceId("1").setFragmentId("1");
        trace1.setTimestamp(1000);

        Consumer c1 = new Consumer();
        c1.setUri("{myns}ns");
        trace1.getNodes().add(c1);

        bts.storeFragments(null, Collections.singletonList(trace1));

        analytics.setConfigurationService(new ConfigurationService() {
            @Override
            public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
                return null;
            }

            @Override
            public List<ConfigMessage> setTransaction(String tenantId, String name, TransactionConfig config)
                    throws Exception {
                return null;
            }

            @Override
            public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs)
                    throws Exception {
                return null;
            }

            @Override
            public TransactionConfig getTransaction(String tenantId, String name) {
                return null;
            }

            @Override
            public Map<String, TransactionConfig> getTransactions(String tenantId, long updated) {
                Map<String, TransactionConfig> ret = new HashMap<>();
                TransactionConfig btc = new TransactionConfig();
                btc.setFilter(new Filter());
                btc.getFilter().getInclusions().add("^\\{myns\\}ns$");
                ret.put("btc1", btc);
                return ret;
            }

            @Override
            public List<TransactionSummary> getTransactionSummaries(String tenantId) {
                return null;
            }

            @Override
            public void removeTransaction(String tenantId, String name) throws Exception {
            }

            @Override
            public List<ConfigMessage> validateTransaction(TransactionConfig config) {
                return null;
            }

            @Override
            public void clear(String tenantId) {
                // TODO Auto-generated method stub

            }
        });

        java.util.List<EndpointInfo> uris = analytics.getUnboundEndpoints(null, 1, 0, false);

        assertNotNull(uris);
        assertEquals(0, uris.size());
    }

    @Test
    public void testBoundEndpoints() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setTransaction("trace1");
        trace1.setTimestamp(1000);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        trace1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri2");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setTransaction("trace2");
        trace2.setTimestamp(2000);

        Consumer c2 = new Consumer();
        c2.setUri("uri4");

        trace2.getNodes().add(c2);

        bts.storeFragments(null, Arrays.asList(trace1, trace2));

        Wait.until(() -> analytics.getBoundEndpoints(null, "trace1", 1, 0).size() == 3);
        java.util.List<EndpointInfo> uris1 = analytics.getBoundEndpoints(null, "trace1", 1, 0);

        assertNotNull(uris1);
        assertEquals(3, uris1.size());
        assertTrue(uris1.contains(new EndpointInfo("uri1")));
        assertTrue(uris1.contains(new EndpointInfo("uri2")));
        assertTrue(uris1.contains(new EndpointInfo("uri3")));

        Wait.until(() -> analytics.getBoundEndpoints(null, "trace2", 1, 0).size() == 1);
        java.util.List<EndpointInfo> uris2 = analytics.getBoundEndpoints(null, "trace2", 1, 0);

        assertNotNull(uris2);
        assertEquals(1, uris2.size());
        assertTrue(uris2.contains(new EndpointInfo("uri4")));
    }

    @Test
    public void testPropertyInfo() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property("prop1", "value1"));
        ct1.getProperties().add(new Property("prop2", "value2"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction("trace1");
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property("prop3", "value3"));
        ct2.getProperties().add(new Property("prop2", "value2"));

        analytics.storeTraceCompletions(null,  Arrays.asList(ct1, ct2));

        Criteria criteria = new Criteria()
            .setStartTime(1)
            .setEndTime(0);

        Wait.until(() -> analytics.getPropertyInfo(null, criteria).size() == 3);
        java.util.List<PropertyInfo> pis = analytics.getPropertyInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(3, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
        assertTrue(pis.get(1).getName().equals("prop2"));
        assertTrue(pis.get(2).getName().equals("prop3"));
    }

    @Test
    public void testPropertyInfoWithTxn() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction("trace1");
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1.getProperties().add(new Property("prop1", "value1"));
        ct1.getProperties().add(new Property("prop2", "value2"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction("trace1");
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2.getProperties().add(new Property("prop3", "value3"));
        ct2.getProperties().add(new Property("prop2", "value2"));

        analytics.storeTraceCompletions(null,  Arrays.asList(ct1, ct2));

        Criteria criteria = new Criteria()
            .setTransaction("trace1")
            .setStartTime(1)
            .setEndTime(0);

        Wait.until(() -> analytics.getPropertyInfo(null, criteria).size() == 4);
        java.util.List<PropertyInfo> pis = analytics.getPropertyInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(4, pis.size());
        assertEquals(Sets.newHashSet("prop1", "prop2", "prop3", Constants.PROP_PRINCIPAL),
                pis.stream().map(pi -> pi.getName()).collect(Collectors.toSet()));

        Criteria criteria2 =new Criteria()
            .setTransaction("trace1")
            .addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS)
            .setStartTime(1)
            .setEndTime(0);

        Wait.until(() -> analytics.getPropertyInfo(null, criteria2).size() == 3);
        pis = analytics.getPropertyInfo(null, criteria2);

        assertNotNull(pis);
        assertEquals(3, pis.size());
        assertEquals(Sets.newHashSet("prop1", "prop2", Constants.PROP_PRINCIPAL),
                pis.stream().map(pi -> pi.getName()).collect(Collectors.toSet()));
    }

    @Test
    public void testGetCompletionTimes() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1.setUri("uri1");

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault1"));
        ct2.setUri("uri2");

        CompletionTime ct3 = new CompletionTime();
        ct3.setTransaction(TXN);
        ct3.setTimestamp(2000);
        ct3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct3.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault2"));
        ct3.setUri("uri1");

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2, ct3));

        Criteria criteria = new Criteria().setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletions(null, criteria).size() == 3);
        List<CompletionTime> results = analytics.getTraceCompletions(null, criteria);

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    public void testGetCompletionTimesForUri() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1.setUri("uri1");

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault1"));
        ct2.setUri("uri2");

        CompletionTime ct3 = new CompletionTime();
        ct3.setTransaction(TXN);
        ct3.setTimestamp(2000);
        ct3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct3.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault2"));
        ct3.setUri("uri1");

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2, ct3));

        Criteria criteria = new Criteria().setUri("uri1").setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletions(null, criteria).size() == 2);
        List<CompletionTime> results = analytics.getTraceCompletions(null, criteria);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testGetCompletionTimesForOperation() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);
        ct1.setOperation(OP1);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault1"));
        ct2.setOperation(OP2);

        CompletionTime ct3 = new CompletionTime();
        ct3.setTransaction(TXN);
        ct3.setTimestamp(2000);
        ct3.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault2"));
        ct3.setOperation(OP1);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2, ct3));

        Criteria criteria = new Criteria().setOperation(OP1).setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletions(null, criteria).size() == 2);
        List<CompletionTime> results = analytics.getTraceCompletions(null, criteria);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testGetCompletionCount() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletionCount(null, criteria) == 2);
        assertEquals(2, analytics.getTraceCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCount2() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(1);

        Wait.until(() -> analytics.getTraceCompletionCount(null, criteria) == 1);
        assertEquals(1, analytics.getTraceCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCountForFault() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2));

        Criteria criteria = new Criteria();
        criteria.addProperty(Constants.PROP_FAULT, "TestFault", Criteria.Operator.HAS);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletionCount(null, criteria) == 1);
        assertEquals(1, analytics.getTraceCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCountForNotFault() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault1"));

        CompletionTime ct3 = new CompletionTime();
        ct3.setTransaction(TXN);
        ct3.setTimestamp(2000);
        ct3.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2, ct3));

        Criteria criteria = new Criteria();
        criteria.addProperty(Constants.PROP_FAULT, "TestFault1", Operator.HASNOT);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletionCount(null, criteria) == 2);
        assertEquals(2, analytics.getTraceCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCountForPrincipal() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault1"));

        CompletionTime ct3 = new CompletionTime();
        ct3.setTransaction(TXN);
        ct3.setTimestamp(2000);
        ct3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct3.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2, ct3));

        Criteria criteria = new Criteria();
        criteria.addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletionCount(null, criteria) == 2);
        assertEquals(2, analytics.getTraceCompletionCount(null, criteria));
    }

    @Test
    public void testGetCompletionCountForPropertyNum() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property("num", "1.5", PropertyType.Number));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTimestamp(2000);
        ct2.getProperties().add(new Property("num", "2.0", PropertyType.Number));

        CompletionTime ct3 = new CompletionTime();
        ct3.setTimestamp(2000);
        ct3.getProperties().add(new Property("num", "3.7", PropertyType.Number));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2, ct3));

        Criteria criteria = new Criteria();
        criteria.addProperty("num", "2", Operator.GTE);
        criteria.setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletionCount(null, criteria) == 2);
        assertEquals(2, analytics.getTraceCompletionCount(null, criteria));

        Criteria criteria2 = new Criteria();
        criteria2.addProperty("num", "2", Operator.GT);
        criteria2.setStartTime(1).setEndTime(0);

        assertEquals(1, analytics.getTraceCompletionCount(null, criteria2));

        Criteria criteria3 = new Criteria();
        criteria3.addProperty("num", "2", Operator.LTE);
        criteria3.setStartTime(1).setEndTime(0);

        assertEquals(2, analytics.getTraceCompletionCount(null, criteria3));

        Criteria criteria4 = new Criteria();
        criteria4.addProperty("num", "2", Operator.LT);
        criteria4.setStartTime(1).setEndTime(0);

        assertEquals(1, analytics.getTraceCompletionCount(null, criteria4));

        Criteria criteria5 = new Criteria();
        criteria5.addProperty("num", "2.0", Operator.EQ);
        criteria5.setStartTime(1).setEndTime(0);

        assertEquals(1, analytics.getTraceCompletionCount(null, criteria5));

        Criteria criteria6 = new Criteria();
        criteria6.addProperty("num", "2.0", Operator.NE);
        criteria6.setStartTime(1).setEndTime(0);

        assertEquals(2, analytics.getTraceCompletionCount(null, criteria6));
    }

    @Test
    public void testGetCompletionFaultCount() throws StoreException {
        CompletionTime ct1 = new CompletionTime();
        ct1.setTransaction(TXN);
        ct1.setTimestamp(1000);
        ct1.getProperties().add(new Property(Constants.PROP_FAULT, "Failed"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2000);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(0);

        Wait.until(() -> analytics.getTraceCompletionFaultCount(null, criteria) == 1);
        assertEquals(1, analytics.getTraceCompletionFaultCount(null, criteria));
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000).size() == 2
        );
        List<TimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria,
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
    public void testGetCompletionTimeseriesStatisticsWithLowerBound() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setLowerBound(200);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000).size() == 2
        );
        List<TimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria,
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
    public void testGetCompletionTimeseriesStatisticsWithUpperBound() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(500);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(300);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setUpperBound(400);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000).size() == 2
        );
        List<TimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria,
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
    public void testGetCompletionTimeseriesStatisticsWithFaults() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_FAULT, "fault1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "fault2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000).size() == 2
        );
        List<TimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria,
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
    public void testGetCompletionTimeseriesStatisticsForPrincipal() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1_2.getProperties().add(new Property(Constants.PROP_FAULT, "fault1"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(1700);
        ct2.setDuration(500);
        ct2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS)
                        .setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000).size() == 1
        );
        List<TimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());

        assertEquals(2, stats.get(0).getCount());

        assertTrue(stats.get(0).getMin() == 100);
        assertTrue(stats.get(0).getAverage() == 200);
        assertTrue(stats.get(0).getMax() == 300);

        assertEquals(1, stats.get(0).getFaultCount());
    }

    @Test
    public void testGetCompletionPropertyDetails() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property("prop1", "value1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property("prop1", "value2"));
        ct1_2.getProperties().add(new Property("prop2", "value3"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        ct2.getProperties().add(new Property("prop1", "value2"));
        ct2.getProperties().add(new Property("prop2", "value4"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionPropertyDetails(null, criteria, "prop1").size() == 2
        );
        List<Cardinality> cards1 = analytics.getTraceCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(2, cards1.size());

        assertEquals("value1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
        assertEquals("value2", cards1.get(1).getValue());
        assertEquals(2, cards1.get(1).getCount());

        List<Cardinality> cards2 = analytics.getTraceCompletionPropertyDetails(null, criteria,
                "prop2");

        assertNotNull(cards2);
        assertEquals(2, cards2.size());

        assertEquals("value3", cards2.get(0).getValue());
        assertEquals(1, cards2.get(0).getCount());
        assertEquals("value4", cards2.get(1).getValue());
        assertEquals(1, cards2.get(1).getCount());
    }

    @Test
    public void testGetCompletionPropertyDetailsForFault() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property("prop1", "value1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault"));
        ct1_2.getProperties().add(new Property("prop1", "value2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2));

        Criteria criteria = new Criteria();
        criteria.addProperty(Constants.PROP_FAULT, "TestFault", Criteria.Operator.HAS);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionPropertyDetails(null, criteria, "prop1").size() == 1
        );
        List<Cardinality> cards1 = analytics.getTraceCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("value2", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

    @Test
    public void testGetCompletionPropertyDetailsForExcludedFault() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property("prop1", "value1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_FAULT, "TestFault"));
        ct1_2.getProperties().add(new Property("prop1", "value2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2));

        Criteria criteria = new Criteria();
        criteria.addProperty(Constants.PROP_FAULT, "TestFault", Operator.HASNOT);
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionPropertyDetails(null, criteria, "prop1").size() == 1
        );
        List<Cardinality> cards1 = analytics.getTraceCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("value1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

    @Test
    public void testGetCompletionPropertyDetailsForPrincipal() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1_1.getProperties().add(new Property("prop1", "value1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct1_2.getProperties().add(new Property("prop1", "value2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS)
                        .setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getTraceCompletionPropertyDetails(null, criteria, "prop1").size() == 1
        );
        List<Cardinality> cards1 = analytics.getTraceCompletionPropertyDetails(null, criteria,
                "prop1");

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("value1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());

    }

    @Test
    public void testGetCompletionFaultDetails() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_FAULT, "fault1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_FAULT, "fault2"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);
        ct2.getProperties().add(new Property(Constants.PROP_FAULT, "fault2"));

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getTraceCompletionFaultDetails(null, criteria).size() == 2 );
        List<Cardinality> cards1 = analytics.getTraceCompletionFaultDetails(null, criteria);

        assertNotNull(cards1);
        assertEquals(2, cards1.size());

        assertEquals("fault1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
        assertEquals("fault2", cards1.get(1).getValue());
        assertEquals(2, cards1.get(1).getCount());
    }

    @Test
    public void testGetCompletionFaultDetailsNotAllFaults() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_FAULT, "fault1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getTraceCompletionFaultDetails(null, criteria).size() == 1);
        List<Cardinality> cards1 = analytics.getTraceCompletionFaultDetails(null, criteria);

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("fault1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

    @Test
    public void testGetCompletionFaultDetailsForPrincipal() throws StoreException {
        CompletionTime ct1_1 = new CompletionTime();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setDuration(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_FAULT, "fault1"));
        ct1_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        CompletionTime ct1_2 = new CompletionTime();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setDuration(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_FAULT, "fault2"));
        ct1_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        CompletionTime ct2 = new CompletionTime();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setDuration(500);

        analytics.storeTraceCompletions(null, Arrays.asList(ct1_1, ct1_2, ct2));

        Criteria criteria = new Criteria();
        criteria.setTransaction(TXN).addProperty(Constants.PROP_PRINCIPAL, "p2", Operator.HAS)
                        .setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getTraceCompletionFaultDetails(null, criteria).size() == 1);
        List<Cardinality> cards1 = analytics.getTraceCompletionFaultDetails(null, criteria);

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("fault2", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

    @Test
    public void testGetNodeTimeseriesStatistics() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setComponentType(Constants.COMPONENT_DATABASE);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setComponentType(Constants.COMPONENT_DATABASE);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setComponentType(Constants.COMPONENT_EJB);

        NodeDetails ct2 = new NodeDetails();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setActual(500);
        ct2.setComponentType(Constants.COMPONENT_DATABASE);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2, ct1_3, ct2));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getNodeTimeseriesStatistics(null, criteria, 1000).size() == 2
        );
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(2000, stats.get(1).getTimestamp());

        assertEquals(2, stats.get(0).getComponentTypes().size());
        assertEquals(1, stats.get(1).getComponentTypes().size());

        assertTrue(stats.get(0).getComponentTypes().containsKey(Constants.COMPONENT_DATABASE));
        assertTrue(stats.get(0).getComponentTypes().containsKey(Constants.COMPONENT_EJB));
        assertTrue(stats.get(1).getComponentTypes().containsKey(Constants.COMPONENT_DATABASE));

        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_DATABASE).getDuration() == 200);
        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_DATABASE).getCount() == 2);
        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_EJB).getDuration() == 150);
        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_EJB).getCount() == 1);
        assertTrue(stats.get(1).getComponentTypes().get(Constants.COMPONENT_DATABASE).getDuration() == 500);
        assertTrue(stats.get(1).getComponentTypes().get(Constants.COMPONENT_DATABASE).getCount() == 1);
    }

    @Test
    public void testGetNodeTimeseriesStatisticsPrincipalFilter() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setComponentType(Constants.COMPONENT_EJB);
        ct1_3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        NodeDetails ct2 = new NodeDetails();
        ct2.setTransaction(TXN);
        ct2.setTimestamp(2100);
        ct2.setActual(500);
        ct2.setComponentType(Constants.COMPONENT_DATABASE);
        ct2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2, ct1_3, ct2));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);
        criteria.addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS);

        Wait.until(() -> analytics.getNodeTimeseriesStatistics(null, criteria, 1000).size() == 1);
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());

        assertEquals(2, stats.get(0).getComponentTypes().size());

        assertTrue(stats.get(0).getComponentTypes().containsKey(Constants.COMPONENT_DATABASE));
        assertTrue(stats.get(0).getComponentTypes().containsKey(Constants.COMPONENT_EJB));

        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_DATABASE).getDuration() == 200);
        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_DATABASE).getCount() == 2);
        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_EJB).getDuration() == 150);
        assertTrue(stats.get(0).getComponentTypes().get(Constants.COMPONENT_EJB).getCount() == 1);
    }

    @Test
    public void testGetNodeSummaryStatistics() throws StoreException {
        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setTransaction(TXN);
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_1.setUri("jdbc");

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_2.setUri("jdbc");

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType(Constants.COMPONENT_EJB);
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");

        NodeDetails ct1_4 = new NodeDetails();
        ct1_4.setTransaction(TXN);
        ct1_4.setTimestamp(1800);
        ct1_4.setActual(170);
        ct1_4.setElapsed(200);
        ct1_4.setType(NodeType.Component);
        ct1_4.setComponentType("OtherComponent");
        ct1_4.setOperation("onlyOp");

        analytics.storeNodeDetails(null, Arrays.asList(ct1_0, ct1_1, ct1_2, ct1_3, ct1_4));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getNodeSummaryStatistics(null, criteria).size() == 4);
        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(4, stats.size());

        Map<String,NodeSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getComponentType(), x -> x));

        assertEquals("jdbc", results.get(Constants.COMPONENT_DATABASE).getUri());
        assertNull(results.get(Constants.COMPONENT_DATABASE).getOperation());
        assertEquals(2, results.get(Constants.COMPONENT_DATABASE).getCount());
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getActual() == 200.0);
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getElapsed() == 400.0);

        assertEquals("BookingService", results.get(Constants.COMPONENT_EJB).getUri());
        assertEquals("createBooking", results.get(Constants.COMPONENT_EJB).getOperation());
        assertEquals(1, results.get(Constants.COMPONENT_EJB).getCount());
        assertTrue(results.get(Constants.COMPONENT_EJB).getActual() == 150.0);
        assertTrue(results.get(Constants.COMPONENT_EJB).getElapsed() == 300.0);

        assertEquals("hello", results.get("Consumer").getUri());
        assertNull(results.get("Consumer").getOperation());
        assertEquals(1, results.get("Consumer").getCount());
        assertTrue(results.get("Consumer").getActual() == 100.0);
        assertTrue(results.get("Consumer").getElapsed() == 200.0);

        assertEquals("onlyOp", results.get("OtherComponent").getOperation());
        assertEquals(1, results.get("OtherComponent").getCount());
        assertTrue(results.get("OtherComponent").getActual() == 170.0);
        assertTrue(results.get("OtherComponent").getElapsed() == 200.0);
    }

    @Test
    public void testGetNodeSummaryStatisticsWithBlankHostNameFilter() throws StoreException {
        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setTransaction(TXN);
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");
        ct1_0.setHostName("hostA");

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_1.setUri("jdbc");
        ct1_1.setHostName("hostA");

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_2.setUri("jdbc");
        ct1_2.setHostName("hostB");

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType(Constants.COMPONENT_EJB);
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");
        ct1_3.setHostName("hostB");

        analytics.storeNodeDetails(null, Arrays.asList(ct1_0, ct1_1, ct1_2, ct1_3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000).setHostName("");

        Wait.until(() -> analytics.getNodeSummaryStatistics(null, criteria).size() == 3);
        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(3, stats.size());

        Map<String,NodeSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getComponentType(), x -> x));

        assertTrue(results.get(Constants.COMPONENT_DATABASE).getUri().equalsIgnoreCase("jdbc"));
        assertNull(results.get(Constants.COMPONENT_DATABASE).getOperation());
        assertEquals(2, results.get(Constants.COMPONENT_DATABASE).getCount());
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getActual() == 200.0);
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getElapsed() == 400.0);

        assertTrue(results.get(Constants.COMPONENT_EJB).getUri().equalsIgnoreCase("BookingService"));
        assertTrue(results.get(Constants.COMPONENT_EJB).getOperation().equalsIgnoreCase("createBooking"));
        assertEquals(1, results.get(Constants.COMPONENT_EJB).getCount());
        assertTrue(results.get(Constants.COMPONENT_EJB).getActual() == 150.0);
        assertTrue(results.get(Constants.COMPONENT_EJB).getElapsed() == 300.0);

        assertTrue(results.get("Consumer").getUri().equalsIgnoreCase("hello"));
        assertNull(results.get("Consumer").getOperation());
        assertEquals(1, results.get("Consumer").getCount());
        assertTrue(results.get("Consumer").getActual() == 100.0);
        assertTrue(results.get("Consumer").getElapsed() == 200.0);
    }

    @Test
    public void testGetNodeSummaryStatisticsWithHostNameFilter() throws StoreException {
        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setTransaction(TXN);
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");
        ct1_0.setHostName("hostA");

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_1.setUri("jdbc");
        ct1_1.setHostName("hostA");

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_2.setUri("jdbc");
        ct1_2.setHostName("hostB");

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType(Constants.COMPONENT_EJB);
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");
        ct1_3.setHostName("hostB");

        analytics.storeNodeDetails(null, Arrays.asList(ct1_0, ct1_1, ct1_2, ct1_3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000).setHostName("hostA");

        Wait.until(() -> analytics.getNodeSummaryStatistics(null, criteria).size() == 2);
        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        Map<String,NodeSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getComponentType(), x -> x));

        assertTrue(results.get(Constants.COMPONENT_DATABASE).getUri().equalsIgnoreCase("jdbc"));
        assertNull(results.get(Constants.COMPONENT_DATABASE).getOperation());
        assertEquals(1, results.get(Constants.COMPONENT_DATABASE).getCount());
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getActual() == 100.0);
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getElapsed() == 200.0);

        assertTrue(results.get("Consumer").getUri().equalsIgnoreCase("hello"));
        assertNull(results.get("Consumer").getOperation());
        assertEquals(1, results.get("Consumer").getCount());
        assertTrue(results.get("Consumer").getActual() == 100.0);
        assertTrue(results.get("Consumer").getElapsed() == 200.0);
    }

    @Test
    public void testGetNodeSummaryStatisticsWithPrincipalFilter() throws StoreException {
        NodeDetails ct1_0 = new NodeDetails();
        ct1_0.setTransaction(TXN);
        ct1_0.setTimestamp(1500);
        ct1_0.setActual(100);
        ct1_0.setElapsed(200);
        ct1_0.setType(NodeType.Consumer);
        ct1_0.setUri("hello");
        ct1_0.setHostName("hostA");
        ct1_0.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setActual(100);
        ct1_1.setElapsed(200);
        ct1_1.setType(NodeType.Component);
        ct1_1.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_1.setUri("jdbc");
        ct1_1.setHostName("hostA");
        ct1_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setActual(300);
        ct1_2.setElapsed(600);
        ct1_2.setType(NodeType.Component);
        ct1_2.setComponentType(Constants.COMPONENT_DATABASE);
        ct1_2.setUri("jdbc");
        ct1_2.setHostName("hostB");
        ct1_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1700);
        ct1_3.setActual(150);
        ct1_3.setElapsed(300);
        ct1_3.setType(NodeType.Component);
        ct1_3.setComponentType(Constants.COMPONENT_EJB);
        ct1_3.setUri("BookingService");
        ct1_3.setOperation("createBooking");
        ct1_3.setHostName("hostB");
        ct1_3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        analytics.storeNodeDetails(null, Arrays.asList(ct1_0, ct1_1, ct1_2, ct1_3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000).addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS);

        Wait.until(() -> analytics.getNodeSummaryStatistics(null, criteria).size() == 2);
        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        Map<String,NodeSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getComponentType(), x -> x));

        assertTrue(results.get(Constants.COMPONENT_DATABASE).getUri().equalsIgnoreCase("jdbc"));
        assertNull(results.get(Constants.COMPONENT_DATABASE).getOperation());
        assertEquals(1, results.get(Constants.COMPONENT_DATABASE).getCount());
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getActual() == 100.0);
        assertTrue(results.get(Constants.COMPONENT_DATABASE).getElapsed() == 200.0);

        assertTrue(results.get("Consumer").getUri().equalsIgnoreCase("hello"));
        assertNull(results.get("Consumer").getOperation());
        assertEquals(1, results.get("Consumer").getCount());
        assertTrue(results.get("Consumer").getActual() == 100.0);
        assertTrue(results.get("Consumer").getElapsed() == 200.0);
    }

    @Test
    public void testGetCommunicationSummaryStatisticsWithoutOps() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setUri(IN1);
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setElapsed(100);
        ct1_1.setInitial(true);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setUri(OUT1_1);
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setElapsed(300);
        ct1_2.setInitial(true);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setUri(OUT1_2);
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1600);
        ct1_3.setElapsed(200);
        ct1_3.setInitial(true);

        NodeDetails ct2_1 = new NodeDetails();
        ct2_1.setUri(IN2);
        ct2_1.setTransaction(TXN);
        ct2_1.setTimestamp(1600);
        ct2_1.setElapsed(500);
        ct2_1.setInitial(true);

        NodeDetails ct2_2 = new NodeDetails();
        ct2_2.setUri(OUT2_1);
        ct2_2.setTransaction(TXN);
        ct2_2.setTimestamp(1700);
        ct2_2.setElapsed(400);
        ct2_2.setInitial(true);

        NodeDetails ct2_3 = new NodeDetails();
        ct2_3.setUri(IN2);
        ct2_3.setTransaction(TXN);
        ct2_3.setTimestamp(1700);
        ct2_3.setElapsed(600);
        ct2_3.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2, ct1_3, ct2_1, ct2_2, ct2_3));

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setLinkId("cd1");
        cd1.setTransaction(TXN);
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource(IN1);
        cd1.setTarget(OUT1_1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setLinkId("cd2");
        cd2.setTransaction(TXN);
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource(IN1);
        cd2.setTarget(OUT1_2);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setLinkId("cd3");
        cd3.setTransaction(TXN);
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource(IN2);
        cd3.setTarget(OUT2_1);

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setLinkId("cd4");
        cd4.setTransaction(TXN);
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource(IN1);
        cd4.setTarget(OUT1_2);

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setLinkId("cd5");
        cd5.setTransaction(TXN);
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource(IN2);
        cd5.setTarget(OUT2_1);

        analytics.storeCommunicationDetails(null, Arrays.asList(cd1, cd2, cd3, cd4, cd5));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getCommunicationSummaryStatistics(null, criteria, false).size() == 5);
        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                            criteria, false);

        assertNotNull(stats);
        assertEquals(5, stats.size());

        Map<String,CommunicationSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

        assertTrue(results.containsKey(IN1));
        assertTrue(results.containsKey(IN2));
        assertTrue(results.containsKey(OUT1_1));
        assertTrue(results.containsKey(OUT1_2));
        assertTrue(results.containsKey(OUT2_1));

        assertEquals(IN1, results.get(IN1).getUri());
        assertNull(results.get(IN1).getOperation());

        assertEquals(1, results.get(IN1).getCount());
        assertEquals(2, results.get(IN2).getCount());
        assertEquals(1, results.get(OUT1_1).getCount());
        assertEquals(1, results.get(OUT1_2).getCount());
        assertEquals(1, results.get(OUT2_1).getCount());

        // Only check in2 node, as others only have single completion time
        assertTrue(500 == results.get(IN2).getMinimumDuration());
        assertTrue(550 == results.get(IN2).getAverageDuration());
        assertTrue(600 == results.get(IN2).getMaximumDuration());

        assertEquals(2, results.get(IN1).getOutbound().size());
        assertEquals(1, results.get(IN2).getOutbound().size());
        assertEquals(0, results.get(OUT1_1).getOutbound().size());
        assertEquals(0, results.get(OUT1_2).getOutbound().size());
        assertEquals(0, results.get(OUT2_1).getOutbound().size());

        assertTrue(results.get(IN1).getOutbound().containsKey(OUT1_1));
        assertTrue(results.get(IN1).getOutbound().containsKey(OUT1_2));
        assertTrue(results.get(IN2).getOutbound().containsKey(OUT2_1));

        assertTrue(100 == results.get(IN1).getOutbound().get(OUT1_1).getMinimumLatency());
        assertTrue(100 == results.get(IN1).getOutbound().get(OUT1_1).getAverageLatency());
        assertTrue(100 == results.get(IN1).getOutbound().get(OUT1_1).getMaximumLatency());
        assertTrue(200 == results.get(IN1).getOutbound().get(OUT1_2).getMinimumLatency());
        assertTrue(250 == results.get(IN1).getOutbound().get(OUT1_2).getAverageLatency());
        assertTrue(300 == results.get(IN1).getOutbound().get(OUT1_2).getMaximumLatency());
        assertTrue(300 == results.get(IN2).getOutbound().get(OUT2_1).getMinimumLatency());
        assertTrue(400 == results.get(IN2).getOutbound().get(OUT2_1).getAverageLatency());
        assertTrue(500 == results.get(IN2).getOutbound().get(OUT2_1).getMaximumLatency());

        assertEquals(1, results.get(IN1).getOutbound().get(OUT1_1).getCount());
        assertEquals(2, results.get(IN1).getOutbound().get(OUT1_2).getCount());
        assertEquals(2, results.get(IN2).getOutbound().get(OUT2_1).getCount());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsWithoutUri() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setOperation(IN1);
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setElapsed(100);
        ct1_1.setInitial(true);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setOperation(OUT1_1);
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setElapsed(300);
        ct1_2.setInitial(true);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setOperation(OUT1_2);
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1600);
        ct1_3.setElapsed(200);
        ct1_3.setInitial(true);

        NodeDetails ct2_1 = new NodeDetails();
        ct2_1.setOperation(IN2);
        ct2_1.setTransaction(TXN);
        ct2_1.setTimestamp(1600);
        ct2_1.setElapsed(500);
        ct2_1.setInitial(true);

        NodeDetails ct2_2 = new NodeDetails();
        ct2_2.setOperation(OUT2_1);
        ct2_2.setTransaction(TXN);
        ct2_2.setTimestamp(1700);
        ct2_2.setElapsed(400);
        ct2_2.setInitial(true);

        NodeDetails ct2_3 = new NodeDetails();
        ct2_3.setOperation(IN2);
        ct2_3.setTransaction(TXN);
        ct2_3.setTimestamp(1700);
        ct2_3.setElapsed(600);
        ct2_3.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2, ct1_3, ct2_1, ct2_2, ct2_3));

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setLinkId("cd1");
        cd1.setTransaction(TXN);
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource(EP_IN1_OP);
        cd1.setTarget(EP_OUT1_1_OP);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setLinkId("cd2");
        cd2.setTransaction(TXN);
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource(EP_IN1_OP);
        cd2.setTarget(EP_OUT1_2_OP);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setLinkId("cd3");
        cd3.setTransaction(TXN);
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource(EP_IN2_OP);
        cd3.setTarget(EP_OUT2_1_OP);

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setLinkId("cd4");
        cd4.setTransaction(TXN);
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource(EP_IN1_OP);
        cd4.setTarget(EP_OUT1_2_OP);

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setLinkId("cd5");
        cd5.setTransaction(TXN);
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource(EP_IN2_OP);
        cd5.setTarget(EP_OUT2_1_OP);

        analytics.storeCommunicationDetails(null, Arrays.asList(cd1, cd2, cd3, cd4, cd5));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getCommunicationSummaryStatistics(null, criteria, false).size() == 5);
        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                            criteria, false);

        assertNotNull(stats);
        assertEquals(5, stats.size());

        Map<String,CommunicationSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

        assertTrue(results.containsKey(EP_IN1_OP));
        assertTrue(results.containsKey(EP_IN2_OP));
        assertTrue(results.containsKey(EP_OUT1_1_OP));
        assertTrue(results.containsKey(EP_OUT1_2_OP));
        assertTrue(results.containsKey(EP_OUT2_1_OP));

        assertEquals(IN1, results.get(EP_IN1_OP).getOperation());
        assertNull(results.get(EP_IN1_OP).getUri());

        assertEquals(1, results.get(EP_IN1_OP).getCount());
        assertEquals(2, results.get(EP_IN2_OP).getCount());
        assertEquals(1, results.get(EP_OUT1_1_OP).getCount());
        assertEquals(1, results.get(EP_OUT1_2_OP).getCount());
        assertEquals(1, results.get(EP_OUT2_1_OP).getCount());

        // Only check in2 node, as others only have single completion time
        assertTrue(500 == results.get(EP_IN2_OP).getMinimumDuration());
        assertTrue(550 == results.get(EP_IN2_OP).getAverageDuration());
        assertTrue(600 == results.get(EP_IN2_OP).getMaximumDuration());

        assertEquals(2, results.get(EP_IN1_OP).getOutbound().size());
        assertEquals(1, results.get(EP_IN2_OP).getOutbound().size());
        assertEquals(0, results.get(EP_OUT1_1_OP).getOutbound().size());
        assertEquals(0, results.get(EP_OUT1_2_OP).getOutbound().size());
        assertEquals(0, results.get(EP_OUT2_1_OP).getOutbound().size());

        assertTrue(results.get(EP_IN1_OP)
                .getOutbound().containsKey(EP_OUT1_1_OP));
        assertTrue(results.get(EP_IN1_OP)
                .getOutbound().containsKey(EP_OUT1_2_OP));
        assertTrue(results.get(EP_IN2_OP)
                .getOutbound().containsKey(EP_OUT2_1_OP));

        assertTrue(100 == results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_1_OP).getMinimumLatency());
        assertTrue(100 == results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_1_OP).getAverageLatency());
        assertTrue(100 == results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_1_OP).getMaximumLatency());
        assertTrue(200 == results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_2_OP).getMinimumLatency());
        assertTrue(250 == results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_2_OP).getAverageLatency());
        assertTrue(300 == results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_2_OP).getMaximumLatency());
        assertTrue(300 == results.get(EP_IN2_OP)
                .getOutbound().get(EP_OUT2_1_OP).getMinimumLatency());
        assertTrue(400 == results.get(EP_IN2_OP)
                .getOutbound().get(EP_OUT2_1_OP).getAverageLatency());
        assertTrue(500 == results.get(EP_IN2_OP)
                .getOutbound().get(EP_OUT2_1_OP).getMaximumLatency());

        assertEquals(1, results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_1_OP).getCount());
        assertEquals(2, results.get(EP_IN1_OP)
                .getOutbound().get(EP_OUT1_2_OP).getCount());
        assertEquals(2, results.get(EP_IN2_OP)
                .getOutbound().get(EP_OUT2_1_OP).getCount());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsWithOps() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setUri(IN1);
        ct1_1.setOperation(OP1);
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setElapsed(100);
        ct1_1.setInitial(true);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setUri(OUT1_1);
        ct1_2.setOperation(OP1_1);
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setElapsed(300);
        ct1_2.setInitial(true);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setUri(OUT1_2);
        ct1_3.setOperation(OP1_2);
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1600);
        ct1_3.setElapsed(200);
        ct1_3.setInitial(true);

        NodeDetails ct2_1 = new NodeDetails();
        ct2_1.setUri(IN2);
        ct2_1.setOperation(OP2);
        ct2_1.setTransaction(TXN);
        ct2_1.setTimestamp(1600);
        ct2_1.setElapsed(500);
        ct2_1.setInitial(true);

        NodeDetails ct2_2 = new NodeDetails();
        ct2_2.setUri(OUT2_1);
        ct2_2.setOperation(OP2_1);
        ct2_2.setTransaction(TXN);
        ct2_2.setTimestamp(1700);
        ct2_2.setElapsed(400);
        ct2_2.setInitial(true);

        NodeDetails ct2_3 = new NodeDetails();
        ct2_3.setUri(IN2);
        ct2_3.setOperation(OP2);
        ct2_3.setTransaction(TXN);
        ct2_3.setTimestamp(1700);
        ct2_3.setElapsed(600);
        ct2_3.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2, ct1_3, ct2_1, ct2_2, ct2_3));

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setLinkId("cd1");
        cd1.setTransaction(TXN);
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource(EP_INOP1);
        cd1.setTarget(EP_OUTOP1_1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setLinkId("cd2");
        cd2.setTransaction(TXN);
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource(EP_INOP1);
        cd2.setTarget(EP_OUTOP1_2);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setLinkId("cd3");
        cd3.setTransaction(TXN);
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource(EP_INOP2);
        cd3.setTarget(EP_OUTOP2_1);

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setLinkId("cd4");
        cd4.setTransaction(TXN);
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource(EP_INOP1);
        cd4.setTarget(EP_OUTOP1_2);

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setLinkId("cd5");
        cd5.setTransaction(TXN);
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource(EP_INOP2);
        cd5.setTarget(EP_OUTOP2_1);

        analytics.storeCommunicationDetails(null, Arrays.asList(cd1, cd2, cd3, cd4, cd5));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getCommunicationSummaryStatistics(null, criteria, false).size() == 5);
        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                                criteria, false);

        assertNotNull(stats);
        assertEquals(5, stats.size());

        Map<String,CommunicationSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

        assertTrue(results.containsKey(EP_INOP1));
        assertTrue(results.containsKey(EP_INOP2));
        assertTrue(results.containsKey(EP_OUTOP1_1));
        assertTrue(results.containsKey(EP_OUTOP1_2));
        assertTrue(results.containsKey(EP_OUTOP2_1));

        assertEquals(IN1, results.get(EP_INOP1).getUri());
        assertEquals(OP1, results.get(EP_INOP1).getOperation());

        assertEquals(1, results.get(EP_INOP1).getCount());
        assertEquals(2, results.get(EP_INOP2).getCount());
        assertEquals(1, results.get(EP_OUTOP1_1).getCount());
        assertEquals(1, results.get(EP_OUTOP1_2).getCount());
        assertEquals(1, results.get(EP_OUTOP2_1).getCount());

        // Only check in2 node, as others only have single completion time
        assertTrue(500 == results.get(EP_INOP2).getMinimumDuration());
        assertTrue(550 == results.get(EP_INOP2).getAverageDuration());
        assertTrue(600 == results.get(EP_INOP2).getMaximumDuration());

        assertEquals(2, results.get(EP_INOP1).getOutbound().size());
        assertEquals(1, results.get(EP_INOP2).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_1).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_2).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP2_1).getOutbound().size());

        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_1));
        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_2));
        assertTrue(results.get(EP_INOP2).getOutbound().containsKey(EP_OUTOP2_1));

        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getMinimumLatency());
        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getAverageLatency());
        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getMaximumLatency());
        assertTrue(200 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getMinimumLatency());
        assertTrue(250 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getAverageLatency());
        assertTrue(300 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getMaximumLatency());
        assertTrue(300 == results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getMinimumLatency());
        assertTrue(400 == results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getAverageLatency());
        assertTrue(500 == results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getMaximumLatency());

        assertEquals(1, results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getCount());
        assertEquals(2, results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getCount());
        assertEquals(2, results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getCount());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsWithOpsAndInternalLinks() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setUri(IN1);
        ct1_1.setOperation(OP1);
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setElapsed(50);
        ct1_1.setInitial(true);

        NodeDetails ct1_1_internal = new NodeDetails();
        ct1_1_internal.setUri(IN1);
        ct1_1_internal.setOperation(OP1);
        ct1_1_internal.setTransaction(TXN);
        ct1_1_internal.setTimestamp(1550);
        ct1_1_internal.setElapsed(100);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setUri(OUT1_1);
        ct1_2.setOperation(OP1_1);
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setElapsed(300);
        ct1_2.setInitial(true);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setUri(OUT1_2);
        ct1_3.setOperation(OP1_2);
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1600);
        ct1_3.setElapsed(200);
        ct1_3.setInitial(true);

        NodeDetails ct2_1 = new NodeDetails();
        ct2_1.setUri(IN2);
        ct2_1.setOperation(OP2);
        ct2_1.setTransaction(TXN);
        ct2_1.setTimestamp(1600);
        ct2_1.setElapsed(500);
        ct2_1.setInitial(true);

        NodeDetails ct2_2 = new NodeDetails();
        ct2_2.setUri(OUT2_1);
        ct2_2.setOperation(OP2_1);
        ct2_2.setTransaction(TXN);
        ct2_2.setTimestamp(1700);
        ct2_2.setElapsed(400);
        ct2_2.setInitial(true);

        NodeDetails ct2_3 = new NodeDetails();
        ct2_3.setUri(IN2);
        ct2_3.setOperation(OP2);
        ct2_3.setTransaction(TXN);
        ct2_3.setTimestamp(1700);
        ct2_3.setElapsed(600);
        ct2_3.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_1_internal,
                ct1_2, ct1_3, ct2_1, ct2_2, ct2_3));

        CommunicationDetails cd1internal = new CommunicationDetails();
        cd1internal.setLinkId("cd1internal");
        cd1internal.setTransaction(TXN);
        cd1internal.setTimestamp(1500);
        cd1internal.setLatency(50);
        cd1internal.setSource(EP_INOP1);
        cd1internal.setTarget(EP_INOP1);
        cd1internal.setInternal(true);

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setLinkId("cd1");
        cd1.setTransaction(TXN);
        cd1.setTimestamp(1550);
        cd1.setLatency(100);
        cd1.setSource(EP_INOP1);
        cd1.setTarget(EP_OUTOP1_1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setLinkId("cd2");
        cd2.setTransaction(TXN);
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource(EP_INOP1);
        cd2.setTarget(EP_OUTOP1_2);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setLinkId("cd3");
        cd3.setTransaction(TXN);
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource(EP_INOP2);
        cd3.setTarget(EP_OUTOP2_1);

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setLinkId("cd4");
        cd4.setTransaction(TXN);
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource(EP_INOP1);
        cd4.setTarget(EP_OUTOP1_2);

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setLinkId("cd5");
        cd5.setTransaction(TXN);
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource(EP_INOP2);
        cd5.setTarget(EP_OUTOP2_1);

        analytics.storeCommunicationDetails(null, Arrays.asList(cd1internal, cd1, cd2, cd3, cd4, cd5));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);

        Wait.until(() -> analytics.getCommunicationSummaryStatistics(null, criteria, false).size() == 5);
        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                                criteria, false);

        assertNotNull(stats);
        assertEquals(5, stats.size());

        Map<String,CommunicationSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

        assertTrue(results.containsKey(EP_INOP1));
        assertTrue(results.containsKey(EP_INOP2));
        assertTrue(results.containsKey(EP_OUTOP1_1));
        assertTrue(results.containsKey(EP_OUTOP1_2));
        assertTrue(results.containsKey(EP_OUTOP2_1));

        // NOTE: Although only called once, because an internal component was called with the
        // same source URL (to propagate it through to fragments externally communicating),
        // then it couts the internal fragment aswell. Issue with this is the aggregation of
        // stats across the top level and internal fragments - but at this stage would be difficult
        // to accumulate all spawned fragments for a single call to the service, to get an
        // overall value.
        assertEquals(1, results.get(EP_INOP1).getCount());
        assertEquals(2, results.get(EP_INOP2).getCount());
        assertEquals(1, results.get(EP_OUTOP1_1).getCount());
        assertEquals(1, results.get(EP_OUTOP1_2).getCount());
        assertEquals(1, results.get(EP_OUTOP2_1).getCount());

        // Only check in2 node, as others only have single completion time
        assertTrue(500 == results.get(EP_INOP2).getMinimumDuration());
        assertTrue(550 == results.get(EP_INOP2).getAverageDuration());
        assertTrue(600 == results.get(EP_INOP2).getMaximumDuration());

        assertEquals(2, results.get(EP_INOP1).getOutbound().size());
        assertEquals(1, results.get(EP_INOP2).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_1).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_2).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP2_1).getOutbound().size());

        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_1));
        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_2));
        assertTrue(results.get(EP_INOP2).getOutbound().containsKey(EP_OUTOP2_1));

        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getMinimumLatency());
        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getAverageLatency());
        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getMaximumLatency());
        assertTrue(200 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getMinimumLatency());
        assertTrue(250 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getAverageLatency());
        assertTrue(300 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getMaximumLatency());
        assertTrue(300 == results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getMinimumLatency());
        assertTrue(400 == results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getAverageLatency());
        assertTrue(500 == results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getMaximumLatency());

        assertEquals(1, results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getCount());
        assertEquals(2, results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getCount());
        assertEquals(2, results.get(EP_INOP2).getOutbound().get(EP_OUTOP2_1).getCount());
    }

    @Test
    public void testGetCommunicationSummaryStatisticForPrincipal() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setUri(IN1);
        ct1_1.setOperation(OP1);
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setElapsed(100);
        ct1_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1_1.setInitial(true);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setUri(OUT1_1);
        ct1_2.setOperation(OP1_1);
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setElapsed(300);
        ct1_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1_2.setInitial(true);

        NodeDetails ct1_3 = new NodeDetails();
        ct1_3.setUri(OUT1_2);
        ct1_3.setOperation(OP1_2);
        ct1_3.setTransaction(TXN);
        ct1_3.setTimestamp(1600);
        ct1_3.setElapsed(200);
        ct1_3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));
        ct1_3.setInitial(true);

        NodeDetails ct2_1 = new NodeDetails();
        ct2_1.setUri(IN2);
        ct2_1.setOperation(OP2);
        ct2_1.setTransaction(TXN);
        ct2_1.setTimestamp(1600);
        ct2_1.setElapsed(500);
        ct2_1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2_1.setInitial(true);

        NodeDetails ct2_2 = new NodeDetails();
        ct2_2.setUri(OUT2_1);
        ct2_2.setOperation(OP2_1);
        ct2_2.setTransaction(TXN);
        ct2_2.setTimestamp(1700);
        ct2_2.setElapsed(400);
        ct2_2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2_2.setInitial(true);

        NodeDetails ct2_3 = new NodeDetails();
        ct2_3.setUri(IN2);
        ct2_3.setOperation(OP2);
        ct2_3.setTransaction(TXN);
        ct2_3.setTimestamp(1700);
        ct2_3.setElapsed(600);
        ct2_3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));
        ct2_3.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2, ct1_3, ct2_1, ct2_2, ct2_3));

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setLinkId("cd1");
        cd1.setTransaction(TXN);
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource(EP_INOP1);
        cd1.setTarget(EP_OUTOP1_1);
        cd1.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setLinkId("cd2");
        cd2.setTransaction(TXN);
        cd2.setTimestamp(1500);
        cd2.setLatency(200);
        cd2.setSource(EP_INOP1);
        cd2.setTarget(EP_OUTOP1_2);
        cd2.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setLinkId("cd3");
        cd3.setTransaction(TXN);
        cd3.setTimestamp(1500);
        cd3.setLatency(300);
        cd3.setSource(EP_INOP2);
        cd3.setTarget(EP_OUTOP2_1);
        cd3.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        CommunicationDetails cd4 = new CommunicationDetails();
        cd4.setLinkId("cd4");
        cd4.setTransaction(TXN);
        cd4.setTimestamp(1600);
        cd4.setLatency(300);
        cd4.setSource(EP_INOP1);
        cd4.setTarget(EP_OUTOP1_2);
        cd4.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p1"));

        CommunicationDetails cd5 = new CommunicationDetails();
        cd5.setLinkId("cd5");
        cd5.setTransaction(TXN);
        cd5.setTimestamp(1600);
        cd5.setLatency(500);
        cd5.setSource(EP_INOP2);
        cd5.setTarget(EP_OUTOP2_1);
        cd5.getProperties().add(new Property(Constants.PROP_PRINCIPAL, "p2"));

        analytics.storeCommunicationDetails(null, Arrays.asList(cd1, cd2, cd4, cd3, cd5));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);
        criteria.addProperty(Constants.PROP_PRINCIPAL, "p1", Operator.HAS);

        Wait.until(() -> analytics.getCommunicationSummaryStatistics(null, criteria, false).size() == 3);
        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                                    criteria, false);

        assertNotNull(stats);
        assertEquals(3, stats.size());

        Map<String,CommunicationSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

        assertTrue(results.containsKey(EP_INOP1));
        assertTrue(results.containsKey(EP_OUTOP1_1));
        assertTrue(results.containsKey(EP_OUTOP1_2));

        assertEquals(1, results.get(EP_INOP1).getCount());
        assertEquals(1, results.get(EP_OUTOP1_1).getCount());
        assertEquals(1, results.get(EP_OUTOP1_2).getCount());

        assertEquals(2, results.get(EP_INOP1).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_1).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_2).getOutbound().size());

        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_1));
        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_2));

        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getMinimumLatency());
        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getAverageLatency());
        assertTrue(100 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getMaximumLatency());
        assertTrue(200 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getMinimumLatency());
        assertTrue(250 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getAverageLatency());
        assertTrue(300 == results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getMaximumLatency());

        assertEquals(1, results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_1).getCount());
        assertEquals(2, results.get(EP_INOP1).getOutbound().get(EP_OUTOP1_2).getCount());
    }

    @Test
    public void testGetCommunicationSummaryStatisticForHost() throws StoreException {
        NodeDetails ct1_1 = new NodeDetails();
        ct1_1.setUri(IN1);
        ct1_1.setOperation(OP1);
        ct1_1.setTransaction(TXN);
        ct1_1.setTimestamp(1500);
        ct1_1.setElapsed(100);
        ct1_1.setHostName("hostA");
        ct1_1.setInitial(true);

        NodeDetails ct1_2 = new NodeDetails();
        ct1_2.setUri(OUT1_1);
        ct1_2.setOperation(OP1_1);
        ct1_2.setTransaction(TXN);
        ct1_2.setTimestamp(1600);
        ct1_2.setElapsed(300);
        ct1_2.setHostName("hostB");
        ct1_2.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1_1, ct1_2));

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setLinkId("cd1");
        cd1.setTransaction(TXN);
        cd1.setTimestamp(1500);
        cd1.setLatency(100);
        cd1.setSource(EP_INOP1);
        cd1.setTarget(EP_OUTOP1_1);
        cd1.setSourceHostName("hostA");
        cd1.setTargetHostName("hostB");

        analytics.storeCommunicationDetails(null, Collections.singletonList(cd1));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1).setEndTime(10000);
        criteria.setHostName("hostA");

        Wait.until(() -> analytics.getCommunicationSummaryStatistics(null, criteria, false).size() == 2);
        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                                    criteria, false);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        Map<String,CommunicationSummaryStatistics> results=
                stats.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));

        assertTrue(results.containsKey(EP_INOP1));
        assertTrue(results.containsKey(EP_OUTOP1_1));

        assertEquals(1, results.get(EP_INOP1).getCount());
        assertEquals(0, results.get(EP_OUTOP1_1).getCount());

        assertEquals(1, results.get(EP_INOP1).getOutbound().size());
        assertEquals(0, results.get(EP_OUTOP1_1).getOutbound().size());

        assertTrue(results.get(EP_INOP1).getOutbound().containsKey(EP_OUTOP1_1));
    }

    @Test
    public void testGetCommunicationSummaryStatisticsServiceName() throws StoreException {
        NodeDetails ct1 = new NodeDetails();
        ct1.setTransaction("testapp");
        ct1.setUri("in1");
        ct1.setOperation("op1");
        ct1.getProperties().add(new Property("prop", "val"));
        ct1.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, "wildfly"));
        ct1.setInitial(true);

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setTransaction("testapp");
        cd1.setSource("in1[op1]");
        cd1.setTarget("out1.1[op1.1]");

        analytics.storeNodeDetails(null, Arrays.asList(ct1));
        analytics.storeCommunicationDetails(null, Arrays.asList(cd1));

        Criteria criteria = new Criteria()
                .setStartTime(0)
                .setEndTime(100000)
                .setTransaction("testapp");

        Collection<CommunicationSummaryStatistics> communicationSummaryStatisticsList =
                analytics.getCommunicationSummaryStatistics(null, criteria, false);

        Assert.assertEquals(1, communicationSummaryStatisticsList.size());
        CommunicationSummaryStatistics communicationSummaryStatistics =
                communicationSummaryStatisticsList.iterator().next();

        Assert.assertEquals("wildfly", communicationSummaryStatistics.getServiceName());
        Assert.assertEquals(1, communicationSummaryStatistics.getOutbound().size());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsServiceNameMissingOperation() throws StoreException {
        NodeDetails ct1 = new NodeDetails();
        ct1.setUri("in1");
        ct1.setTransaction("testapp");
        ct1.getProperties().add(new Property("prop", "val"));
        ct1.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, "wildfly"));
        ct1.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1));

        Criteria criteria = new Criteria()
                .setStartTime(0)
                .setEndTime(100000)
                .setTransaction("testapp");

        Collection<CommunicationSummaryStatistics> communicationSummaryStatisticsList =
                analytics.getCommunicationSummaryStatistics(null, criteria, false);

        Assert.assertEquals(1, communicationSummaryStatisticsList.size());
        CommunicationSummaryStatistics communicationSummaryStatistics =
                communicationSummaryStatisticsList.iterator().next();

        Assert.assertEquals("wildfly", communicationSummaryStatistics.getServiceName());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsServiceNameMissingURI() throws StoreException {
        NodeDetails ct1 = new NodeDetails();
        ct1.setOperation("op1");
        ct1.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, "wildfly"));
        ct1.setInitial(true);

        analytics.storeNodeDetails(null, Arrays.asList(ct1));

        Criteria criteria = new Criteria()
                .setStartTime(0)
                .setEndTime(100000);

        Collection<CommunicationSummaryStatistics> communicationSummaryStatisticsList =
                analytics.getCommunicationSummaryStatistics(null, criteria, false);

        Assert.assertEquals(1, communicationSummaryStatisticsList.size());
        CommunicationSummaryStatistics communicationSummaryStatistics =
                communicationSummaryStatisticsList.iterator().next();

        Assert.assertEquals("wildfly", communicationSummaryStatistics.getServiceName());
    }

    @Test // HWKAPM-771
    public void testGetCommunicationSummaryStatisticsServiceNameOnClientNode() throws StoreException {
        NodeDetails nd1 = new NodeDetails();
        nd1.setUri("/call");
        nd1.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, "A"));
        nd1.setInitial(true);
        nd1.setType(NodeType.Producer);
        nd1.setElapsed(1000);

        NodeDetails nd2 = new NodeDetails();
        nd2.setUri("/call");
        nd2.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, "B"));
        nd2.setInitial(true);
        nd2.setType(NodeType.Consumer);
        nd2.setElapsed(500);

        CommunicationDetails cd = new CommunicationDetails();
        cd.setSource("client:/call");
        cd.setTarget("/call");

        analytics.storeNodeDetails(null, Arrays.asList(nd1, nd2));
        analytics.storeCommunicationDetails(null, Collections.singletonList(cd));

        Criteria criteria = new Criteria()
                .setStartTime(0)
                .setEndTime(100000);

        Collection<CommunicationSummaryStatistics> communicationSummaryStatisticsList =
                analytics.getCommunicationSummaryStatistics(null, criteria, false);

        Assert.assertEquals(2, communicationSummaryStatisticsList.size());
        CommunicationSummaryStatistics producer =
                communicationSummaryStatisticsList.stream().filter(cs -> cs.getUri()
                        .startsWith("client:")).findFirst().orElse(null);
        CommunicationSummaryStatistics consumer =
                communicationSummaryStatisticsList.stream().filter(cs -> !cs.getUri()
                        .startsWith("client:")).findFirst().orElse(null);

        assertNotNull(consumer);
        assertNotNull(producer);

        assertEquals(1, producer.getOutbound().size());

        assertEquals("B", consumer.getServiceName());
        assertEquals("A", producer.getServiceName());

        assertEquals(nd2.getElapsed(), consumer.getAverageDuration());
        assertEquals(nd1.getElapsed(), producer.getAverageDuration());
    }

    @Test
    public void testHostNames() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000);
        trace1.setHostName("hostA");

        Trace trace2 = new Trace();
        trace2.setTimestamp(2000);
        trace2.setHostName("hostB");

        bts.storeFragments(null, Arrays.asList(trace1, trace2));

        Wait.until(() -> analytics.getHostNames(null, new Criteria().setStartTime(1)).size() == 2);
        Set<String> hostnames = analytics.getHostNames(null, new Criteria().setStartTime(1));

        assertNotNull(hostnames);
        assertEquals(2, hostnames.size());

        assertTrue(hostnames.contains("hostA"));
        assertTrue(hostnames.contains("hostB"));
    }

    @Test
    public void testGetEndpointResponseTimeseriesStatistics() throws StoreException {
        NodeDetails nd1 = new NodeDetails();
        nd1.setTimestamp(1500);
        nd1.setActual(100);
        nd1.setElapsed(400);
        nd1.setType(NodeType.Consumer);

        // Should not be included in the aggregation as not a Consumer
        NodeDetails nd2 = new NodeDetails();
        nd2.setTimestamp(1500);
        nd2.setActual(100);
        nd2.setElapsed(200);
        nd2.setType(NodeType.Component);
        nd2.setComponentType(Constants.COMPONENT_DATABASE);

        analytics.storeNodeDetails(null, Arrays.asList(nd1, nd2));

        Criteria criteria = new Criteria().setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getEndpointResponseTimeseriesStatistics(null, criteria, 1000).size() == 1
        );
        List<TimeseriesStatistics> stats = analytics.getEndpointResponseTimeseriesStatistics(null, criteria,
                1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1000, stats.get(0).getTimestamp());
        assertEquals(1, stats.get(0).getCount());

        assertTrue(stats.get(0).getMin() == 400);
        assertTrue(stats.get(0).getAverage() == 400);
        assertTrue(stats.get(0).getMax() == 400);

        assertEquals(0, stats.get(0).getFaultCount());
    }

    @Test
    public void testEndpointPropertyDetails() throws StoreException {
        NodeDetails nd1 = new NodeDetails();
        nd1.setTimestamp(1500);
        nd1.setInitial(true);
        nd1.getProperties().add(new Property("buildStamp", "v1", PropertyType.Text));

        // Should not be included in the aggregation as not a Consumer
        NodeDetails nd2 = new NodeDetails();
        nd2.setTimestamp(1500);
        nd2.getProperties().add(new Property("buildStamp", "v2", PropertyType.Text));

        analytics.storeNodeDetails(null, Arrays.asList(nd1, nd2));

        Criteria criteria = new Criteria().setStartTime(1).setEndTime(10000);

        Wait.until(() ->
            analytics.getEndpointPropertyDetails(null, criteria, "buildStamp").size() == 1
        );
        List<Cardinality> cards1 = analytics.getEndpointPropertyDetails(null, criteria,
                "buildStamp");

        assertNotNull(cards1);
        assertEquals(1, cards1.size());

        assertEquals("v1", cards1.get(0).getValue());
        assertEquals(1, cards1.get(0).getCount());
    }

}
