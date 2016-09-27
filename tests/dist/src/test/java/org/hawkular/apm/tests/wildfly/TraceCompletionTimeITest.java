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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.analytics.service.rest.client.AnalyticsServiceRESTClient;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceCompletionTimeITest extends AbstractITest {

    private static AnalyticsServiceRESTClient analyticsService;
    private static TraceServiceRESTClient traceService;
    private static TracePublisherRESTClient tracePublisher;

    @BeforeClass
    public static void initClass() {
        analyticsService = new AnalyticsServiceRESTClient();
        analyticsService.setUsername(HAWKULAR_APM_USERNAME);
        analyticsService.setPassword(HAWKULAR_APM_PASSWORD);

        traceService = new TraceServiceRESTClient();
        traceService.setUsername(HAWKULAR_APM_USERNAME);
        traceService.setPassword(HAWKULAR_APM_PASSWORD);

        tracePublisher = new TracePublisherRESTClient();
        tracePublisher.setUsername(HAWKULAR_APM_USERNAME);
        tracePublisher.setPassword(HAWKULAR_APM_PASSWORD);
    }

    @Before
    public void initTest() {
        analyticsService.clear(null);
        traceService.clear(null);
    }

    @Test
    public void testGetCompletionTimesSingleFragment() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(1234, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(1234, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesTwoFragmentInteractionP2PSync() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1_2ip2psync");
        trace1.setStartTime(System.currentTimeMillis() - 60000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(5000, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        p1.addInteractionCorrelationId("cid1_2ip2psync");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setId("2_2ip2psync");
        trace2.setStartTime(trace1.getStartTime() + 2000);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        c2.addInteractionCorrelationId("cid1_2ip2psync");
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesTwoFragmentInteractionP2PAsync() throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1_2ip2pasync");
        trace1.setStartTime(System.currentTimeMillis() - 60000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(3000, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.addInteractionCorrelationId("cid1_2ip2pasync");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setId("2_2ip2pasync");
        // Assuming no latency, so starts at the same time as the producer
        trace2.setStartTime(trace1.getStartTime() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        c2.addInteractionCorrelationId("cid1_2ip2pasync");
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(4500, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesThreeFragmentInteractionP2PSync() throws Exception {
        testGetCompletionTimesThreeFragmentP2PSync(Scope.Interaction, "3ip2psync");
    }

    @Test
    public void testGetCompletionTimesThreeFragmentControlFlowP2PSync() throws Exception {
        testGetCompletionTimesThreeFragmentP2PSync(Scope.ControlFlow, "3cfp2psync");
    }

    protected void testGetCompletionTimesThreeFragmentP2PSync(Scope scope, String suffix) throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1_"+suffix);
        trace1.setStartTime(System.currentTimeMillis() - 60000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(5000, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        p1.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setId("2_"+suffix);
        trace2.setStartTime(trace1.getStartTime() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MILLISECONDS));
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setUri("testuri3");
        p2.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p2.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        p2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        c2.getNodes().add(p2);

        Trace trace3 = new Trace();
        trace3.setId("3_"+suffix);
        trace3.setStartTime(trace1.getStartTime() + 1000);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        c3.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesThreeFragmentInteractionP2PAsync() throws Exception {
        testGetCompletionTimesThreeFragmentInteractionP2PAsync(Scope.Interaction, "3ip2pasync");
    }

    @Test
    public void testGetCompletionTimesThreeFragmentControlFlowP2PAsync() throws Exception {
        testGetCompletionTimesThreeFragmentInteractionP2PAsync(Scope.ControlFlow, "3cfp2pasync");
    }

    protected void testGetCompletionTimesThreeFragmentInteractionP2PAsync(Scope scope, String suffix) throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1_"+suffix);
        trace1.setStartTime(System.currentTimeMillis() - 60000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setId("2_"+suffix);
        trace2.setStartTime(trace1.getStartTime() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setUri("testuri3");
        p2.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p2.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        c2.getNodes().add(p2);

        Trace trace3 = new Trace();
        trace3.setId("3_"+suffix);
        trace3.setStartTime(trace2.getStartTime() + 500);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        c3.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesThreeFragmentInteractionMultiConsumerAsync() throws Exception {
        testGetCompletionTimesThreeFragmentMultiConsumerAsync(Scope.Interaction, "3imcasync");
    }

    @Test
    public void testGetCompletionTimesThreeFragmentControlFlowMultiConsumerAsync() throws Exception {
        testGetCompletionTimesThreeFragmentMultiConsumerAsync(Scope.ControlFlow, "3cfmcasync");
    }

    protected void testGetCompletionTimesThreeFragmentMultiConsumerAsync(Scope scope, String suffix) throws Exception {
        Trace trace1 = new Trace();
        trace1.setId("1_"+suffix);
        trace1.setStartTime(System.currentTimeMillis() - 60000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p1.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        p1.getDetails().put(Producer.DETAILS_PUBLISH, "true");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setId("2_"+suffix);
        trace2.setStartTime(trace1.getStartTime() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setUri("testuri3");
        p2.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p2.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        p2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        p2.getDetails().put(Producer.DETAILS_PUBLISH, "true");
        c2.getNodes().add(p2);

        Trace trace3 = new Trace();
        trace3.setId("3_"+suffix);
        trace3.setStartTime(trace2.getStartTime() + 500);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        c3.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1,
                15, TimeUnit.SECONDS);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesThreeFragmentCausedBy() throws Exception {
        String suffix="3cb";

        Trace trace1 = new Trace();
        trace1.setId("1_"+suffix);
        trace1.setStartTime(System.currentTimeMillis() - 60000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setUri("comp1");
        comp1.getProperties().add(new Property("prop1", "value1"));
        comp1.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        comp1.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        c1.getNodes().add(comp1);

        Trace trace2 = new Trace();
        trace2.setId("2_"+suffix);
        trace2.setStartTime(trace1.getStartTime() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS));
        c2.getProperties().add(new Property("prop2", "value2"));
        // Link back to the component 'comp1'
        c2.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, trace1.getId()+":0:0"));
        trace2.getNodes().add(c2);

        Component comp2 = new Component();
        comp2.setUri("comp2");
        comp2.setBaseTime(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        comp2.setDuration(TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS));
        c2.getNodes().add(comp2);

        Trace trace3 = new Trace();
        trace3.setId("3_"+suffix);
        trace3.setStartTime(trace2.getStartTime() + 500);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.getProperties().add(new Property("prop3", "value3"));
        c3.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        // Link back to the component 'comp2'
        c3.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, trace2.getId()+":0:0"));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1,
                15, TimeUnit.SECONDS);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
        assertEquals(3, times.get(0).getProperties().size());
    }

    @Test
    public void testGetCompletionTimesFragmentCausedBy() throws Exception {
        String suffix="3cb2";
        long startTime = System.currentTimeMillis();

        Trace trace1 = new Trace();
        trace1.setId("1_"+suffix);
        trace1.setStartTime(startTime - 60000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        c1.setDuration(TimeUnit.NANOSECONDS.convert(4000, TimeUnit.MILLISECONDS));
        c1.getProperties().add(new Property("prop1","value1"));
        trace1.getNodes().add(c1);
        Component comp1 = new Component();
        comp1.setUri("comp1");
        c1.getNodes().add(comp1);

        Trace trace2 = new Trace();
        trace2.setId("2_"+suffix);
        trace2.setStartTime(startTime - 40000);
        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        c2.setDuration(TimeUnit.NANOSECONDS.convert(15000, TimeUnit.MILLISECONDS));
        c2.getProperties().add(new Property("prop1","value1"));
        c2.getProperties().add(new Property("prop2","value2"));
        c2.addCausedByCorrelationId(trace1.getId()+":0:0");
        trace2.getNodes().add(c2);
        Component comp2 = new Component();
        comp2.setUri("comp2");
        c2.getNodes().add(comp2);

        Trace trace3 = new Trace();
        trace3.setId("3_"+suffix);
        trace3.setStartTime(startTime - 30000);
        Consumer c3 = new Consumer();
        c3.setUri("uri3");
        c3.setDuration(TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MILLISECONDS));
        c3.getProperties().add(new Property("prop3","value3"));
        c3.addCausedByCorrelationId(trace1.getId()+":0:0");
        trace3.getNodes().add(c3);
        Component comp3 = new Component();
        comp3.setUri("comp3");
        c3.getNodes().add(comp3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletionTimes(null, new Criteria()).size() == 1,
                15, TimeUnit.SECONDS);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletionTimes(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals((trace2.getStartTime() - trace1.getStartTime())
                + TimeUnit.MILLISECONDS.convert(c2.getDuration(), TimeUnit.NANOSECONDS), times.get(0).getDuration());
        assertEquals(3, times.get(0).getProperties().size());
    }

}
