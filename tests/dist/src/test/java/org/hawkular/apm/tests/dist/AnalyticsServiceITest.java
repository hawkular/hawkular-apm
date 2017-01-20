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
package org.hawkular.apm.tests.dist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.analytics.service.rest.client.AnalyticsServiceRESTClient;
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
import org.hawkular.apm.api.model.analytics.TransactionInfo;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.config.service.rest.client.ConfigurationServiceRESTClient;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AnalyticsServiceITest extends AbstractITest {
    /**
     * Default Criteria returns results within one the last hour,
     * therefore subtracting this constant from current time.
     */
    private static final int FOUR_MS_IN_MICRO_SEC = 4000;

    private static final String MY_FAULT = "MyFault";
    private static final String TESTAPP = "testapp";
    private static final String ANOAPP = "anoapp";

    private static AnalyticsServiceRESTClient analyticsService;
    private static TraceServiceRESTClient traceService;
    private static TracePublisherRESTClient tracePublisher;
    private static ConfigurationServiceRESTClient configService;


    @BeforeClass
    public static void initClass() {
        analyticsService = new AnalyticsServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
        traceService = new TraceServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
        tracePublisher = new TracePublisherRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
        configService = new ConfigurationServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
    }

    @Before
    public void initTest() {
        analyticsService.clear(null);
        traceService.clear(null);
        configService.clear(null);
    }

    @Test
    public void testGetUnboundEndpoints() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        // Retrieve stored trace
        List<EndpointInfo> endpoints = analyticsService.getUnboundEndpoints(null, 0, 0, true);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertEquals("testuri", endpoints.get(0).getEndpoint());
    }

    @Test
    public void testGetBoundEndpoints() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction("trace1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        // Retrieve stored trace Endpoints
        List<EndpointInfo> endpoints = analyticsService.getBoundEndpoints(null, "trace1", 0, 0);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains(new EndpointInfo("testuri")));
    }

    @Test
    public void testGetTransactionInfo() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction("trace1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Component c1 = new Component();
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setTraceId("2");
        trace2.setFragmentId("2");
        trace2.setTransaction("trace2");
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Component c2 = new Component();
        c2.getProperties().add(new Property("prop2", "value2"));
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 2);

        TransactionConfig btxnconfig1 = new TransactionConfig();
        btxnconfig1.setLevel(ReportingLevel.Ignore);
        btxnconfig1.setFilter(new Filter());
        btxnconfig1.getFilter().getInclusions().add("myfilter");

        configService.setTransaction(null, "btxn1", btxnconfig1);

        List<TransactionInfo> tis = analyticsService.getTransactionInfo(null, new Criteria());

        assertNotNull(tis);
        assertEquals(3, tis.size());
        assertEquals("btxn1", tis.get(0).getName());
        assertEquals(ReportingLevel.Ignore, tis.get(0).getLevel());
        assertTrue(tis.get(0).isStaticConfig());
        assertEquals("trace1", tis.get(1).getName());
        assertEquals(ReportingLevel.All, tis.get(1).getLevel());
        assertFalse(tis.get(1).isStaticConfig());
        assertEquals("trace2", tis.get(2).getName());
        assertEquals(ReportingLevel.All, tis.get(2).getLevel());
        assertFalse(tis.get(2).isStaticConfig());
    }

    @Test
    public void testGetPropertyInfo() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction("trace1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Arrays.asList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria=new Criteria()
                .setTransaction("trace1")
                .setStartTime(0)
                .setEndTime(0);

        List<PropertyInfo> pis = analyticsService.getPropertyInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
    }

    @Test
    public void testGetCompletionCount() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, criteria).size() == 1);

        // Get transaction count
        Long count = analyticsService.getTraceCompletionCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionCountWithPropertyFilter() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.getProperties().add(new Property("prop1", "2.5", PropertyType.Number));
        c1.getProperties().add(new Property("prop2", "hello"));
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Arrays.asList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria().setStartTime(0).setEndTime(0)).size() == 1);

        // Get transaction count
        assertEquals(1, analyticsService.getTraceCompletionCount(null, new Criteria()
                .setTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "1", Operator.GT)));

        assertEquals(1, analyticsService.getTraceCompletionCount(null, new Criteria()
                .setTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "3", Operator.LT)));

        assertEquals(0, analyticsService.getTraceCompletionCount(null, new Criteria()
                .setTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "2.4", Operator.LT)));

        assertEquals(1, analyticsService.getTraceCompletionCount(null, new Criteria()
                .setTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HAS)));

        assertEquals(0, analyticsService.getTraceCompletionCount(null, new Criteria()
                .setTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HASNOT)));
    }

    @Test
    public void testGetCompletionFaultCount() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.getProperties().add(new Property(Constants.PROP_FAULT, "Failed"));
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, criteria).size() == 1);

        // Get transaction count
        Long count = analyticsService.getTraceCompletionFaultCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionTimes() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, criteria);

        assertNotNull(times);
        assertEquals(1, times.size());

        CompletionTime ct = times.get(0);
        assertEquals(TESTAPP, ct.getTransaction());
    }

    @Test
    public void testGetCompletionTimesWithPropertyFilter() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri1");
        c1.getProperties().add(new Property(Constants.PROP_FAULT, MY_FAULT));
        c1.getProperties().add(new Property("prop2", "hello"));
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setTraceId("2");
        trace2.setFragmentId("2");
        trace2.setTransaction(TESTAPP);
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.getProperties().add(new Property("prop1", "2.5", PropertyType.Number));
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 2);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 2);

        // Get trace completion times
        Criteria criteria = new Criteria()
                .setTransaction(TESTAPP)
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HAS);

        List<CompletionTime> times = analyticsService.getTraceCompletions(null, criteria);

        assertNotNull(times);
        assertEquals(1, times.size());

        CompletionTime ct = times.get(0);
        assertEquals("1", ct.getId());
        assertEquals(1, ct.getProperties(Constants.PROP_FAULT).size());
        assertEquals(MY_FAULT, ct.getProperties(Constants.PROP_FAULT).iterator().next().getValue());
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Get transaction count
        List<TimeseriesStatistics> stats = analyticsService
                .getTraceCompletionTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetCompletionPropertyDetails() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setTraceId("2");
        trace2.setFragmentId("2");
        trace2.setTransaction(ANOAPP);
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(1000000);
        c2.getProperties().add(new Property("prop1", "value2"));
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 2);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 2);

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analyticsService.getTraceCompletionPropertyDetails(null, criteria, "prop1");

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionFaultDetails() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.getProperties().add(new Property(Constants.PROP_FAULT, "fault1"));
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, criteria).size() == 1);

        List<Cardinality> cards = analyticsService.getTraceCompletionFaultDetails(null, criteria);

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetNodeTimeseriesStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setTimestamp(trace1.getTimestamp());
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analyticsService.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsHostName() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setTimestamp(trace1.getTimestamp());
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analyticsService.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        criteria = new Criteria();
        criteria.setHostName("hostB").setStartTime(0).setEndTime(0);

        // Get transaction count
        stats = analyticsService.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setTimestamp(trace1.getTimestamp());
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<NodeSummaryStatistics> stats = analyticsService.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsHostName() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType(Constants.COMPONENT_DATABASE);
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setTimestamp(trace1.getTimestamp());
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        Wait.until(() -> analyticsService.getNodeSummaryStatistics(null, criteria).size() == 2);
        Collection<NodeSummaryStatistics> stats = analyticsService.getNodeSummaryStatistics(null, criteria);
        assertNotNull(stats);
        assertEquals(2, stats.size());

        Criteria criteria2 = new Criteria();
        criteria2.setHostName("hostB").setStartTime(0).setEndTime(0);

        stats = analyticsService.getNodeSummaryStatistics(null, criteria2);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsFlat() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setTimestamp(trace1.getTimestamp());
        p1.setDuration(1000000);
        p1.addInteractionCorrelationId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setTraceId("1");
        trace2.setFragmentId("2");
        trace2.setTransaction(TESTAPP);
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(500000);
        c2.addInteractionCorrelationId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analyticsService.getCommunicationSummaryStatistics(null,
                criteria, false);

        assertNotNull(stats);
        assertEquals(2, stats.size());

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
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setTimestamp(trace1.getTimestamp());
        p1.setDuration(1000000);
        p1.addInteractionCorrelationId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setTraceId("1");
        trace2.setFragmentId("2");
        trace2.setTransaction(TESTAPP);
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(500000);
        c2.addInteractionCorrelationId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted, 10 seconds didn't seem enough :/
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analyticsService.getCommunicationSummaryStatistics(null,
                criteria, true);

        assertNotNull(stats);
        assertEquals(1, stats.size());

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
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Set<String> hosts = analyticsService.getHostNames(null, criteria);

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertTrue(hosts.contains("hostA"));
    }

    @Test
    public void testGetCompletionTimeMultiFragment() throws Exception {
        long baseTime=TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC;

        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTransaction(TESTAPP);
        trace1.setTimestamp(baseTime);

        Consumer c1 = new Consumer();
        c1.setUri("originuri2");
        c1.setDuration(1000000);
        c1.setTimestamp(1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setDuration(1000000);
        c1.setTimestamp(1);
        p1.addInteractionCorrelationId("interaction2");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setTraceId("1");
        trace2.setFragmentId("2");
        trace2.setTransaction(TESTAPP);
        trace2.setTimestamp(baseTime + 1000);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(500000);
        c2.setTimestamp(1);
        c2.addInteractionCorrelationId("interaction2");

        Component comp2 = new Component();
        comp2.setDuration(1500000);
        comp2.setTimestamp(1);
        c2.getNodes().add(comp2);

        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        assertEquals(1000000, trace1.calculateDuration());
        assertEquals(1500000, trace2.calculateDuration());

        tracePublisher.publish(null, traces);

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setTransaction(TESTAPP).setStartTime(0).setEndTime(0);

        // Get transaction count
        Wait.until(() -> analyticsService.getTraceCompletionTimeseriesStatistics(null, criteria, 10000).size() == 1);
        List<TimeseriesStatistics> stats = analyticsService
                .getTraceCompletionTimeseriesStatistics(null, criteria, 10000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1749999, stats.get(0).getAverage());
        assertEquals(1, stats.get(0).getCount());
    }

    @Test
    public void testGetEndpointResponseTimeseriesStatistics() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());

        Criteria criteria = new Criteria().setStartTime(0).setEndTime(0);

        // Get transaction count
        List<TimeseriesStatistics> stats = analyticsService
                .getEndpointResponseTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

}
