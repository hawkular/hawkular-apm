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
package org.hawkular.apm.server.processor.tracecompletiontime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.processor.tracecompletiontime.TraceCompletionInformation.Communication;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceCompletionInformationInitiatorTest {

    @Test
    public void testProcessSingleEmptyTrace() throws RetryAttemptException {
        Trace trace = new Trace();

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        assertNull(initiator.processOneToOne(null, trace));
    }

    @Test
    public void testProcessSingleNotInitialFragment() throws RetryAttemptException {
        Trace trace = new Trace().setTraceId("traceId").setFragmentId("anotherId");
        Consumer c = new Consumer();
        trace.getNodes().add(c);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        assertNull(initiator.processOneToOne(null, trace));
    }

    @Test
    public void testProcessSingleInitialFragmentConsumer() throws RetryAttemptException {
        Trace trace = new Trace();
        trace.setTraceId("traceId");
        trace.setFragmentId("traceId");
        trace.setTransaction("traceName");
        trace.setTimestamp(100000);

        Consumer c = new Consumer();
        c.setUri("uri");
        c.setTimestamp(1);
        c.setDuration(200000);
        c.getProperties().add(new Property(Constants.PROP_FAULT, "myFault"));
        c.setEndpointType("HTTP");

        trace.getNodes().add(c);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = initiator.processOneToOne(null, trace);

        assertNotNull(ci);
        assertEquals(1, ci.getCommunications().size());
        assertTrue(ci.getCommunications().get(0).getIds().contains("traceId:0"));

        assertEquals(trace.getTraceId(), ci.getCompletionTime().getId());
        assertEquals(trace.getTransaction(), ci.getCompletionTime().getTransaction());
        assertEquals(c.getEndpointType(), ci.getCompletionTime().getEndpointType());
        assertFalse(ci.getCompletionTime().isInternal());
        assertEquals(trace.getTimestamp(), ci.getCompletionTime().getTimestamp());
        assertEquals(c.getUri(), ci.getCompletionTime().getUri());
        assertEquals(200000, ci.getCompletionTime().getDuration());
        assertEquals(1, ci.getCompletionTime().getProperties(Constants.PROP_FAULT).size());
        assertEquals(c.getProperties(Constants.PROP_FAULT), ci.getCompletionTime().getProperties(Constants.PROP_FAULT));
    }

    @Test
    public void testProcessSingleInitialFragmentProducer() throws RetryAttemptException {
        Trace trace = new Trace();
        trace.setTraceId("traceId");
        trace.setFragmentId(trace.getTraceId());

        Producer p = new Producer();
        p.setUri("uri");
        p.addInteractionCorrelationId("pid");

        trace.getNodes().add(p);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = initiator.processOneToOne(null, trace);

        assertNotNull(ci);
        assertEquals(2, ci.getCommunications().size());
        assertTrue(ci.getCommunications().get(0).getIds().contains("traceId:0"));
        assertTrue(ci.getCommunications().get(1).getIds().contains("pid"));
        assertEquals(EndpointUtil.encodeClientURI(p.getUri()), ci.getCompletionTime().getUri());
    }

    @Test
    public void testProcessSingleInitialFragmentComponent() throws RetryAttemptException {
        Trace trace = new Trace();
        trace.setTraceId("traceId");
        trace.setFragmentId("traceId");
        trace.setTransaction("traceName");
        trace.setTimestamp(100000);

        Component c = new Component();
        c.setUri("uri");
        c.setTimestamp(1);
        c.setDuration(200000);
        c.getProperties().add(new Property(Constants.PROP_FAULT, "myFault"));

        trace.getNodes().add(c);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = initiator.processOneToOne(null, trace);

        assertNotNull(ci);
        assertEquals(1, ci.getCommunications().size());
        assertTrue(ci.getCommunications().get(0).getIds().contains("traceId:0"));

        assertEquals(trace.getTraceId(), ci.getCompletionTime().getId());
        assertEquals(trace.getTransaction(), ci.getCompletionTime().getTransaction());
        assertEquals(trace.getTimestamp(), ci.getCompletionTime().getTimestamp());
        assertEquals(c.getUri(), ci.getCompletionTime().getUri());
        assertEquals(200000, ci.getCompletionTime().getDuration());
        assertEquals(c.getProperties(Constants.PROP_FAULT), ci.getCompletionTime().getProperties(Constants.PROP_FAULT));
        assertEquals(1, ci.getCompletionTime().getProperties(Constants.PROP_FAULT).size());
    }

    @Test
    public void testProcessSingleInitialFragmentConsumerWithProducers() throws RetryAttemptException {
        Trace trace = new Trace();
        trace.setTraceId("traceId");
        trace.setFragmentId("traceId");
        trace.setTransaction("traceName");
        trace.setTimestamp(100);

        Consumer c = new Consumer();
        c.setUri("uri");

        trace.getNodes().add(c);

        Producer p1 = new Producer();
        p1.setUri("p1");
        p1.addInteractionCorrelationId("p1id");
        c.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("p2");
        p2.addInteractionCorrelationId("p2id");
        c.getNodes().add(p2);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = initiator.processOneToOne(null, trace);

        assertNotNull(ci);
        assertEquals(5, ci.getCommunications().size());

        for (Communication cm : ci.getCommunications()) {
            assertEquals(1, cm.getIds().size());
        }

        assertEquals("p1id", ci.getCommunications().get(2).getIds().get(0));
        assertEquals("p2id", ci.getCommunications().get(4).getIds().get(0));
    }
}
