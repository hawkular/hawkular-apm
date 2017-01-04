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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.tests.common.Wait;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author gbrown
 */
public class TraceServiceElasticsearchTest {

    private TraceServiceElasticsearch ts;

    @BeforeClass
    public static void initClass() {
        System.setProperty("HAWKULAR_APM_CONFIG_DIR", "target");
    }

    @Before
    public void beforeTest() {
        ts = new TraceServiceElasticsearch();
    }

    @After
    public void afterTest() {
        ts.clear(null);
    }

    @Test
    public void testQueryBTxnName() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTransaction("trace1");
        trace1.setTimestamp(1000);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTransaction("trace2");
        trace2.setTimestamp(2000);

        Trace trace3 = new Trace();
        trace3.setFragmentId("id3");
        trace3.setTimestamp(3000);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.setTransaction("trace1");

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getFragmentId());
        assertEquals("trace1", result1.get(0).getTransaction());
    }

    @Test
    public void testQueryNoBTxnName() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTransaction("trace1");
        trace1.setTimestamp(1000);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTransaction("trace2");
        trace2.setTimestamp(2000);

        Trace trace3 = new Trace();
        trace3.setFragmentId("id3");
        trace3.setTimestamp(3000);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.setTransaction("");

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id3", result1.get(0).getFragmentId());
        assertNull(result1.get(0).getTransaction());
    }

    @Test
    public void testSearchFragments() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTimestamp(1000);

        Consumer consumer1 = new Consumer();
        consumer1.getProperties().add(new Property("prop1a", "value1a"));
        consumer1.getProperties().add(new Property("prop1b", "value1b"));
        trace1.getNodes().add(consumer1);

        Component component1 = new Component();
        component1.getProperties().add(new Property("prop2", "value2"));
        consumer1.getNodes().add(component1);

        Producer producer1 = new Producer();
        producer1.getProperties().add(new Property("prop3", "value3"));
        consumer1.getNodes().add(producer1);

        ts.storeFragments(null, Arrays.asList(trace1));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals(trace1, result1.get(0));
    }

    @Test
    public void testQuerySinglePropertyAndValueIncluded() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTimestamp(1000);

        Consumer consumer1 = new Consumer();
        consumer1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(consumer1);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTimestamp(2000);

        Consumer consumer2 = new Consumer();
        consumer2.getProperties().add(new Property("prop2", "value2"));
        trace2.getNodes().add(consumer2);

        Trace trace3 = new Trace();
        trace3.setFragmentId("id3");
        trace3.setTimestamp(3000);

        Consumer consumer3 = new Consumer();
        consumer3.getProperties().add(new Property("prop1", "value3"));
        trace3.getNodes().add(consumer3);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.addProperty("prop1", "value1", null);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getFragmentId());
    }

    @Test
    public void testQuerySinglePropertyAndValueExcluded() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTimestamp(1000);
        Consumer consumer1 = new Consumer();
        consumer1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(consumer1);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTimestamp(2000);
        Consumer consumer2 = new Consumer();
        consumer2.getProperties().add(new Property("prop2", "value2"));
        trace2.getNodes().add(consumer2);

        Trace trace3 = new Trace();
        trace3.setFragmentId("id3");
        trace3.setTimestamp(3000);
        Consumer consumer3 = new Consumer();
        consumer3.getProperties().add(new Property("prop1", "value3"));
        trace3.getNodes().add(consumer3);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.addProperty("prop1", "value1", Operator.HASNOT);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 2);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertTrue((result1.get(0).getFragmentId().equals("id2") && result1.get(1).getFragmentId().equals("id3"))
                || (result1.get(0).getFragmentId().equals("id3") && result1.get(1).getFragmentId().equals("id2")));
    }

    @Test
    public void testQuerySinglePropertyAndMultiValueIncluded() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTimestamp(1000);
        Consumer consumer1 = new Consumer();
        consumer1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(consumer1);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTimestamp(2000);
        Consumer consumer2 = new Consumer();
        consumer2.getProperties().add(new Property("prop2", "value2"));
        trace2.getNodes().add(consumer2);

        Trace trace3 = new Trace();
        trace3.setFragmentId("id3");
        trace3.setTimestamp(3000);
        Consumer consumer3 = new Consumer();
        consumer3.getProperties().add(new Property("prop3", "value3"));
        trace3.getNodes().add(consumer3);

        Trace trace4 = new Trace();
        trace4.setFragmentId("id4");
        trace4.setTimestamp(4000);
        Consumer consumer4 = new Consumer();
        consumer4.getProperties().add(new Property("prop1", "value1"));
        consumer4.getProperties().add(new Property("prop3", "value3"));
        trace4.getNodes().add(consumer4);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3, trace4));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.addProperty("prop1", "value1", null);
        criteria.addProperty("prop3", "value3", null);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id4", result1.get(0).getFragmentId());
    }

    @Test
    public void testQuerySinglePropertyAndMultiValueExcluded() throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTimestamp(1000);
        Consumer consumer1 = new Consumer();
        consumer1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(consumer1);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTimestamp(2000);
        Consumer consumer2 = new Consumer();
        consumer2.getProperties().add(new Property("prop2", "value2"));
        trace2.getNodes().add(consumer2);

        Trace trace3 = new Trace();
        trace3.setFragmentId("id3");
        trace3.setTimestamp(3000);
        Consumer consumer3 = new Consumer();
        consumer3.getProperties().add(new Property("prop1", "value3"));
        trace3.getNodes().add(consumer3);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.addProperty("prop1", "value1", Operator.HASNOT);
        criteria.addProperty("prop1", "value3", Operator.HASNOT);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id2", result1.get(0).getFragmentId());
    }

    @Test
    public void testQueryInteractionCorrelationId() throws StoreException {
        testQueryCorrelationId(Scope.Interaction);
    }

    @Test
    public void testQueryControlFlowCorrelationId() throws StoreException {
        testQueryCorrelationId(Scope.ControlFlow);
    }

    protected void testQueryCorrelationId(Scope scope) throws StoreException {
        Trace trace1 = new Trace();
        trace1.setFragmentId("id1");
        trace1.setTimestamp(1000);

        Consumer c1=new Consumer();
        c1.getCorrelationIds().add(new CorrelationIdentifier(scope, "gid1"));
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setFragmentId("id2");
        trace2.setTimestamp(2000);

        Consumer c2=new Consumer();
        c2.getCorrelationIds().add(new CorrelationIdentifier(scope, "gid2"));
        trace2.getNodes().add(c2);

        ts.storeFragments(null, Arrays.asList(trace1, trace2));

        Criteria criteria = new Criteria();
        criteria.setStartTime(1);
        criteria.getCorrelationIds().add(new CorrelationIdentifier(scope, "gid1"));

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getFragmentId());
    }

    @Test
    public void testStoreAndRetrieveInteractionTraceById() throws StoreException, JsonProcessingException {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
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
        p1_2.setEndpointType("HTTP");
        c1.getNodes().add(p1_2);

        Trace trace2 = new Trace();
        trace2.setTraceId("1");
        trace2.setFragmentId("2");
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        c2.setEndpointType("HTTP");
        c2.getProperties().add(new Property("prop1","value1"));
        c2.getProperties().add(new Property("prop2","value2"));
        c2.addInteractionCorrelationId("id1_2");
        trace2.getNodes().add(c2);
        Producer p2_1 = new Producer();
        p2_1.addInteractionCorrelationId("id2_1");
        c2.getNodes().add(p2_1);
        Producer p2_2 = new Producer();
        p2_2.addInteractionCorrelationId("id2_2");
        c2.getNodes().add(p2_2);

        ts.storeFragments(null, Arrays.asList(trace1, trace2));

        // Retrieve stored trace
        Wait.until(() -> ts.getTrace(null, "1") != null);
        Trace result = ts.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());

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
    public void testStoreAndRetrieveCausedByTraceById() throws StoreException, JsonProcessingException {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        c1.getProperties().add(new Property("prop1","value1"));
        trace1.getNodes().add(c1);
        Component comp1 = new Component();
        comp1.setUri("comp1");
        c1.getNodes().add(comp1);

        Trace trace2 = new Trace();
        trace2.setTraceId("1");
        trace2.setFragmentId("2");
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        c2.getProperties().add(new Property("prop1","value1"));
        c2.getProperties().add(new Property("prop2","value2"));
        c2.addCausedByCorrelationId(trace1.getFragmentId()+":0:0");
        trace2.getNodes().add(c2);
        Component comp2 = new Component();
        comp2.setUri("comp2");
        c2.getNodes().add(comp2);

        Trace trace3 = new Trace();
        trace3.setTraceId("1");
        trace3.setFragmentId("3");
        trace3.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c3 = new Consumer();
        c3.setUri("uri3");
        c3.getProperties().add(new Property("prop3","value3"));
        c3.addCausedByCorrelationId(trace1.getFragmentId()+":0:0");
        trace3.getNodes().add(c3);
        Component comp3 = new Component();
        comp3.setUri("comp3");
        c3.getNodes().add(comp3);

        ts.storeFragments(null, Arrays.asList(trace1, trace2, trace3));

        // Retrieve stored trace
        Wait.until(() -> ts.getTrace(null, "1") != null);
        Trace result = ts.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());

        assertEquals(3, result.allProperties().size());
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());

        Consumer resultconsumer1 = (Consumer) result.getNodes().get(0);

        assertEquals("uri1", resultconsumer1.getUri());
        assertEquals(1, resultconsumer1.getNodes().size());
        assertEquals(Component.class, resultconsumer1.getNodes().get(0).getClass());

        Component resultcomp1 = (Component)resultconsumer1.getNodes().get(0);

        assertEquals(2, resultcomp1.getNodes().size());
        assertEquals(Producer.class, resultcomp1.getNodes().get(0).getClass());

        Producer resultproducer1 = (Producer)resultcomp1.getNodes().get(0);

        assertEquals(1, resultproducer1.getNodes().size());
        assertEquals(Consumer.class, resultproducer1.getNodes().get(0).getClass());

        Consumer resultconsumer2 = (Consumer) resultproducer1.getNodes().get(0);

        assertEquals("uri2", resultconsumer2.getUri());
        assertEquals(1, resultconsumer2.getNodes().size());
        assertEquals(Component.class, resultconsumer2.getNodes().get(0).getClass());

        Component resultcomp2 = (Component)resultconsumer2.getNodes().get(0);

        assertTrue(resultcomp2.getNodes().isEmpty());

        assertEquals(Producer.class, resultcomp1.getNodes().get(1).getClass());

        Producer resultproducer2 = (Producer)resultcomp1.getNodes().get(1);

        assertEquals(Consumer.class, resultproducer2.getNodes().get(0).getClass());

        Consumer resultconsumer3 = (Consumer) resultproducer2.getNodes().get(0);

        assertEquals("uri3", resultconsumer3.getUri());
        assertEquals(1, resultconsumer3.getNodes().size());
        assertEquals(Component.class, resultconsumer3.getNodes().get(0).getClass());

        Component resultcomp3 = (Component)resultconsumer3.getNodes().get(0);

        assertTrue(resultcomp3.getNodes().isEmpty());
    }

    @Test
    public void testGetTraceByIdJustProducerConsumer() throws StoreException, JsonProcessingException {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Producer producer = new Producer();
        producer.setUri("uri");
        producer.addInteractionCorrelationId("id1");
        trace1.getNodes().add(producer);

        Trace trace2 = new Trace();
        trace2.setTraceId("1");
        trace2.setFragmentId("2");
        trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer consumer = new Consumer();
        consumer.setUri("uri");
        consumer.addInteractionCorrelationId("id1");
        trace2.getNodes().add(consumer);

        ts.storeFragments(null, Arrays.asList(trace1, trace2));

        // Retrieve stored trace
        Wait.until(() -> ts.getTrace(null, "1") != null);
        Trace result = ts.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());

        assertEquals(1, result.getNodes().size());
        assertEquals(Producer.class, result.getNodes().get(0).getClass());
        assertEquals(1, ((Producer)result.getNodes().get(0)).getNodes().size());
        assertEquals(Consumer.class, ((Producer)result.getNodes().get(0)).getNodes().get(0).getClass());
        assertTrue(((Consumer)((Producer)result.getNodes().get(0)).getNodes().get(0)).getNodes().isEmpty());
    }

    @Test
    public void testStoreAndRetrieveComplexTraceById() throws Exception {
        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
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
        trace2.setTraceId(trace1.getTraceId());
        trace2.setFragmentId("2");
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

        ts.storeFragments(null, Arrays.asList(trace1, trace2));

        // Retrieve stored trace
        Wait.until(() -> ts.getTrace(null, "1") != null);
        Trace result = ts.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());

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
    public void testGetTraceWithMoreThan10Fragments() throws StoreException, JsonProcessingException {
        List<Trace> traces = new ArrayList<>();

        Trace trace1 = new Trace();
        trace1.setTraceId("1");
        trace1.setFragmentId("1");
        trace1.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);
        Component comp1 = new Component();
        c1.getNodes().add(comp1);
        traces.add(trace1);

        int childNumber = 20;

        for (int i=0; i < childNumber; i++) {
            Trace trace2 = new Trace();
            trace2.setTraceId("1");
            trace2.setFragmentId("2-"+i);
            trace2.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));
            Consumer c2 = new Consumer();
            c2.addCausedByCorrelationId(trace1.getFragmentId()+":0:0");
            trace2.getNodes().add(c2);
            traces.add(trace2);
        }

        ts.storeFragments(null, traces);

        // Retrieve stored trace
        Wait.until(() -> ts.getTrace(null, "1") != null);
        Trace result = ts.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getFragmentId());

        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());

        Consumer resultconsumer1 = (Consumer) result.getNodes().get(0);

        assertEquals(1, resultconsumer1.getNodes().size());
        assertEquals(Component.class, resultconsumer1.getNodes().get(0).getClass());

        Component resultcomp1 = (Component)resultconsumer1.getNodes().get(0);

        assertEquals(childNumber, resultcomp1.getNodes().size());
    }

}
