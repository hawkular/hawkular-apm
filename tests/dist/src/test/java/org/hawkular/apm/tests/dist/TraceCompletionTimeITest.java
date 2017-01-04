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
    /**
     * Default Criteria returns results within one the last hour,
     * therefore subtracting this constant from current time.
     */
    private static final int FOUR_MS_IN_MICRO_SEC = 4000;

    private static AnalyticsServiceRESTClient analyticsService;
    private static TraceServiceRESTClient traceService;
    private static TracePublisherRESTClient tracePublisher;

    @BeforeClass
    public static void initClass() {
        analyticsService = new AnalyticsServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
        traceService = new TraceServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
        tracePublisher = new TracePublisherRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
    }

    @Before
    public void initTest() {
        analyticsService.clear(null);
        traceService.clear(null);
    }

    @Test
    public void testGetCompletionTimesSingleFragment() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1234);
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(1234, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesTwoFragmentInteractionP2PSync() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1_2ip2psync");
        trace1.setFragmentId("1_2ip2psync");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(5000);
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setTimestamp(500);
        p1.setDuration(4000);
        p1.addInteractionCorrelationId("cid1_2ip2psync");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setTraceId("1_2ip2psync");
        trace2.setFragmentId("2_2ip2psync");
        trace2.setTimestamp(trace1.getTimestamp() + 2000);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(1000);
        c2.addInteractionCorrelationId("cid1_2ip2psync");
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesTwoFragmentInteractionP2PAsync() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1_2ip2pasync");
        trace1.setFragmentId("1_2ip2pasync");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(3000);
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setTimestamp(trace1.getTimestamp() + 500);
        p1.setDuration(500);
        p1.addInteractionCorrelationId("cid1_2ip2pasync");
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setTraceId("1_2ip2pasync");
        trace2.setFragmentId("2_2ip2pasync");
        // Assuming no latency, so starts at the same time as the producer
        trace2.setTimestamp(trace1.getTimestamp() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(4000);
        c2.addInteractionCorrelationId("cid1_2ip2pasync");
        trace2.getNodes().add(c2);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 2);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

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
        trace1.setTraceId("1_"+suffix);
        trace1.setFragmentId("1_"+suffix);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(5000);
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setTimestamp(500);
        p1.setDuration(4000);
        p1.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setTraceId(trace1.getTraceId());
        trace2.setFragmentId("2_"+suffix);
        trace2.setTimestamp(trace1.getTimestamp() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(2000);
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setUri("testuri3");
        p2.setTimestamp(500);
        p2.setDuration(1000);
        p2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        c2.getNodes().add(p2);

        Trace trace3 = new Trace();
        trace3.setTraceId(trace1.getTraceId());
        trace3.setFragmentId("3_"+suffix);
        trace3.setTimestamp(trace1.getTimestamp() + 1000);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.setDuration(500);
        c3.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

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
        trace1.setTraceId("1_"+suffix);
        trace1.setFragmentId("1_"+suffix);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000);
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setTimestamp(trace1.getTimestamp() + 500);
        p1.setDuration(500);
        p1.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setTraceId(trace1.getTraceId());
        trace2.setFragmentId("2_"+suffix);
        trace2.setTimestamp(trace1.getTimestamp() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(1000);
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setUri("testuri3");
        p2.setTimestamp(trace2.getTimestamp() + 500);
        p2.setDuration(500);
        p2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        c2.getNodes().add(p2);

        Trace trace3 = new Trace();
        trace3.setTraceId(trace1.getTraceId());
        trace3.setFragmentId("3_"+suffix);
        trace3.setTimestamp(trace2.getTimestamp() + 500);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.setTimestamp(trace3.getTimestamp());
        c3.setDuration(4000);
        c3.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

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
        trace1.setTraceId("1_"+suffix);
        trace1.setFragmentId("1_"+suffix);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000);
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setTimestamp(trace1.getTimestamp() + 500);
        p1.setDuration(500);
        p1.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        p1.getProperties().add(new Property(Producer.PROPERTY_PUBLISH, "true"));
        c1.getNodes().add(p1);

        Trace trace2 = new Trace();
        trace2.setTraceId(trace1.getTraceId());
        trace2.setFragmentId("2_"+suffix);
        trace2.setTimestamp(trace1.getTimestamp() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(1000);
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid1_"+suffix));
        trace2.getNodes().add(c2);

        Producer p2 = new Producer();
        p2.setUri("testuri3");
        p2.setTimestamp(trace2.getTimestamp() + 500);
        p2.setDuration(500);
        p2.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        p2.getProperties().add(new Property(Producer.PROPERTY_PUBLISH, "true"));
        c2.getNodes().add(p2);

        Trace trace3 = new Trace();
        trace3.setTraceId(trace1.getTraceId());
        trace3.setFragmentId("3_"+suffix);
        trace3.setTimestamp(trace2.getTimestamp() + 500);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.setTimestamp(trace3.getTimestamp());
        c3.setDuration(4000);
        c3.getCorrelationIds().add(new CorrelationIdentifier(scope, "cid2_"+suffix));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1,
                15, TimeUnit.SECONDS);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
    }

    @Test
    public void testGetCompletionTimesThreeFragmentCausedBy() throws Exception {
        String suffix="3cb";

        Trace trace1 = new Trace();
        trace1.setTraceId("1_"+suffix);
        trace1.setFragmentId("1_"+suffix);
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(1000);
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setUri("comp1");
        comp1.getProperties().add(new Property("prop1", "value1"));
        comp1.setTimestamp(trace1.getTimestamp() + 500);
        comp1.setDuration(500);
        c1.getNodes().add(comp1);

        Trace trace2 = new Trace();
        trace2.setTraceId(trace1.getTraceId());
        trace2.setFragmentId("2_"+suffix);
        trace2.setTimestamp(trace1.getTimestamp() + 500);

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(1000);
        c2.getProperties().add(new Property("prop2", "value2"));
        // Link back to the component 'comp1'
        c2.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, trace1.getFragmentId()+":0:0"));
        trace2.getNodes().add(c2);

        Component comp2 = new Component();
        comp2.setUri("comp2");
        comp2.setTimestamp(trace2.getTimestamp() + 500);
        comp2.setDuration(500);
        c2.getNodes().add(comp2);

        Trace trace3 = new Trace();
        trace3.setTraceId(trace1.getTraceId());
        trace3.setFragmentId("3_"+suffix);
        trace3.setTimestamp(trace2.getTimestamp() + 500);

        Consumer c3 = new Consumer();
        c3.setUri("testuri3");
        c3.getProperties().add(new Property("prop3", "value3"));
        c3.setTimestamp(trace3.getTimestamp());
        c3.setDuration(4000);
        // Link back to the component 'comp2'
        c3.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, trace2.getFragmentId()+":0:0"));
        trace3.getNodes().add(c3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1,
                15, TimeUnit.SECONDS);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals(5000, times.get(0).getDuration());
        assertEquals(3, times.get(0).getProperties().size());
    }

    @Test
    public void testGetCompletionTimesFragmentCausedBy() throws Exception {
        String suffix="3cb2";
        long startTime = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());

        Trace trace1 = new Trace();
        trace1.setTraceId("1_"+suffix);
        trace1.setFragmentId("1_"+suffix);
        trace1.setTimestamp(startTime - 60000);
        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        c1.setTimestamp(trace1.getTimestamp());
        c1.setDuration(4000);
        c1.getProperties().add(new Property("prop1","value1"));
        trace1.getNodes().add(c1);
        Component comp1 = new Component();
        comp1.setUri("comp1");
        comp1.setTimestamp(trace1.getTimestamp());
        c1.getNodes().add(comp1);

        Trace trace2 = new Trace();
        trace2.setTraceId(trace1.getTraceId());
        trace2.setFragmentId("2_"+suffix);
        trace2.setTimestamp(startTime - 40000);
        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        c2.setTimestamp(trace2.getTimestamp());
        c2.setDuration(15000);
        c2.getProperties().add(new Property("prop1","value1"));
        c2.getProperties().add(new Property("prop2","value2"));
        c2.addCausedByCorrelationId(trace1.getFragmentId()+":0:0");
        trace2.getNodes().add(c2);
        Component comp2 = new Component();
        comp2.setUri("comp2");
        comp2.setTimestamp(trace2.getTimestamp());
        c2.getNodes().add(comp2);

        Trace trace3 = new Trace();
        trace3.setTraceId(trace1.getTraceId());
        trace3.setFragmentId("3_"+suffix);
        trace3.setTimestamp(startTime - 30000);
        Consumer c3 = new Consumer();
        c3.setUri("uri3");
        c3.setTimestamp(trace3.getTimestamp());
        c3.setDuration(2000);
        c3.getProperties().add(new Property("prop3","value3"));
        c3.addCausedByCorrelationId(trace1.getFragmentId()+":0:0");
        trace3.getNodes().add(c3);
        Component comp3 = new Component();
        comp3.setUri("comp3");
        comp3.setTimestamp(trace3.getTimestamp());
        c3.getNodes().add(comp3);

        tracePublisher.publish(null, Arrays.asList(trace1, trace2, trace3));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 3);

        // Wait to result derived
        Wait.until(() -> analyticsService.getTraceCompletions(null, new Criteria()).size() == 1,
                15, TimeUnit.SECONDS);

        // Get trace completion times
        List<CompletionTime> times = analyticsService.getTraceCompletions(null, new Criteria());

        assertNotNull(times);
        assertEquals(1, times.size());
        assertEquals((trace2.getTimestamp() - trace1.getTimestamp()) + c2.getDuration(), times.get(0).getDuration());
        assertEquals(3, times.get(0).getProperties().size());
    }
}
