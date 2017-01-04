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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
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
        traceService = new TraceServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
        tracePublisher = new TracePublisherRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
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
    public void testStoreAndRetrieveFragmentById() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");

        tracePublisher.publish(null, Collections.singletonList(trace1));

        Wait.until(() -> traceService.getFragment(null, "1") != null);

        // Retrieve stored trace
        Trace result = traceService.getFragment(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());
    }

    @Test
    public void testStoreAndRetrieveSimpleTraceById() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");

        tracePublisher.publish(null, Collections.singletonList(trace1));

        Wait.until(() -> traceService.getTrace(null, "1") != null);

        // Retrieve stored trace
        Trace result = traceService.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());
    }

    @Test
    public void testStoreAndQueryAll() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Wait to ensure record persisted
        Wait.until(() -> traceService.searchFragments(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = traceService.searchFragments(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());
    }

    @Test
    public void testStoreAndQueryStartTimeInclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000000);
        trace1.setTraceId("1");
        trace1.setFragmentId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);

        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());
    }

    @Test
    public void testStoreAndQueryStartTimeExclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000000);
        trace1.setTraceId("1");
        trace1.setFragmentId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        criteria.setStartTime(1100);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryEndTimeInclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1000000);
        trace1.setTraceId("1");
        trace1.setFragmentId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(2000);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());
    }

    @Test
    public void testStoreAndQueryEndTimeExclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTimestamp(1200000);
        trace1.setTraceId("1");
        trace1.setFragmentId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(1500);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        criteria.setEndTime(1100);
        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryPropertiesInclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("hello", "world"));
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", null);
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());
    }

    @Test
    public void testStoreAndQueryPropertiesNotFound() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("hello", "world"));
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

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
    public void testStoreAndQueryPropertiesExclude() throws Exception {
        traceService.clear(null);
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("hello", "world"));
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

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
    public void testStoreAndQueryCorrelationsInclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.ControlFlow);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.ControlFlow, "myid"));
        Wait.until(() -> traceService.searchFragments(null, criteria).size() == 1);

        List<Trace> result = traceService.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getFragmentId());
    }

    @Test
    public void testStoreAndQueryCorrelationsExclude() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()) - FOUR_MS_IN_MICRO_SEC);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Interaction);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        tracePublisher.publish(null, Collections.singletonList(trace1));

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
