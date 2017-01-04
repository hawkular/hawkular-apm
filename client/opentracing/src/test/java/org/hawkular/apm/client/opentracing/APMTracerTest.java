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
package org.hawkular.apm.client.opentracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.tests.common.Wait;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class APMTracerTest {

    private static final String MY_VALUE = "myValue";
    private static final String MY_TAG = "myTag";
    private static final String TEST_TXN = "TestBTxn";
    private static final String TEST_APM_ID = "abcd";
    private static final String TEST_APM_TRACEID = "xyz";
    private static ObjectMapper mapper;

    @BeforeClass
    public static void init() {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testClient() throws JsonProcessingException, InterruptedException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        ClientService service = new ClientService(tracer, MY_VALUE);

        service.handle();

        assertEquals(1, recorder.getTraces().size());

        Trace trace = recorder.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Component.class, trace.getNodes().get(0).getClass());

        Component component = (Component) trace.getNodes().get(0);

        // Get producer invoking a remote service
        assertEquals(1, component.getNodes().size());
        assertEquals(Producer.class, component.getNodes().get(0).getClass());

        Producer producer = (Producer) component.getNodes().get(0);

        assertEquals(1, service.getMessages().size());

        // Check producer has interaction based correlation id matching the value in the outbound message
        assertTrue(service.getMessages().get(0).getHeaders().containsKey(Constants.HAWKULAR_APM_ID));

        assertEquals(1, producer.getCorrelationIds().size());
        assertEquals(producer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_ID)));

        assertEquals(trace.getTraceId(), service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TRACEID));

        Set<Property> props=producer.getProperties(MY_TAG);

        assertFalse(props.isEmpty());
        assertEquals(MY_VALUE, props.iterator().next().getValue());

        assertEquals(ClientService.ORDER_ID_VALUE,
                component.getProperties(ClientService.ORDER_ID_NAME).iterator().next().getValue());
    }

    @Test
    public void testClientNullTag() throws JsonProcessingException, InterruptedException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        ClientService service = new ClientService(tracer, null);

        service.handle();

        assertEquals(1, recorder.getTraces().size());

        Trace trace = recorder.getTraces().get(0);
        assertEquals(1, trace.getNodes().size());
        assertEquals(Component.class, trace.getNodes().get(0).getClass());

        Component component = (Component) trace.getNodes().get(0);

        // Get producer invoking a remote service
        assertEquals(1, component.getNodes().size());
        assertEquals(Producer.class, component.getNodes().get(0).getClass());

        Producer producer = (Producer) component.getNodes().get(0);
        Set<Property> props=producer.getProperties(MY_TAG);

        assertTrue(props.isEmpty());
    }

    @Test
    public void testSync() throws JsonProcessingException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        SyncService service = new SyncService(tracer);

        Message message = new Message();
        message.getHeaders().put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        message.getHeaders().put(Constants.HAWKULAR_APM_ID, TEST_APM_ID);
        message.getHeaders().put(Constants.HAWKULAR_APM_TXN, TEST_TXN);

        service.handle1(message);

        assertEquals(1, recorder.getTraces().size());

        Trace trace = recorder.getTraces().get(0);

        assertEquals(TEST_TXN, trace.getTransaction());
        assertEquals(1, trace.getNodes().size());
        assertEquals(Consumer.class, trace.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) trace.getNodes().get(0);

        // Check has supplied correlation id
        assertEquals(1, consumer.getCorrelationIds().size());
        assertEquals(consumer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID));

        assertEquals("http", consumer.getEndpointType());
        assertEquals(1, consumer.getProperties(Constants.PROP_FAULT).size());
        assertEquals(SyncService.MY_FAULT, consumer.getProperties(Constants.PROP_FAULT).iterator().next().getValue());

        // Get middle component
        assertEquals(1, consumer.getNodes().size());
        assertEquals(Component.class, consumer.getNodes().get(0).getClass());

        Component component = (Component) consumer.getNodes().get(0);

        // Get producer invoking a remote service
        assertEquals(1, component.getNodes().size());
        assertEquals(Constants.COMPONENT_DATABASE, component.getComponentType());
        assertTrue(component.getProperties(Constants.PROP_DATABASE_STATEMENT).size() > 0);
        assertEquals(Producer.class, component.getNodes().get(0).getClass());

        Producer producer = (Producer) component.getNodes().get(0);

        assertEquals(1, service.getMessages().size());

        // Check producer has interaction based correlation id matching the value in the outbound message
        assertTrue(service.getMessages().get(0).getHeaders().containsKey(Constants.HAWKULAR_APM_ID));
        assertEquals("http", producer.getEndpointType());
        assertEquals(1, producer.getCorrelationIds().size());
        assertEquals(producer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_ID)));

        assertEquals(TEST_APM_TRACEID, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TRACEID));
    }

    @Test
    public void testSyncSetTxnNameOnConsumer() throws JsonProcessingException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        SyncService service = new SyncService(tracer);

        Message message = new Message();
        message.getHeaders().put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        message.getHeaders().put(Constants.HAWKULAR_APM_ID, TEST_APM_ID);

        service.handle1(message);

        assertEquals(1, recorder.getTraces().size());

        Trace trace = recorder.getTraces().get(0);

        assertEquals(SyncService.SYNC_TXN_NAME_1, trace.getTransaction());
        assertEquals(1, trace.getNodes().size());
        assertEquals(Consumer.class, trace.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) trace.getNodes().get(0);

        // Check has supplied correlation id
        assertEquals(1, consumer.getCorrelationIds().size());
        assertEquals(consumer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID));

        assertEquals("http", consumer.getEndpointType());
        assertEquals(1, consumer.getProperties(Constants.PROP_FAULT).size());
        assertEquals(SyncService.MY_FAULT, consumer.getProperties(Constants.PROP_FAULT).iterator().next().getValue());

        // Get middle component
        assertEquals(1, consumer.getNodes().size());
        assertEquals(Component.class, consumer.getNodes().get(0).getClass());

        Component component = (Component) consumer.getNodes().get(0);

        // Get producer invoking a remote service
        assertEquals(1, component.getNodes().size());
        assertEquals(Constants.COMPONENT_DATABASE, component.getComponentType());
        assertTrue(component.getProperties(Constants.PROP_DATABASE_STATEMENT).size() > 0);
        assertEquals(Producer.class, component.getNodes().get(0).getClass());

        Producer producer = (Producer) component.getNodes().get(0);

        assertEquals(1, service.getMessages().size());

        // Check producer has interaction based correlation id matching the value in the outbound message
        assertTrue(service.getMessages().get(0).getHeaders().containsKey(Constants.HAWKULAR_APM_ID));
        assertEquals("http", producer.getEndpointType());
        assertEquals(1, producer.getCorrelationIds().size());
        assertEquals(producer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_ID)));

        assertEquals(TEST_APM_TRACEID, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TRACEID));

        assertEquals(SyncService.SYNC_TXN_NAME_1,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TXN));
    }

    @Test
    public void testSyncSetTxnNameOnProducer() throws JsonProcessingException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        SyncService service = new SyncService(tracer);

        Message message = new Message();
        message.getHeaders().put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        message.getHeaders().put(Constants.HAWKULAR_APM_ID, TEST_APM_ID);

        // Call alternate 'handle' method that does not set the transaction name straightaway
        service.handle2(message);

        assertEquals(1, recorder.getTraces().size());

        Trace trace = recorder.getTraces().get(0);

        assertEquals(SyncService.SYNC_TXN_NAME_2, trace.getTransaction());
        assertEquals(1, trace.getNodes().size());
        assertEquals(Consumer.class, trace.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) trace.getNodes().get(0);

        // Check has supplied correlation id
        assertEquals(1, consumer.getCorrelationIds().size());
        assertEquals(consumer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID));

        assertEquals("http", consumer.getEndpointType());
        assertEquals(1, consumer.getProperties(Constants.PROP_FAULT).size());
        assertEquals(SyncService.MY_FAULT, consumer.getProperties(Constants.PROP_FAULT).iterator().next().getValue());

        // Get middle component
        assertEquals(1, consumer.getNodes().size());
        assertEquals(Component.class, consumer.getNodes().get(0).getClass());

        Component component = (Component) consumer.getNodes().get(0);

        // Get producer invoking a remote service
        assertEquals(1, component.getNodes().size());
        assertEquals(Constants.COMPONENT_DATABASE, component.getComponentType());
        assertTrue(component.getProperties(Constants.PROP_DATABASE_STATEMENT).size() > 0);
        assertEquals(Producer.class, component.getNodes().get(0).getClass());

        Producer producer = (Producer) component.getNodes().get(0);

        assertEquals(1, service.getMessages().size());

        // Check producer has interaction based correlation id matching the value in the outbound message
        assertTrue(service.getMessages().get(0).getHeaders().containsKey(Constants.HAWKULAR_APM_ID));
        assertEquals("http", producer.getEndpointType());
        assertEquals(1, producer.getCorrelationIds().size());
        assertEquals(producer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_ID)));

        assertEquals(TEST_APM_TRACEID, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TRACEID));

        assertEquals(SyncService.SYNC_TXN_NAME_2,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TXN));
    }

    @Test
    public void testAsync() throws JsonProcessingException, InterruptedException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        AsyncService service = new AsyncService(tracer);

        Message message = new Message();
        message.getHeaders().put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        message.getHeaders().put(Constants.HAWKULAR_APM_ID, TEST_APM_ID);
        message.getHeaders().put(Constants.HAWKULAR_APM_TXN, TEST_TXN);

        CountDownLatch latch=new CountDownLatch(1);
        service.handle(message, obj -> {
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);

        Wait.until(() -> recorder.getTraces().size() == 1, 5, TimeUnit.SECONDS);

        assertEquals(1, recorder.getTraces().size());

        Trace trace1 = recorder.getTraces().get(0);
        assertEquals(TEST_TXN, trace1.getTransaction());
        assertEquals(1, trace1.getNodes().size());
        assertEquals(Consumer.class, trace1.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) trace1.getNodes().get(0);

        // Check has supplied correlation id
        assertEquals(1, consumer.getCorrelationIds().size());
        assertEquals(consumer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID));

        // Get middle component
        assertEquals(1, consumer.getNodes().size());
        assertEquals(Producer.class, consumer.getNodes().get(0).getClass());

        Producer producer = (Producer) consumer.getNodes().get(0);

        assertEquals(1, service.getMessages().size());

        // Check producer has interaction based correlation id matching the value in the outbound message
        assertTrue(service.getMessages().get(0).getHeaders().containsKey(Constants.HAWKULAR_APM_ID));

        assertEquals(1, producer.getCorrelationIds().size());
        assertEquals(producer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_ID)));

        assertEquals(TEST_APM_TRACEID, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TRACEID));

        assertEquals(TEST_TXN, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TXN));
    }

    @Test
    public void testSpawn() throws JsonProcessingException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        SpawnService service = new SpawnService(tracer);

        Message message = new Message();
        message.getHeaders().put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        message.getHeaders().put(Constants.HAWKULAR_APM_ID, TEST_APM_ID);
        message.getHeaders().put(Constants.HAWKULAR_APM_TXN, TEST_TXN);

        service.handle(message);

        Wait.until(() -> recorder.getTraces().size() == 2, 5, TimeUnit.SECONDS);

        assertEquals(2, recorder.getTraces().size());

        Trace trace1 = recorder.getTraces().get(0);
        assertEquals(TEST_TXN, trace1.getTransaction());
        assertEquals(1, trace1.getNodes().size());
        assertEquals(Consumer.class, trace1.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) trace1.getNodes().get(0);

        // Check has supplied correlation id
        assertEquals(1, consumer.getCorrelationIds().size());
        assertEquals(consumer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID));

        // Get middle component
        assertEquals(1, consumer.getNodes().size());
        assertEquals(Component.class, consumer.getNodes().get(0).getClass());

        Component component1 = (Component) consumer.getNodes().get(0);

        assertEquals(0, component1.getNodes().size());

        // Check trace2
        Trace trace2 = recorder.getTraces().get(1);
        assertEquals(TEST_TXN, trace2.getTransaction());
        assertEquals(1, trace2.getNodes().size());
        assertEquals(Consumer.class, trace2.getNodes().get(0).getClass());

        Consumer consumer2 = (Consumer) trace2.getNodes().get(0);

        assertTrue(consumer2.getCorrelationIds().contains(new CorrelationIdentifier(Scope.CausedBy,
                trace1.getFragmentId() + ":0:0")));
        assertEquals(EndpointUtil.getSourceEndpoint(trace1), EndpointUtil.getSourceEndpoint(trace2));

        assertEquals(1, consumer2.getNodes().size());
        assertEquals(Component.class, consumer2.getNodes().get(0).getClass());

        Component component2 = (Component) consumer2.getNodes().get(0);

        // Get producer invoking a remote service
        assertEquals(1, component2.getNodes().size());
        assertEquals(Producer.class, component2.getNodes().get(0).getClass());

        Producer producer = (Producer) component2.getNodes().get(0);

        assertEquals(1, service.getMessages().size());

        // Check producer has interaction based correlation id matching the value in the outbound message
        assertTrue(service.getMessages().get(0).getHeaders().containsKey(Constants.HAWKULAR_APM_ID));

        assertEquals(1, producer.getCorrelationIds().size());
        assertEquals(producer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction,
                service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_ID)));

        assertEquals(TEST_APM_TRACEID, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TRACEID));

        assertEquals(TEST_TXN, service.getMessages().get(0).getHeaders().get(Constants.HAWKULAR_APM_TXN));
    }

    @Test
    public void testForkJoin() throws JsonProcessingException {
        TestTraceRecorder recorder = new TestTraceRecorder();
        APMTracer tracer = new APMTracer(recorder);

        ForkJoinService service = new ForkJoinService(tracer);

        Message message = new Message();
        message.getHeaders().put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        message.getHeaders().put(Constants.HAWKULAR_APM_ID, TEST_APM_ID);
        message.getHeaders().put(Constants.HAWKULAR_APM_TXN, TEST_TXN);

        service.handle(message);

        assertEquals(1, recorder.getTraces().size());

        Trace trace = recorder.getTraces().get(0);
        assertEquals(TEST_APM_TRACEID, trace.getTraceId());
        assertEquals(TEST_TXN, trace.getTransaction());
        assertEquals(1, trace.getNodes().size());
        assertEquals(Consumer.class, trace.getNodes().get(0).getClass());

        Consumer consumer = (Consumer) trace.getNodes().get(0);

        // Check has supplied correlation id
        assertEquals(1, consumer.getCorrelationIds().size());
        assertEquals(consumer.getCorrelationIds().get(0), new CorrelationIdentifier(Scope.Interaction, TEST_APM_ID));

        // Get middle component
        assertEquals(5, consumer.getNodes().size());
        for (int i = 0; i < 5; i++) {
            assertEquals(Component.class, consumer.getNodes().get(i).getClass());
            Component component = (Component) consumer.getNodes().get(i);
            assertEquals(0, component.getNodes().size());
        }

        assertEquals(0, service.getMessages().size());
    }

    public static class TestTraceRecorder implements TraceRecorder {

        private List<Trace> traces = new ArrayList<>();

        @Override
        public void record(Trace trace) {
            traces.add(trace);
        }

        /**
         * @return the traces
         */
        public List<Trace> getTraces() {
            return traces;
        }

        public void clear() {
            traces.clear();
        }
    }

}
