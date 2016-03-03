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
package org.hawkular.btm.api.model.btxn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BusinessTransactionTest {

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
        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setStartTime(100);

        Consumer node1 = new Consumer();
        node1.setBaseTime(110);
        btxn.getNodes().add(node1);

        assertEquals("Start time incorrect", 100L, btxn.getStartTime());
    }

    @Test
    public void testEndTime() {
        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setStartTime(100);

        Consumer node1 = new Consumer();
        node1.setBaseTime(100000000);
        btxn.getNodes().add(node1);

        Component node2 = new Component();
        node2.setBaseTime(150000000);
        node2.setDuration(0);
        node1.getNodes().add(node2);

        // This node will have the latest time associated with the
        // business transaction, comprised of the start time + duration
        Producer node3 = new Producer();
        node3.setBaseTime(200000000);
        node3.setDuration(50000000);
        node1.getNodes().add(node3);

        assertEquals("End time incorrect", 250L, btxn.endTime());
    }

    @Test
    public void testSerialize() {
        BusinessTransaction btxn = example1();

        // Serialize
        ObjectMapper mapper = new ObjectMapper();

        try {
            mapper.writeValueAsString(btxn);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Failed to serialize");
        }
    }

    @Test
    public void testEqualityAfterDeserialization() {
        BusinessTransaction btxn = example1();

        // Serialize
        ObjectMapper mapper = new ObjectMapper();
        String json = null;

        try {
            json = mapper.writeValueAsString(btxn);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Failed to serialize");
        }

        assertNotNull(json);

        BusinessTransaction btxn2 = null;

        try {
            btxn2 = mapper.readValue(json.getBytes(), BusinessTransaction.class);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to deserialize");
        }

        assertNotNull(btxn2);

        assertEquals(btxn, btxn2);
    }

    @Test
    public void testInitialFragmentTrue() {
        BusinessTransaction btxn = new BusinessTransaction();

        Consumer c1 = new Consumer();
        c1.addGlobalId("gid");
        btxn.getNodes().add(c1);

        assertTrue(btxn.initialFragment());
    }

    @Test
    public void testInitialFragmentFalse() {
        BusinessTransaction btxn = new BusinessTransaction();

        Consumer c1 = new Consumer();
        c1.addGlobalId("gid");
        c1.addInteractionId("myid");
        btxn.getNodes().add(c1);

        assertFalse(btxn.initialFragment());
    }

    protected BusinessTransaction example1() {
        // Business transaction
        BusinessTransaction btxn = new BusinessTransaction();

        btxn.getProperties().put(TEST_PROP1, TEST_VALUE1);
        btxn.getProperties().put(TEST_PROP2, TEST_VALUE2);

        // Top level (consumer) node
        Consumer c1 = new Consumer();
        btxn.getNodes().add(c1);

        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID1"));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "CID2"));
        c1.setDuration(1000);
        c1.setBaseTime(1);
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

        s1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID1"));
        s1.setDuration(900);
        s1.setBaseTime(2);
        s1.setOperation("Op1");
        s1.setUri("ServiceType1");

        // Third level (component) node
        Component cp1 = new Component();
        s1.getNodes().add(cp1);

        cp1.setDuration(400);
        cp1.setBaseTime(3);
        cp1.setUri("jdbc:TestDB");
        cp1.setComponentType("Database");
        cp1.setOperation("select X from Y");

        // Third level (component) node - this represents the service proxy
        // used by the consumer service
        Component s2 = new Component();
        s1.getNodes().add(s2);

        s2.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID3"));
        s2.setDuration(500);
        s2.setBaseTime(3);
        s2.setOperation("Op2");
        s2.setUri("ServiceType2");

        // Fourth level (producer) node
        Producer p1 = new Producer();
        s2.getNodes().add(p1);

        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID3"));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "CID4"));
        c1.setDuration(400);
        c1.setBaseTime(4);
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

        return (btxn);
    }

    @Test
    public void testCalculateDuration() {
        long baseTime = System.currentTimeMillis();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(baseTime); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setDuration(1000000000);
        c1.setBaseTime(100000000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setDuration(1000000000);
        p1.setBaseTime(500000000);
        p1.addInteractionId("interaction1");
        c1.getNodes().add(p1);

        btxn1.getNodes().add(c1);

        long duration = btxn1.calculateDuration();

        assertEquals(1400, duration);
    }
}
