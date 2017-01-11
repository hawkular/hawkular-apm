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
package org.hawkular.apm.server.processor.nodedetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.junit.Test;

/**
 * @author gbrown
 */
public class NodeDetailsDeriverTest {

    private static final String INTERNAL_URI = "internalUri";

    private static final String TEST_URI = "testUri";

    @Test
    public void testProcessMultipleNotInternalConsumer() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Trace trace = new Trace();

        Consumer consumer = new Consumer();
        consumer.setUri(INTERNAL_URI);
        trace.getNodes().add(consumer);

        Producer producer = new Producer();
        producer.setEndpointType("HTTP");
        producer.setUri(TEST_URI);
        consumer.getNodes().add(producer);

        List<NodeDetails> details = deriver.processOneToMany(null, trace);

        assertNotNull(details);
        assertEquals(1, details.size());

        assertEquals(TEST_URI, details.get(0).getUri());
    }

    @Test
    public void testProcessMultipleNotInternalProducer() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Trace trace = new Trace();

        Consumer consumer = new Consumer();
        consumer.setEndpointType("HTTP");
        consumer.setUri(TEST_URI);
        trace.getNodes().add(consumer);

        Producer producer = new Producer();
        producer.setUri(INTERNAL_URI);
        consumer.getNodes().add(producer);

        List<NodeDetails> details = deriver.processOneToMany(null, trace);

        assertNotNull(details);
        assertEquals(1, details.size());

        assertEquals(TEST_URI, details.get(0).getUri());
        // NodeDetails for Consumer, which is initial node of the fragment
        assertTrue(details.get(0).isInitial());
    }

    @Test
    public void testProcessCommonProperties() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Trace trace = new Trace();

        Property p1 = new Property(Constants.PROP_BUILD_STAMP, "myBuild");
        Property p2 = new Property(Constants.PROP_PRINCIPAL, "jdoe");
        Property p3 = new Property(Constants.PROP_SERVICE_NAME, "myService");
        Property p4 = new Property("LocalProp", "LocalValue");

        Consumer consumer = new Consumer();
        consumer.setEndpointType("HTTP");
        consumer.getProperties().add(p1);
        consumer.getProperties().add(p2);
        trace.getNodes().add(consumer);

        Component component1 = new Component();
        component1.getProperties().add(p3);
        consumer.getNodes().add(component1);

        Component component2 = new Component();
        component2.getProperties().add(p4);
        component1.getNodes().add(component2);

        List<NodeDetails> details = deriver.processOneToMany(null, trace);

        assertEquals(3, details.size());
        // Has all properties
        assertEquals(new HashSet<>(Arrays.asList(p1,p2,p3,p4)), details.get(0).getProperties());
        // Has three properties, the common ones
        assertEquals(new HashSet<>(Arrays.asList(p1,p2,p3)), details.get(1).getProperties());
        // Has one extra due to a local property on the component
        assertEquals(new HashSet<>(Arrays.asList(p1,p2,p3,p4)), details.get(2).getProperties());
    }

    @Test
    public void testNotInitial() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Trace trace = new Trace();

        Consumer consumer = new Consumer();
        trace.getNodes().add(consumer);

        Producer producer = new Producer();
        producer.setEndpointType("HTTP");
        consumer.getNodes().add(producer);

        List<NodeDetails> details = deriver.processOneToMany(null, trace);

        assertNotNull(details);
        assertEquals(1, details.size());
        // Should not be initial, as connected via internal link from another fragment
        assertFalse(details.get(0).isInitial());
    }

    @Test
    public void testInitial() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Trace trace = new Trace();

        Consumer consumer = new Consumer();
        consumer.setEndpointType("HTTP");
        trace.getNodes().add(consumer);

        Component component = new Component();
        consumer.getNodes().add(component);

        List<NodeDetails> details = deriver.processOneToMany(null, trace);

        assertNotNull(details);
        assertEquals(2, details.size());
        assertTrue(details.get(0).isInitial());
        assertFalse(details.get(1).isInitial());
    }

    @Test
    public void testCalculateActualTimeSync() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Consumer consumer = new Consumer();
        consumer.setTimestamp(1000);
        consumer.setDuration(500);

        Component component1 = new Component();
        component1.setTimestamp(1100);
        component1.setDuration(200);
        consumer.getNodes().add(component1);

        Component component2 = new Component();
        component2.setTimestamp(1300);
        component2.setDuration(200);
        consumer.getNodes().add(component2);

        assertEquals(100, deriver.calculateActualTime(consumer));
    }

    @Test
    public void testCalculateActualTimeForkJoin() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Consumer consumer = new Consumer();
        consumer.setTimestamp(1000);
        consumer.setDuration(500);

        Component component1 = new Component();
        component1.setTimestamp(1100);
        component1.setDuration(250);
        consumer.getNodes().add(component1);

        Component component2 = new Component();
        component2.setTimestamp(1100);
        component2.setDuration(300);
        consumer.getNodes().add(component2);

        assertEquals(200, deriver.calculateActualTime(consumer));
    }

    @Test
    public void testCalculateActualTimeAsync() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Consumer consumer = new Consumer();
        consumer.setTimestamp(1000);
        consumer.setDuration(500);

        Component component1 = new Component();
        component1.setTimestamp(1100);
        component1.setDuration(550);
        consumer.getNodes().add(component1);

        Component component2 = new Component();
        component2.setTimestamp(1100);
        component2.setDuration(700);
        consumer.getNodes().add(component2);

        assertEquals(consumer.getDuration(), deriver.calculateActualTime(consumer));
    }

    @Test
    public void testCalculateActualTimeAsync2() throws RetryAttemptException {
        NodeDetailsDeriver deriver = new NodeDetailsDeriver();

        Consumer consumer = new Consumer();
        consumer.setTimestamp(1000);
        consumer.setDuration(500);

        Component component1 = new Component();
        component1.setTimestamp(1200);
        component1.setDuration(400);
        consumer.getNodes().add(component1);

        assertEquals(consumer.getDuration(), deriver.calculateActualTime(consumer));
    }

}
