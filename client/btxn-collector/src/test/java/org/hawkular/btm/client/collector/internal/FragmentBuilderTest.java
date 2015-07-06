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
package org.hawkular.btm.client.collector.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Service;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FragmentBuilderTest {

    @Test
    public void testInitialBusinessTxn() {
        FragmentBuilder builder = new FragmentBuilder();

        assertNotNull("Business transaction should not be null", builder.getBusinessTransaction());

        assertNotNull("Business transaction id should not be null", builder.getBusinessTransaction().getId());

        assertTrue("Business transaction should have no nodes", builder.getBusinessTransaction().getNodes().isEmpty());
    }

    @Test
    public void testPushSingleNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        assertFalse("Business transaction should not be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);
    }

    @Test
    public void testPushPopSingleNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        Node result = builder.popNode();

        assertTrue("Business transaction should be complete", builder.isComplete());

        assertEquals("Popped node not same", consumer, result);

        assertTrue("Should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);
    }

    @Test
    public void testPushOneNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        Service service = new Service();

        builder.pushNode(service);

        assertFalse("Business transaction should not be complete", builder.isComplete());

        assertTrue("BTxn should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);

        assertTrue("Consumer should have one node", consumer.getNodes().size() == 1);

        assertEquals("Consumer contained node incorrect", consumer.getNodes().get(0), service);
    }

    @Test
    public void testPushTwoChildNodes() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        Service service1 = new Service();

        builder.pushNode(service1);

        Node poppedService1 = builder.popNode();

        assertEquals("Popped service1 incorrect", poppedService1, service1);

        Service service2 = new Service();

        builder.pushNode(service2);

        Node poppedService2 = builder.popNode();

        assertEquals("Popped service2 incorrect", poppedService2, service2);

        Node poppedConsumer = builder.popNode();

        assertEquals("Popped consumer incorrect", poppedConsumer, consumer);

        assertTrue("Business transaction should be complete", builder.isComplete());

        assertTrue("BTxn should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);

        assertTrue("Consumer should have two child nodes", consumer.getNodes().size() == 2);

        assertEquals("Consumer contained node1 incorrect", consumer.getNodes().get(0), service1);
        assertEquals("Consumer contained node2 incorrect", consumer.getNodes().get(1), service2);
    }

    @Test
    public void testPushSingleNodeAndRetain() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        builder.retainNode("testId");

        builder.popNode();

        assertFalse("Business transaction should NOT be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);

        Node retained = builder.retrieveNode("testId");

        assertNotNull("Retained node should not be null", retained);

        assertEquals("Retained node incorrect", retained, consumer);

        builder.releaseNode("testId");

        assertTrue("Business transaction should now be complete after release", builder.isComplete());
    }

    @Test
    public void testSuppressSingleChildNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        builder.suppress();

        Component c1 = new Component();
        builder.pushNode(c1);

        builder.popNode();

        assertTrue("Should be suppressed", builder.isSuppressed());

        builder.popNode();

        assertFalse("Should no longer be suppressed", builder.isSuppressed());

        assertTrue("Business transaction should be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);

        assertTrue("Should have zero child nodes", ((Consumer) builder.getBusinessTransaction().getNodes().get(0))
                .getNodes().size() == 0);
    }

    @Test
    public void testSuppressDoubleChildNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        builder.suppress();

        Component c1 = new Component();
        builder.pushNode(c1);

        builder.popNode();

        Component c2 = new Component();
        builder.pushNode(c2);

        builder.popNode();

        assertTrue("Should be suppressed", builder.isSuppressed());

        builder.popNode();

        assertFalse("Should no longer be suppressed", builder.isSuppressed());

        assertTrue("Business transaction should be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getBusinessTransaction().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getBusinessTransaction().getNodes().get(0), consumer);

        assertTrue("Should have zero child nodes", ((Consumer) builder.getBusinessTransaction().getNodes().get(0))
                .getNodes().size() == 0);
    }

}
