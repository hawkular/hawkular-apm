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
package org.hawkular.apm.api.model.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TraceTest {

    private static final String VALUE1 = "Value1";
    private static final String HEADER1 = "Header1";
    private static final String VALUE2 = "Value2";
    private static final String HEADER2 = "Header2";

    private static final String TEST_VALUE1 = "TestValue1";
    private static final String TEST_PROP1 = "TestProp1";
    private static final String TEST_VALUE2 = "TestValue2";
    private static final String TEST_PROP2 = "TestProp2";

    @Test
    public void testStartTime() {
        Trace trace = new Trace();
        trace.setTimestamp(100);

        Consumer node1 = new Consumer();
        node1.setTimestamp(110);
        trace.getNodes().add(node1);

        assertEquals("Start time incorrect", 100L, trace.getTimestamp());
    }

    @Test
    public void testEndTime() {
        Trace trace = new Trace();
        trace.setTimestamp(100000);

        Consumer node1 = new Consumer();
        node1.setTimestamp(100000);
        trace.getNodes().add(node1);

        Component node2 = new Component();
        node2.setTimestamp(150000);
        node2.setDuration(0);
        node1.getNodes().add(node2);

        // This node will have the latest time associated with the
        // transaction, comprised of the start time + duration
        Producer node3 = new Producer();
        node3.setTimestamp(200000);
        node3.setDuration(50000);
        node1.getNodes().add(node3);

        assertEquals(250000, trace.endTime());
    }

    @Test
    public void testSerialize() {
        Trace trace = example1();

        // Serialize
        ObjectMapper mapper = new ObjectMapper();

        try {
            mapper.writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Failed to serialize");
        }
    }

    @Test
    public void testEqualityAfterDeserialization() {
        Trace trace = example1();

        // Serialize
        ObjectMapper mapper = new ObjectMapper();
        String json = null;

        try {
            json = mapper.writeValueAsString(trace);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Failed to serialize");
        }

        assertNotNull(json);

        Trace trace2 = null;

        try {
            trace2 = mapper.readValue(json.getBytes(), Trace.class);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to deserialize");
        }

        assertNotNull(trace2);

        assertEquals(trace, trace2);
    }

    @Test
    public void testInitialFragmentTrue() {
        Trace trace = new Trace().setTraceId("1").setFragmentId("1");

        assertTrue(trace.initialFragment());
    }

    @Test
    public void testInitialFragmentFalse() {
        Trace trace = new Trace().setTraceId("1").setFragmentId("2");
        assertFalse(trace.initialFragment());
    }

    protected Trace example1() {
        // Business transaction
        Trace trace = new Trace();

        // Top level (consumer) node
        Consumer c1 = new Consumer();
        trace.getNodes().add(c1);

        c1.getProperties().add(new Property(TEST_PROP1, TEST_VALUE1));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "CID1"));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, "CID2"));
        c1.setDuration(1000);
        c1.setTimestamp(1);
        c1.setEndpointType("JMS");
        c1.setUri("queue:test");

        Message req1 = new Message();
        req1.getHeaders().put(HEADER1, VALUE1);
        req1.addContent("all", null, "Parameter1");

        c1.setIn(req1);

        Message resp1 = new Message();
        resp1.getHeaders().put(HEADER2, VALUE2);
        resp1.addContent("all", null, "Parameter2");

        c1.setOut(resp1);

        // Second level (component) node
        Component s1 = new Component();
        c1.getNodes().add(s1);

        s1.getProperties().add(new Property(TEST_PROP2, TEST_VALUE2));
        s1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "CID1"));
        s1.setDuration(900);
        s1.setTimestamp(2);
        s1.setOperation("Op1");
        s1.setUri("ServiceType1");

        // Third level (component) node
        Component cp1 = new Component();
        s1.getNodes().add(cp1);

        cp1.setDuration(400);
        cp1.setTimestamp(3);
        cp1.setUri("jdbc:TestDB");
        cp1.setComponentType(Constants.COMPONENT_DATABASE);
        cp1.setOperation("select X from Y");

        // Third level (component) node - this represents the service proxy
        // used by the consumer service
        Component s2 = new Component();
        s1.getNodes().add(s2);

        s2.getCorrelationIds().add(new CorrelationIdentifier(Scope.ControlFlow, "CID3"));
        s2.setDuration(500);
        s2.setTimestamp(3);
        s2.setOperation("Op2");
        s2.setUri("ServiceType2");

        // Fourth level (producer) node
        Producer p1 = new Producer();
        s2.getNodes().add(p1);

        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.ControlFlow, "CID3"));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.CausedBy, "CID4"));
        c1.setDuration(400);
        c1.setTimestamp(4);
        c1.setEndpointType("HTTP");
        c1.setUri("http://example.com/service");

        Message req5 = new Message();
        req5.getHeaders().put(HEADER1, VALUE1);
        req5.addContent("all", null, "Parameter5");

        p1.setIn(req5);

        Message resp5 = new Message();
        resp5.getHeaders().put(HEADER2, VALUE2);
        resp5.addContent("all", null, "Parameter6");

        p1.setOut(resp5);

        return (trace);
    }

    @Test
    public void testCalculateDuration() {
        long baseTime = TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis());

        Trace trace1 = new Trace();
        trace1.setFragmentId("1");
        trace1.setTransaction("testapp");
        trace1.setTimestamp(baseTime); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setDuration(1000000);
        c1.setTimestamp(100000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setDuration(1000000);
        p1.setTimestamp(500000);
        p1.addInteractionCorrelationId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        long duration = trace1.calculateDuration();

        assertEquals(1400000, duration);
    }

    @Test
    public void testAllProperties() {
        Trace trace1 = new Trace();
        trace1.setFragmentId("1");
        trace1.setTransaction("testapp");

        Consumer c1 = new Consumer();
        c1.getProperties().add(new Property("prop1", "value1"));
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.getProperties().add(new Property("prop2", "value2"));
        c1.getNodes().add(p1);

        Component ct1 = new Component();
        ct1.getProperties().add(new Property("prop3", "value3"));
        c1.getNodes().add(ct1);

        assertEquals(3, trace1.allProperties().size());
    }

}
