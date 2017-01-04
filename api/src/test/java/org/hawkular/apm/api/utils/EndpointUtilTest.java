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
package org.hawkular.apm.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.junit.Test;

/**
 * @author gbrown
 */
public class EndpointUtilTest {

    private static final String OPERATION = "Op";
    private static final String URI = "/uri";

    @Test
    public void testEndpointUri() {
        assertEquals(URI, EndpointUtil.encodeEndpoint(URI, null));
    }

    @Test
    public void testEndpointUriAndOperation() {
        String result = EndpointUtil.encodeEndpoint(URI, OPERATION);
        String expected = URI + "[" + OPERATION + "]";
        assertEquals(expected, result);
    }

    @Test
    public void testDecodeEndpointURIWithOp() {
        String result = EndpointUtil.encodeEndpoint(URI, OPERATION);
        assertEquals(URI, EndpointUtil.decodeEndpointURI(result));
    }

    @Test
    public void testDecodeEndpointURIWithoutOp() {
        String result = EndpointUtil.encodeEndpoint(URI, null);
        assertEquals(URI, EndpointUtil.decodeEndpointURI(result));
    }

    @Test
    public void testDecodeEndpointOpStripped() {
        String result = EndpointUtil.encodeEndpoint(URI, OPERATION);
        assertEquals(OPERATION, EndpointUtil.decodeEndpointOperation(result,true));
    }

    @Test
    public void testDecodeEndpointOpNotStripped() {
        String result = EndpointUtil.encodeEndpoint(URI, OPERATION);
        assertEquals("["+OPERATION+"]", EndpointUtil.decodeEndpointOperation(result,false));
    }

    @Test
    public void testDecodeEndpointOpNull() {
        String result = EndpointUtil.encodeEndpoint(URI, null);
        assertNull(EndpointUtil.decodeEndpointOperation(result,false));
    }

    @Test
    public void testEncodeClientURI() {
        assertEquals(Constants.URI_CLIENT_PREFIX + URI, EndpointUtil.encodeClientURI(URI));
    }

    @Test
    public void testDecodeClientURI() {
        assertEquals(URI, EndpointUtil.decodeClientURI(Constants.URI_CLIENT_PREFIX + URI));
    }

    @Test
    public void testDecodeNonClientURI() {
        assertEquals(URI, EndpointUtil.decodeClientURI(URI));
    }

    @Test
    public void testGetSourceEndpointConsumer() {
        Trace trace = new Trace();
        trace.setTraceId("1");
        trace.setFragmentId(trace.getTraceId());
        Consumer consumer = new Consumer();
        consumer.setUri(URI);
        consumer.setOperation(OPERATION);
        trace.getNodes().add(consumer);

        EndpointRef ep = EndpointUtil.getSourceEndpoint(trace);

        assertNotNull(ep);
        assertEquals(new EndpointRef(URI, OPERATION, false), ep);
    }

    @Test
    public void testGetSourceEndpointClientProducer() {
        Trace trace = new Trace();
        trace.setTraceId("1");
        trace.setFragmentId(trace.getTraceId());
        Producer producer = new Producer();
        producer.setUri(URI);
        producer.setOperation(OPERATION);
        trace.getNodes().add(producer);

        EndpointRef ep = EndpointUtil.getSourceEndpoint(trace);

        assertNotNull(ep);
        assertEquals(new EndpointRef(URI, OPERATION, true), ep);
    }

    @Test
    public void testGetSourceEndpointNotClientComponentProducer() {
        // As this initial fragment does not have a single Producer node,
        // its endpoint ref will be based on the component's URI and operation
        Trace trace = new Trace();
        trace.setTraceId("1");
        trace.setFragmentId(trace.getTraceId());
        Component component = new Component();
        component.setUri(URI);
        component.setOperation(OPERATION);
        trace.getNodes().add(component);

        Producer producer = new Producer();
        producer.setUri("otheruri");
        producer.setOperation("otherop");
        component.getNodes().add(producer);

        EndpointRef ep = EndpointUtil.getSourceEndpoint(trace);

        assertNotNull(ep);
        assertEquals(new EndpointRef(URI, OPERATION, false), ep);
    }
}
