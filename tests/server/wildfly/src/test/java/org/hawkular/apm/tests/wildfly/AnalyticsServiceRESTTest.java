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
package org.hawkular.apm.tests.wildfly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.analytics.service.rest.client.AnalyticsServiceRESTClient;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.PropertyType;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.EndpointInfo;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.PrincipalInfo;
import org.hawkular.apm.api.model.analytics.PropertyInfo;
import org.hawkular.apm.api.model.analytics.TransactionInfo;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class AnalyticsServiceRESTTest {

    /**  */
    private static final String MY_FAULT = "MyFault";
    /**  */
    private static final String TESTAPP = "testapp";
    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    private static AnalyticsServiceRESTClient analytics;

    private static TraceServiceRESTClient service;

    private static TracePublisherRESTClient publisher;

    @BeforeClass
    public static void initClass() {
        analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        service = new TraceServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        publisher = new TracePublisherRESTClient();
        publisher.setUsername(TEST_USERNAME);
        publisher.setPassword(TEST_PASSWORD);
    }

    @Before
    public void initTest() {
        analytics.clear(null);
        service.clear(null);
    }

    @Test
    public void testGetUnboundEndpoints() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored trace
        List<EndpointInfo> endpoints = analytics.getUnboundEndpoints(null, 0, 0, true);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertEquals("testuri", endpoints.get(0).getEndpoint());
    }

    @Test
    public void testGetBoundEndpoints() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored trace Endpoints
        List<EndpointInfo> endpoints = analytics.getBoundEndpoints(null, "trace1", 0, 0);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains(new EndpointInfo("testuri")));
    }

    @Test
    public void testGetTransactionInfo() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Component c1 = new Component();
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction("trace2");
        trace2.setStartTime(System.currentTimeMillis() - 2000); // Within last hour

        Component c2 = new Component();
        c2.getProperties().add(new Property("prop2", "value2"));
        trace2.getNodes().add(c2);

        publisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 2);

        List<TransactionInfo> tis = analytics.getTransactionInfo(null, new Criteria());

        assertNotNull(tis);
        assertEquals(2, tis.size());
        assertEquals("trace1", tis.get(0).getName());
        assertEquals("trace2", tis.get(1).getName());
    }

    @Test
    public void testGetPropertyInfo() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        publisher.publish(null, Arrays.asList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria=new Criteria()
                .setBusinessTransaction("trace1")
                .setStartTime(0)
                .setEndTime(0);

        List<PropertyInfo> pis = analytics.getPropertyInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
    }

    @Test
    public void testGetPrincipalInfo() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setPrincipal("p1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        publisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria=new Criteria()
                .setBusinessTransaction("trace1")
                .setStartTime(0)
                .setEndTime(0);

        List<PrincipalInfo> pis = analytics.getPrincipalInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getId().equals("p1"));
        assertEquals(1, pis.get(0).getCount());
    }

    @Test
    public void testGetCompletionCount() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, criteria).size() == 1);

        // Get transaction count
        Long count = analytics.getTraceCompletionCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionCountWithPropertyFilter() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.getProperties().add(new Property("prop1", "2.5", PropertyType.Number));
        c1.getProperties().add(new Property("prop2", "hello"));
        trace1.getNodes().add(c1);

        publisher.publish(null, Arrays.asList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria().setStartTime(0).setEndTime(0)).size() == 1);

        // Get transaction count
        assertEquals(1, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "1", Operator.GT)));

        assertEquals(1, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "3", Operator.LT)));

        assertEquals(0, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "2.4", Operator.LT)));

        assertEquals(1, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HAS)));

        assertEquals(0, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HASNOT)));
    }

    @Test
    public void testGetCompletionFaultCount() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setFault("Failed");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, criteria).size() == 1);

        // Get transaction count
        Long count = analytics.getTraceCompletionFaultCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionTimes() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Get trace completion times
        List<CompletionTime> times = analytics.getTraceCompletionTimes(null, criteria);

        assertNotNull(times);
        assertEquals(1, times.size());

        CompletionTime ct = times.get(0);
        assertEquals(TESTAPP, ct.getBusinessTransaction());
    }

    @Test
    public void testGetCompletionTimesWithPropertyFilter() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri1");
        c1.setFault(MY_FAULT);
        c1.getProperties().add(new Property("prop2", "hello"));
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction(TESTAPP);
        trace2.setStartTime(System.currentTimeMillis() - 2000); // Within last hour
        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.getProperties().add(new Property("prop1", "2.5", PropertyType.Number));
        trace2.getNodes().add(c2);

        publisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 2);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 2);

        // Get trace completion times
        Criteria criteria = new Criteria()
                .setBusinessTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HAS);

        List<CompletionTime> times = analytics.getTraceCompletionTimes(null, criteria);

        assertNotNull(times);
        assertEquals(1, times.size());

        CompletionTime ct = times.get(0);
        assertEquals("1", ct.getId());
        assertNotNull(ct.getFault());
        assertEquals(MY_FAULT, ct.getFault());
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetCompletionPropertyDetails() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        publisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getTraceCompletionPropertyDetails(null, criteria, "prop1");

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionFaultDetails() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setFault("fault1");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, criteria).size() == 1);

        List<Cardinality> cards = analytics.getTraceCompletionFaultDetails(null, criteria);

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetNodeTimeseriesStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsHostName() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        criteria = new Criteria();
        criteria.setHostName("hostB").setStartTime(0).setEndTime(0);

        // Get transaction count
        stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsHostName() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        Wait.until(() -> analytics.getNodeSummaryStatistics(null, criteria).size() == 2);
        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);
        assertNotNull(stats);
        assertEquals(2, stats.size());

        Criteria criteria2 = new Criteria();
        criteria2.setHostName("hostB").setStartTime(0).setEndTime(0);

        stats = analytics.getNodeSummaryStatistics(null, criteria2);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsFlat() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setDuration(1000000);
        p1.addInteractionCorrelationId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction(TESTAPP);
        trace2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setDuration(500000);
        c2.addInteractionCorrelationId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                criteria, false);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("COMMS STATS=" + mapper.writeValueAsString(stats));

        CommunicationSummaryStatistics first = null;
        CommunicationSummaryStatistics second = null;

        for (CommunicationSummaryStatistics css : stats) {
            if (css.getId().equals("originuri")) {
                first = css;
            } else if (css.getId().equals("testuri")) {
                second = css;
            } else {
                fail("Unknown uri: " + css.getId());
            }
        }

        assertNotNull(first);
        assertNotNull(second);

        assertEquals(1, first.getOutbound().size());
        assertEquals(0, second.getOutbound().size());

        assertEquals(first.getOutbound().keySet().iterator().next(), second.getId());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsTree() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setDuration(1000000);
        p1.addInteractionCorrelationId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction(TESTAPP);
        trace2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setDuration(500000);
        c2.addInteractionCorrelationId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        publisher.publish(null, traces);

        // Wait to ensure record persisted, 10 seconds didn't seem enough :/
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                criteria, true);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        System.out.println("COMMS STATS=" + mapper.writeValueAsString(stats));

        CommunicationSummaryStatistics first = stats.iterator().next();

        assertNotNull(first);

        assertEquals(1, first.getOutbound().size());

        assertTrue(first.getOutbound().containsKey("testuri"));
        assertNotNull(first.getOutbound().get("testuri").getNode());

        CommunicationSummaryStatistics second = first.getOutbound().get("testuri").getNode();

        assertEquals(0, second.getOutbound().size());

        assertEquals(first.getOutbound().keySet().iterator().next(), second.getId());
        assertEquals("originuri", first.getId());
        assertEquals("testuri", second.getId());
    }

    @Test
    public void testGetHostNames() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        publisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Set<String> hosts = analytics.getHostNames(null, criteria);

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertTrue(hosts.contains("hostA"));
    }

    @Test
    public void testGetCompletionTimeMultiFragment() throws Exception {
        long baseTime=System.currentTimeMillis() - 4000;

        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction(TESTAPP);
        trace1.setStartTime(baseTime); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri2");
        c1.setDuration(1000000000);
        c1.setBaseTime(1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setDuration(1000000000);
        c1.setBaseTime(1);
        p1.addInteractionCorrelationId("interaction2");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction(TESTAPP);
        trace2.setStartTime(baseTime + 1000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(500000000);
        c2.setBaseTime(1);
        c2.addInteractionCorrelationId("interaction2");

        Component comp2 = new Component();
        comp2.setDuration(1500000000);
        comp2.setBaseTime(1);
        c2.getNodes().add(comp2);

        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        assertEquals(1000, trace1.calculateDuration());
        assertEquals(1500, trace2.calculateDuration());

        publisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analytics.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Get transaction count
        Wait.until(() -> analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 10000).size() == 1);
        List<CompletionTimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 10000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1750, stats.get(0).getAverage());
        assertEquals(1, stats.get(0).getCount());
    }

}
