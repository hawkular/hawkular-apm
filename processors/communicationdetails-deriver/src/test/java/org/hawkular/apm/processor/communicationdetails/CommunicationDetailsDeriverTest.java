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
package org.hawkular.apm.processor.communicationdetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationDetailsDeriverTest {

    /**  */
    private static final String BTXN_NAME = "traceName";

    @Test
    public void testInitialise() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(System.currentTimeMillis());

        traces.add(trace1);

        Consumer c1 = new Consumer();
        c1.setBaseTime(System.nanoTime());

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        try {
            deriver.initialise(null, traces);
        } catch (RetryAttemptException e) {
            fail("Failed: "+e);
        }

        assertNotNull(deriver.getProducerInfoCache().get(null, "pid1"));
        assertNull(deriver.getProducerInfoCache().get(null, "cid1"));
    }

    @Test
    public void testInitialiseClientFragment() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(System.currentTimeMillis());

        traces.add(trace1);

        Component c1 = new Component();
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("p1");
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("p2");
        p2.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid2 = new CorrelationIdentifier();
        pid2.setScope(Scope.Interaction);
        pid2.setValue("pid2");
        p2.getCorrelationIds().add(pid2);

        c1.getNodes().add(p2);

        try {
            deriver.initialise(null, traces);
        } catch (RetryAttemptException e) {
            fail("Failed: "+e);
        }

        ProducerInfo pi1 = deriver.getProducerInfoCache().get(null, "pid1");
        ProducerInfo pi2 = deriver.getProducerInfoCache().get(null, "pid2");

        assertNotNull(pi1);
        assertNotNull(pi2);

        assertEquals(Constants.URI_CLIENT_PREFIX + "p1", pi1.getSourceUri());

        // Check that producer info 2 has same origin URI as p1, as they
        // are from the same fragment (without a consumer) so are being identified
        // as a client of the first producer URI found (see HWKBTM-353).
        assertEquals(Constants.URI_CLIENT_PREFIX + "p1", pi2.getSourceUri());
    }

    @Test
    public void testInitialiseServerFragment() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(System.currentTimeMillis());

        traces.add(trace1);

        Consumer c1 = new Consumer();
        c1.setUri("consumerURI");
        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setUri("p1");
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        Producer p2 = new Producer();
        p2.setUri("p2");
        p2.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid2 = new CorrelationIdentifier();
        pid2.setScope(Scope.Interaction);
        pid2.setValue("pid2");
        p2.getCorrelationIds().add(pid2);

        c1.getNodes().add(p2);

        try {
            deriver.initialise(null, traces);
        } catch (RetryAttemptException e) {
            fail("Failed: "+e);
        }

        ProducerInfo pi1 = deriver.getProducerInfoCache().get(null, "pid1");
        ProducerInfo pi2 = deriver.getProducerInfoCache().get(null, "pid2");

        assertNotNull(pi1);
        assertNotNull(pi2);

        assertEquals("consumerURI", pi1.getSourceUri());
        assertEquals("consumerURI", pi2.getSourceUri());
    }

    @Test
    public void testProcessSingleNoProducer() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        traces.add(trace1);

        Consumer c1 = new Consumer();

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        try {
            deriver.processOneToOne(null, trace1);
            fail("Should have thrown exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testProcessSingle() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");
        trace1.setPrincipal("p1");
        trace1.getProperties().add(new Property("prop1", "value1"));

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        traces2.add(trace2);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.setPrincipal("p1");
        trace2.getProperties().add(new Property("prop2", "value2"));

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        CommunicationDetails details = null;
        try {
            deriver.initialise(null, traces1);
            deriver.initialise(null, traces2);
            details = deriver.processOneToOne(null, trace2);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertNotNull(details);

        assertEquals("pid1", details.getId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals("FirstURI", details.getSource());
        assertEquals("SecondURI", details.getTarget());

        assertFalse(details.isMultiConsumer());

        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.hasProperty("prop1"));
        assertTrue(details.hasProperty("prop2"));
        assertEquals("trace1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("trace2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
        assertEquals("p1", details.getPrincipal());

        long timestamp = trace1.getStartTime() + TimeUnit.MILLISECONDS.convert(p1.getBaseTime() -
                c1.getBaseTime(), TimeUnit.NANOSECONDS);
        assertEquals(timestamp, details.getTimestamp());

        assertEquals(999599, details.getTimestampOffset());
    }

    @Test
    public void testProcessSingleMultiConsumer() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(1000000);

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(0);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        trace1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(1000000);
        p1.setDuration(2000000000);
        p1.getDetails().put(Producer.DETAILS_PUBLISH, "true");

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        trace2.setStartTime(2000000);

        traces2.add(trace2);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.getProperties().add(new Property("prop1", "value1"));

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200000000);
        c2.getDetails().put(Consumer.DETAILS_PUBLISH, "true");

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        CommunicationDetails details = null;
        try {
            deriver.initialise(null, traces1);
            deriver.initialise(null, traces2);
            details = deriver.processOneToOne(null, trace2);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertNotNull(details);

        assertTrue(details.isMultiConsumer());
    }

    @Test
    public void testProcessSingleWithClient() {
        TestProducerInfoCache cache=new TestProducerInfoCache();

        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);
        deriver.init();

        List<Trace> traces1 = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setStartTime(System.currentTimeMillis());

        traces1.add(trace1);

        trace1.setBusinessTransaction(BTXN_NAME);
        trace1.setId("trace1");
        trace1.setHostName("host1");
        trace1.setHostAddress("addr1");
        trace1.setPrincipal("p1");

        Producer p1 = new Producer();
        p1.setUri("TheURI");
        p1.setBaseTime(System.nanoTime());
        p1.setDuration(2000000000);

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        trace1.getNodes().add(p1);

        List<Trace> traces2 = new ArrayList<Trace>();

        Trace trace2 = new Trace();
        traces2.add(trace2);

        trace2.setBusinessTransaction(BTXN_NAME);
        trace2.setId("trace2");
        trace2.setHostName("host2");
        trace2.setHostAddress("addr2");
        trace2.setPrincipal("p1");
        trace2.getProperties().add(new Property("prop1", "value1"));

        Consumer c2 = new Consumer();
        c2.setUri("TheURI");
        c2.setDuration(1200000000);

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        trace2.getNodes().add(c2);

        CommunicationDetails details = null;
        try {
            deriver.initialise(null, traces1);
            deriver.initialise(null, traces2);
            details = deriver.processOneToOne(null, trace2);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertNotNull(details);

        assertEquals("pid1", details.getId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals(Constants.URI_CLIENT_PREFIX + "TheURI", details.getSource());
        assertEquals("TheURI", details.getTarget());
        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.hasProperty("prop1"));
        assertEquals("trace1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("trace2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
        assertEquals("p1", details.getPrincipal());
    }

}
