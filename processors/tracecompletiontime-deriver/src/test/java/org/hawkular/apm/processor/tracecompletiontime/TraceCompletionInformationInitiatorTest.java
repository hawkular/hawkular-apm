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
package org.hawkular.apm.processor.tracecompletiontime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceCompletionInformationInitiatorTest {

    @Test
    public void testProcessSingleEmptyBtxn() {
        Trace trace = new Trace();

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        try {
            assertNull(initiator.processSingle(null, trace));
        } catch (Exception e) {
            fail("Failed: " + e);
        }
    }

    @Test
    public void testProcessSingleConsumerWithInteractionId() {
        Trace trace = new Trace();
        Consumer c = new Consumer();
        c.addInteractionId("myId");
        trace.getNodes().add(c);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        try {
            assertNull(initiator.processSingle(null, trace));
        } catch (Exception e) {
            fail("Failed: " + e);
        }
    }

    @Test
    public void testProcessSingleConsumerWithNoInteractionIdNoProducers() {
        Trace trace = new Trace();
        trace.setId("traceId");
        trace.setBusinessTransaction("traceName");
        trace.setStartTime(100);

        Consumer c = new Consumer();
        c.setUri("uri");
        c.setBaseTime(1);
        c.setDuration(200000000);
        c.setFault("myFault");
        c.setEndpointType("HTTP");

        trace.getNodes().add(c);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = null;

        try {
            ci = initiator.processSingle(null, trace);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ci);
        assertEquals(0, ci.getCommunications().size());

        assertEquals(trace.getId(), ci.getCompletionTime().getId());
        assertEquals(trace.getBusinessTransaction(), ci.getCompletionTime().getBusinessTransaction());
        assertEquals(c.getEndpointType(), ci.getCompletionTime().getEndpointType());
        assertFalse(ci.getCompletionTime().isInternal());
        assertEquals(trace.getStartTime(), ci.getCompletionTime().getTimestamp());
        assertEquals(c.getUri(), ci.getCompletionTime().getUri());
        assertEquals(200, ci.getCompletionTime().getDuration());
        assertEquals(c.getFault(), ci.getCompletionTime().getFault());
    }

    @Test
    public void testProcessSingleComponentNoProducers() {
        Trace trace = new Trace();
        trace.setId("traceId");
        trace.setBusinessTransaction("traceName");
        trace.setStartTime(100);

        Component c = new Component();
        c.setUri("uri");
        c.setBaseTime(1);
        c.setDuration(200000000);
        c.setFault("myFault");

        trace.getNodes().add(c);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = null;

        try {
            ci = initiator.processSingle(null, trace);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ci);
        assertEquals(0, ci.getCommunications().size());

        assertEquals(trace.getId(), ci.getCompletionTime().getId());
        assertEquals(trace.getBusinessTransaction(), ci.getCompletionTime().getBusinessTransaction());
        assertEquals(trace.getStartTime(), ci.getCompletionTime().getTimestamp());
        assertEquals(c.getUri(), ci.getCompletionTime().getUri());
        assertEquals(200, ci.getCompletionTime().getDuration());
        assertEquals(c.getFault(), ci.getCompletionTime().getFault());
    }

    @Test
    public void testProcessSingleConsumerWithNoInteractionIdWithProducers() {
        Trace trace = new Trace();
        trace.setId("traceId");
        trace.setBusinessTransaction("traceName");
        trace.setStartTime(100);

        Consumer c = new Consumer();
        c.setUri("uri");

        trace.getNodes().add(c);

        Producer p1 = new Producer();
        p1.setUri("p1");
        p1.addInteractionId("p1id");
        c.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("p2");
        p2.addInteractionId("p2id");
        c.getNodes().add(p2);

        TraceCompletionInformationInitiator initiator = new TraceCompletionInformationInitiator();

        TraceCompletionInformation ci = null;

        try {
            ci = initiator.processSingle(null, trace);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ci);
        assertEquals(2, ci.getCommunications().size());

        assertEquals(1, ci.getCommunications().get(0).getIds().size());
        assertEquals(1, ci.getCommunications().get(1).getIds().size());
        assertEquals("p1id", ci.getCommunications().get(0).getIds().get(0));
        assertEquals("p2id", ci.getCommunications().get(1).getIds().get(0));
    }
}
