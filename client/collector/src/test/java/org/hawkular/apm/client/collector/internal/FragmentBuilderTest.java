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
package org.hawkular.apm.client.collector.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Node;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FragmentBuilderTest {

    @Test
    public void testInitialBusinessTxn() {
        FragmentBuilder builder = new FragmentBuilder();

        assertNotNull("Trace should not be null", builder.getTrace());

        assertNotNull("Trace id should not be null", builder.getTrace().getFragmentId());

        assertTrue("Trace should have no nodes", builder.getTrace().getNodes().isEmpty());
    }

    @Test
    public void testPushSingleNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        assertFalse("Trace should not be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

        assertEquals(1, builder.getNodeStack().size());
    }

    @Test
    public void testPushPopSingleNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        Node result = builder.popNode(Consumer.class, null);

        assertTrue("Trace should be complete", builder.isComplete());

        assertEquals("Popped node not same", consumer, result);

        assertTrue("Should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

        assertEquals(0, builder.getNodeStack().size());

        assertEquals(1, builder.getPoppedNodes().size());
    }

    @Test
    public void testPoppedNodesClearedAfterPush() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        builder.popNode(Consumer.class, null);

        Component comp = new Component();

        builder.pushNode(comp);

        assertEquals(1, builder.getNodeStack().size());

        assertEquals(0, builder.getPoppedNodes().size());
    }

    @Test
    public void testPushOneNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        Component service = new Component();

        builder.pushNode(service);

        assertFalse("Trace should not be complete", builder.isComplete());

        assertTrue("Trace should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

        assertTrue("Consumer should have one node", consumer.getNodes().size() == 1);

        assertEquals("Consumer contained node incorrect", consumer.getNodes().get(0), service);
    }

    @Test
    public void testPushTwoChildNodes() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        Component service1 = new Component();

        builder.pushNode(service1);

        Node poppedService1 = builder.popNode(Component.class, null);

        assertEquals("Popped service1 incorrect", poppedService1, service1);

        Component service2 = new Component();

        builder.pushNode(service2);

        Node poppedService2 = builder.popNode(Component.class, null);

        assertEquals("Popped service2 incorrect", poppedService2, service2);

        Node poppedConsumer = builder.popNode(Consumer.class, null);

        assertEquals("Popped consumer incorrect", poppedConsumer, consumer);

        assertTrue("Trace should be complete", builder.isComplete());

        assertTrue("Trace should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

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

        builder.popNode(Consumer.class, null);

        assertFalse("Trace should NOT be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

        Node retained = builder.retrieveNode("testId");

        assertNotNull("Retained node should not be null", retained);

        assertEquals("Retained node incorrect", retained, consumer);

        builder.releaseNode("testId");

        assertTrue("Trace should now be complete after release", builder.isComplete());
    }

    @Test
    public void testSuppressSingleChildNode() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();

        builder.pushNode(consumer);

        builder.suppress();

        Component c1 = new Component();
        builder.pushNode(c1);

        builder.popNode(Component.class, null);

        assertTrue("Should be suppressed", builder.isSuppressed());

        builder.popNode(Consumer.class, null);

        assertFalse("Should no longer be suppressed", builder.isSuppressed());

        assertTrue("Trace should be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

        assertTrue("Should have zero child nodes", ((Consumer) builder.getTrace().getNodes().get(0))
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

        builder.popNode(Component.class, null);

        Component c2 = new Component();
        builder.pushNode(c2);

        builder.popNode(Component.class, null);

        assertTrue("Should be suppressed", builder.isSuppressed());

        builder.popNode(Consumer.class, null);

        assertFalse("Should no longer be suppressed", builder.isSuppressed());

        assertTrue("Trace should be complete", builder.isComplete());

        assertTrue("Should have one node", builder.getTrace().getNodes().size() == 1);

        assertEquals("Node incorrect", builder.getTrace().getNodes().get(0), consumer);

        assertTrue("Should have zero child nodes", ((Consumer) builder.getTrace().getNodes().get(0))
                .getNodes().size() == 0);
    }

    @Test
    public void testAsyncStackNoUri() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();
        builder.pushNode(consumer);

        Component c1 = new Component();
        builder.pushNode(c1);

        assertNotNull(builder.popNode(Consumer.class, null));

        assertFalse(builder.isComplete());

        assertNotNull(builder.popNode(Component.class, null));

        assertTrue(builder.isComplete());
    }

    @Test
    public void testAsyncStackIgnoreNodeNoUri() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();
        builder.pushNode(consumer);

        Component c1 = new Component();
        builder.pushNode(c1);
        builder.ignoreNode();

        assertNotNull(builder.popNode(Consumer.class, null));

        assertFalse(builder.isComplete());

        assertTrue(builder.isCompleteExceptIgnoredNodes());

        assertNotNull(builder.popNode(Component.class, null));

        assertTrue(builder.isComplete());
    }

    @Test
    public void testAsyncStackWithUriNotPassed() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();
        consumer.setUri("ConsumerURI");
        builder.pushNode(consumer);

        Component c1 = new Component();
        c1.setUri("ComponentURI");
        builder.pushNode(c1);

        assertNotNull(builder.popNode(Consumer.class, null));

        assertNotNull(builder.popNode(Component.class, null));

        assertTrue(builder.isComplete());
    }

    @Test
    public void testAsyncStackWithUriPassed() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();
        consumer.setUri("ConsumerURI");
        builder.pushNode(consumer);

        Component c1 = new Component();
        c1.setUri("ComponentURI");
        builder.pushNode(c1);

        assertNotNull(builder.popNode(Consumer.class, "ConsumerURI"));

        assertNotNull(builder.popNode(Component.class, "ComponentURI"));

        assertTrue(builder.isComplete());

        assertNotNull(builder.getTrace());

        assertEquals(1, builder.getTrace().getNodes().size());

        assertEquals(Consumer.class, builder.getTrace().getNodes().get(0).getClass());

        assertEquals(1, ((Consumer) builder.getTrace().getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testAsyncStackSuppressed() {
        FragmentBuilder builder = new FragmentBuilder();

        Consumer consumer = new Consumer();
        consumer.setUri("ConsumerURI");
        builder.pushNode(consumer);

        builder.suppress();

        Component c1 = new Component();
        c1.setUri("ComponentURI");
        builder.pushNode(c1);

        assertNotNull(builder.popNode(Consumer.class, "ConsumerURI"));

        assertNotNull(builder.popNode(Component.class, "ComponentURI"));

        assertTrue(builder.isComplete());

        assertNotNull(builder.getTrace());

        assertEquals(1, builder.getTrace().getNodes().size());

        assertEquals(Consumer.class, builder.getTrace().getNodes().get(0).getClass());

        assertEquals(0, ((Consumer) builder.getTrace().getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testInitInBuffer() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initInBuffer(1);
        assertTrue(builder.isInBufferActive(1));
    }

    @Test
    public void testInitInBufferIncorrectHashcode() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initInBuffer(1);
        assertFalse(builder.isInBufferActive(2));
    }

    @Test
    public void testInitInBufferIgnoreHashcode() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initInBuffer(1);
        assertTrue(builder.isInBufferActive(-1));
    }

    @Test
    public void testInitOutBuffer() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initOutBuffer(1);
        assertTrue(builder.isOutBufferActive(1));
    }

    @Test
    public void testInitOutBufferIncorrectHashcode() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initOutBuffer(1);
        assertFalse(builder.isOutBufferActive(2));
    }

    @Test
    public void testInitOutBufferIgnoreHashcode() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initOutBuffer(1);
        assertTrue(builder.isOutBufferActive(-1));
    }

    @Test
    public void testWriteInData() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initInBuffer(1);

        String data1 = "Hello ";
        builder.writeInData(1, data1.getBytes(), 0, data1.length());

        String data2 = "World";
        builder.writeInData(1, data2.getBytes(), 0, data2.length());

        assertTrue(builder.isInBufferActive(1));
        assertEquals("Hello World", new String(builder.getInData(1)));

        assertFalse(builder.isInBufferActive(1));
    }

    @Test
    public void testWriteOutData() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initOutBuffer(1);

        String data1 = "Hello ";
        builder.writeOutData(1, data1.getBytes(), 0, data1.length());

        String data2 = "World";
        builder.writeOutData(1, data2.getBytes(), 0, data2.length());

        assertTrue(builder.isOutBufferActive(1));
        assertEquals("Hello World", new String(builder.getOutData(1)));

        assertFalse(builder.isOutBufferActive(1));
    }

    @Test
    public void testWriteInDataIgnoreHashcode() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initInBuffer(1);

        String data1 = "Hello ";
        builder.writeInData(1, data1.getBytes(), 0, data1.length());

        String data2 = "World";
        builder.writeInData(1, data2.getBytes(), 0, data2.length());

        assertTrue(builder.isInBufferActive(1));
        assertEquals("Hello World", new String(builder.getInData(-1)));

        assertFalse(builder.isInBufferActive(1));
    }

    @Test
    public void testWriteOutDataIgnoreHashcode() {
        FragmentBuilder builder = new FragmentBuilder();
        builder.initOutBuffer(1);

        String data1 = "Hello ";
        builder.writeOutData(1, data1.getBytes(), 0, data1.length());

        String data2 = "World";
        builder.writeOutData(1, data2.getBytes(), 0, data2.length());

        assertTrue(builder.isOutBufferActive(1));
        assertEquals("Hello World", new String(builder.getOutData(-1)));

        assertFalse(builder.isOutBufferActive(1));
    }

}
