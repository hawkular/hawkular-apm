/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
import static org.junit.Assert.assertNotNull;
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

        Consumer node1 = new Consumer();
        node1.setStartTime(100);
        btxn.getNodes().add(node1);

        assertEquals("Start time incorrect", 100L, btxn.startTime());
    }

    @Test
    public void testEndTime() {
        BusinessTransaction btxn = new BusinessTransaction();

        Consumer node1 = new Consumer();
        node1.setStartTime(100);
        btxn.getNodes().add(node1);

        Service node2 = new Service();
        node2.setStartTime(150);
        node2.setDuration(0);
        node1.getNodes().add(node2);

        // This node will have the latest time associated with the
        // business transaction, comprised of the start time + duration
        Producer node3 = new Producer();
        node3.setStartTime(200);
        node3.setDuration(50);
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

    protected BusinessTransaction example1() {
        // Business transaction
        BusinessTransaction btxn = new BusinessTransaction();

        btxn.getProperties().put(TEST_PROP1, TEST_VALUE1);
        btxn.getProperties().put(TEST_PROP2, TEST_VALUE2);

        // Top level (consumer) node
        Consumer c1 = new Consumer();
        btxn.getNodes().add(c1);

        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID1", 0));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Exchange, "CID2", 0));
        c1.setDuration(1000);
        c1.setStartTime(1);
        c1.setEndpointType("JMS");
        c1.setUri("queue:test");

        Message req1 = new Message();
        req1.getHeaders().put(HEADER1, VALUE1);
        req1.getParameters().add("Parameter1");

        c1.setRequest(req1);

        Message resp1 = new Message();
        resp1.getHeaders().put(HEADER2, VALUE2);
        resp1.getParameters().add("Parameter2");

        c1.setResponse(resp1);

        // Second level (service) node
        Service s1 = new Service();
        c1.getNodes().add(s1);

        s1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID1", 0));
        s1.setDuration(900);
        s1.setStartTime(2);
        s1.setOperation("Op1");
        s1.setServiceType("ServiceType1");

        Message req2 = new Message();
        req2.getHeaders().put(HEADER1, VALUE1);
        req2.getParameters().add("Parameter1");

        s1.setRequest(req2);

        Message resp2 = new Message();
        resp2.getHeaders().put(HEADER2, VALUE2);
        resp2.getParameters().add("Parameter2");

        s1.setResponse(resp2);

        // Third level (component) node
        Component cp1 = new Component();
        s1.getNodes().add(cp1);

        cp1.setDuration(400);
        cp1.setStartTime(3);
        cp1.setUri("jdbc:TestDB");
        cp1.setComponentType("Database");

        Message req3 = new Message();
        req3.getParameters().add("select X from Y");

        cp1.setRequest(req3);

        Message resp3 = new Message();
        resp3.getParameters().add("23");

        cp1.setResponse(resp3);

        // Third level (service) node - this represents the service proxy
        // used by the consumer service
        Service s2 = new Service();
        s1.getNodes().add(s2);

        s2.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID3", 0));
        s2.setDuration(500);
        s2.setStartTime(3);
        s2.setOperation("Op2");
        s2.setServiceType("ServiceType2");

        Message req4 = new Message();
        req4.getHeaders().put(HEADER1, VALUE1);
        req4.getParameters().add("Parameter3");

        s2.setRequest(req4);

        Message resp4 = new Message();
        resp4.getHeaders().put(HEADER2, VALUE2);
        resp4.getParameters().add("Parameter4");

        s2.setResponse(resp4);

        // Fourth level (producer) node
        Producer p1 = new Producer();
        s2.getNodes().add(p1);

        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "CID3", 0));
        c1.getCorrelationIds().add(new CorrelationIdentifier(Scope.Exchange, "CID4", 1000));
        c1.setDuration(400);
        c1.setStartTime(4);
        c1.setEndpointType("HTTP");
        c1.setUri("http://example.com/service");

        Message req5 = new Message();
        req5.getHeaders().put(HEADER1, VALUE1);
        req5.getParameters().add("Parameter5");

        p1.setRequest(req5);

        Message resp5 = new Message();
        resp5.getHeaders().put(HEADER2, VALUE2);
        resp5.getParameters().add("Parameter6");

        p1.setResponse(resp5);

        return (btxn);
    }

}
