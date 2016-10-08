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
package org.hawkular.apm.tests.dist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceServiceITest extends AbstractITest {
    /**
     * Default Criteria returns results within one the last hour,
     * therefore subtracting this constant from current time.
     */
    private static final int FOUR_MS_IN_MICRO_SEC = 4000;

    private static TraceServiceRESTClient traceService;
    private static TracePublisherRESTClient tracePublisher;

    @BeforeClass
    public static void initClass() {
        traceService = new TraceServiceRESTClient();
        traceService.setUsername(HAWKULAR_APM_USERNAME);
        traceService.setPassword(HAWKULAR_APM_PASSWORD);

        tracePublisher = new TracePublisherRESTClient();
        tracePublisher.setUsername(HAWKULAR_APM_USERNAME);
        tracePublisher.setPassword(HAWKULAR_APM_PASSWORD);
    }

    @AfterClass
    public static void cleanupClass() throws InterruptedException {
        // Crude solution to prevent derived 'trace completion times' from these tests affecting
        // other integration tests
        synchronized (traceService) {
            traceService.wait(10000);
        }
    }

    @Before
    public void initTest() {
        traceService.clear(null);
    }

    @Test
    public void testStoreAndRetrieveFragmentById() {
        Trace trace1 = new Trace();
        trace1.setId("1");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Wait.until(() -> traceService.getFragment(null, "1") != null);

        // Retrieve stored trace
        Trace result = traceService.getFragment(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void testStoreAndRetrieveSimpleTraceById() {
        Trace trace1 = new Trace();
        trace1.setId("1");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Wait.until(() -> traceService.getTrace(null, "1") != null);

        // Retrieve stored trace
        Trace result = traceService.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void testStoreAndRetrieveComplexTraceById() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        c1.getProperties().add(new Property("prop1","value1"));
        trace1.getNodes().add(c1);
        Producer p1_1 = new Producer();
        p1_1.addInteractionCorrelationId("id1_1");
        c1.getNodes().add(p1_1);
        Producer p1_2 = new Producer();
        p1_2.addInteractionCorrelationId("id1_2");
        p1_2.setUri("uri2");
        c1.getNodes().add(p1_2);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        c2.addInteractionCorrelationId("id1_2");
        c2.getProperties().add(new Property("prop1","value1"));
        c2.getProperties().add(new Property("prop2","value2"));
        trace2.getNodes().add(c2);
        Producer p2_1 = new Producer();
        p2_1.addInteractionCorrelationId("id2_1");
        c2.getNodes().add(p2_1);
        Producer p2_2 = new Producer();
        p2_2.addInteractionCorrelationId("id2_2");
        c2.getNodes().add(p2_2);

        try {
            tracePublisher.publish(null, Arrays.asList(trace1, trace2));
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        Wait.until(() -> {
            try {
                // see https://issues.jboss.org/browse/HWKAPM-584
                Trace t = traceService.getTrace(null, "1");
                return t != null;
            } catch (Throwable t) {
                return false;
            }
        });

        // Retrieve stored trace
        Trace result = traceService.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());

        assertEquals(2, result.allProperties().size());
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());
        assertEquals("uri1", result.getNodes().get(0).getUri());
        assertEquals(2, ((Consumer)result.getNodes().get(0)).getNodes().size());
        assertEquals(Producer.class, ((Consumer)result.getNodes().get(0)).getNodes().get(0).getClass());
        assertTrue(((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(0)).getNodes().isEmpty());
        assertEquals(Producer.class, ((Consumer)result.getNodes().get(0)).getNodes().get(1).getClass());
        assertEquals("uri2", ((Consumer)result.getNodes().get(0)).getNodes().get(1).getUri());
        assertEquals(1, ((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(1)).getNodes().size());
        assertEquals(Consumer.class, ((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(1)).getNodes()
                .get(0).getClass());
        assertEquals("uri2", ((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(1)).getNodes()
                .get(0).getUri());
    }

    @Test
    public void testStoreAndQueryAll() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        List<Trace> traces = new ArrayList<>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryStartTimeInclude() {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);

        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryStartTimeExclude() {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        criteria.setStartTime(1100);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryEndTimeInclude() {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(2000);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryEndTimeExclude() {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1200000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(1500);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        criteria.setEndTime(1100);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryPropertiesInclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("hello", "world"));
        trace1.getNodes().add(c1);

        try {
            tracePublisher.publish(null, Collections.singletonList(trace1));
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", null);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryPropertiesNotFound() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("hello", "world"));
        trace1.getNodes().add(c1);

        try {
            tracePublisher.publish(null, Collections.singletonList(trace1));
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteriaToWait = new Criteria();
        criteriaToWait.addProperty("hello", "world", null);
        Wait.until(() -> traceService.searchFragments(null, criteriaToWait).size() > 0, 1, TimeUnit.SECONDS);

        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "fred", null);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryPropertiesExclude() {
        traceService.clear(null);
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("hello", "world"));
        trace1.getNodes().add(c1);

        try {
            tracePublisher.publish(null, Collections.singletonList(trace1));
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Criteria criteriaToWait = new Criteria();
        criteriaToWait.addProperty("hello", "world", null);
        Wait.until(() -> traceService.searchFragments(null, criteriaToWait).size() == 1);

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", Operator.HASNOT);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryCorrelationsInclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.ControlFlow);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.ControlFlow, "myid"));
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryCorrelationsExclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Interaction);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            tracePublisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Criteria criteriaToWait = new Criteria();
        criteriaToWait.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "myid"));
        Wait.until(() -> traceService.searchFragments(null, criteriaToWait).size() == 1);

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "notmyid"));

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }
}
