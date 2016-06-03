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
package org.hawkular.apm.processor.btxncompletiontime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BTxnCompletionInformationProcessorTest {

    @Test
    public void testProcessSingleNoCommns() {
        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();

        BTxnCompletionInformation info = new BTxnCompletionInformation();
        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNull(result);
    }

    @Test
    public void testProcessSingleExpireComms() {
        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();

        BTxnCompletionInformation info = new BTxnCompletionInformation();
        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() - 10);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertTrue(result.getCommunications().isEmpty());
    }

    @Test
    public void testProcessSingleP2PNoCommsDetails() {
        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(new TestCommunicationDetailsCache());

        BTxnCompletionInformation info = new BTxnCompletionInformation();
        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() + 60000);

        List<String> ids = new ArrayList<String>();
        ids.add("id1");
        c1.setIds(ids);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(1, result.getCommunications().size());
    }

    @Test
    public void testProcessSingleP2PWithOneCommsDetailsDefiningDuration() {
        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(new TestCommunicationDetailsCache());

        // Add comms details for test
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("id1");
        cd1.setLatency(40);
        cd1.setTargetFragmentDuration(370);
        cd1.getProperties().put("prop2", "value2");

        cds.add(cd1);

        processor.getCommunicationDetailsCache().store(null, cds);

        BTxnCompletionInformation info = new BTxnCompletionInformation();

        CompletionTime ct = new CompletionTime();
        ct.setDuration(157);        // Current duration to date
        ct.getProperties().put("prop1", "value1");
        info.setCompletionTime(ct);

        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() + 60000);
        c1.setBaseDuration(111);    // The duration at the point the producer sends the communication

        List<String> ids = new ArrayList<String>();
        ids.add("id1");
        c1.setIds(ids);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(0, result.getCommunications().size());

        assertEquals(111 + 40 + 370, info.getCompletionTime().getDuration());
        assertEquals(2, result.getCompletionTime().getProperties().size());
        assertTrue(result.getCompletionTime().getProperties().containsKey("prop1"));
        assertTrue(result.getCompletionTime().getProperties().containsKey("prop2"));
    }

    @Test
    public void testProcessSingleP2PWithOneCommsDetailsNotDefiningDuration() {
        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(new TestCommunicationDetailsCache());

        // Add comms details for test
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("id1");
        cd1.setLatency(40);
        cd1.setTargetFragmentDuration(370);

        cds.add(cd1);

        processor.getCommunicationDetailsCache().store(null, cds);

        BTxnCompletionInformation info = new BTxnCompletionInformation();

        CompletionTime ct = new CompletionTime();
        ct.setDuration(560);        // Current duration to date - will be greater than the resulting comms
        info.setCompletionTime(ct);

        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() + 60000);
        c1.setBaseDuration(111);    // The duration at the point the producer sends the communication

        List<String> ids = new ArrayList<String>();
        ids.add("id1");
        c1.setIds(ids);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(0, result.getCommunications().size());

        assertEquals(560, info.getCompletionTime().getDuration());
    }

    @Test
    public void testProcessSingleP2PWithTwoSequentialCommsDetailsDefiningDuration1() {
        // This version of the test only processes the first communication details,
        // as the second is not available at the time - see next test for version
        // that evaluates both in the same step

        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(new TestCommunicationDetailsCache());

        // Add comms details for test
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("id1");
        cd1.setLatency(40);
        cd1.setTargetFragmentDuration(370);

        CommunicationDetails.Outbound ob1 = new CommunicationDetails.Outbound();
        ob1.getIds().add("id2");
        ob1.setProducerOffset(12);
        cd1.getOutbound().add(ob1);

        cds.add(cd1);

        processor.getCommunicationDetailsCache().store(null, cds);

        BTxnCompletionInformation info = new BTxnCompletionInformation();

        CompletionTime ct = new CompletionTime();
        ct.setDuration(157);        // Current duration to date
        info.setCompletionTime(ct);

        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() + 60000);
        c1.setBaseDuration(111);    // The duration at the point the producer sends the communication

        List<String> ids = new ArrayList<String>();
        ids.add("id1");
        c1.setIds(ids);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(1, result.getCommunications().size());

        assertEquals(111 + 40 + 370, info.getCompletionTime().getDuration());
    }

    @Test
    public void testProcessSingleP2PWithTwoSequentialCommsDetailsDefiningDuration2() {
        // This version evaluates both communications in the same step, as opposed
        // to only the first (see previous test)

        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(new TestCommunicationDetailsCache());

        // Add comms details for test
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("id1");
        cd1.setLatency(40);
        cd1.setTargetFragmentDuration(370);

        CommunicationDetails.Outbound ob1 = new CommunicationDetails.Outbound();
        ob1.getIds().add("id2");
        ob1.setProducerOffset(12);
        cd1.getOutbound().add(ob1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setId("id2");
        cd2.setLatency(33);
        cd2.setTargetFragmentDuration(345);

        cds.add(cd1);
        cds.add(cd2);

        processor.getCommunicationDetailsCache().store(null, cds);

        BTxnCompletionInformation info = new BTxnCompletionInformation();

        CompletionTime ct = new CompletionTime();
        ct.setDuration(157);        // Current duration to date
        info.setCompletionTime(ct);

        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() + 60000);
        c1.setBaseDuration(111);    // The duration at the point the producer sends the communication

        List<String> ids = new ArrayList<String>();
        ids.add("id1");
        c1.setIds(ids);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(0, result.getCommunications().size());

        // Base duration + id1 latency + id2 producer offset in next fragment + id2 latency + id2's target
        // fragment duration
        assertEquals(111 + 40 + 12 + 33 + 345, info.getCompletionTime().getDuration());
    }

    @Test
    public void testProcessSingleP2PWithTwoConcurrentCommsDetailsDefiningDuration() {
        BTxnCompletionInformationProcessor processor = new BTxnCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(new TestCommunicationDetailsCache());

        // Add comms details for test
        List<CommunicationDetails> cds = new ArrayList<CommunicationDetails>();

        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("id1");
        cd1.setLatency(40);
        cd1.setTargetFragmentDuration(370);

        cds.add(cd1);

        CommunicationDetails.Outbound ob1 = new CommunicationDetails.Outbound();
        ob1.getIds().add("id2");
        ob1.setProducerOffset(12);
        cd1.getOutbound().add(ob1);

        CommunicationDetails.Outbound ob2 = new CommunicationDetails.Outbound();
        ob2.getIds().add("id3");
        ob2.setProducerOffset(17);
        cd1.getOutbound().add(ob2);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setId("id2");
        cd2.setLatency(33);
        cd2.setTargetFragmentDuration(345);

        cds.add(cd2);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setId("id3");
        cd3.setLatency(44);
        cd3.setTargetFragmentDuration(371);

        cds.add(cd3);

        processor.getCommunicationDetailsCache().store(null, cds);

        BTxnCompletionInformation info = new BTxnCompletionInformation();

        CompletionTime ct = new CompletionTime();
        ct.setDuration(157);        // Current duration to date
        info.setCompletionTime(ct);

        BTxnCompletionInformation.Communication c1 = new BTxnCompletionInformation.Communication();
        c1.setExpire(System.currentTimeMillis() + 60000);
        c1.setBaseDuration(111);    // The duration at the point the producer sends the communication

        List<String> ids = new ArrayList<String>();
        ids.add("id1");
        c1.setIds(ids);
        info.getCommunications().add(c1);

        BTxnCompletionInformation result = null;

        try {
            result = processor.processSingle(null, info);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(0, result.getCommunications().size());

        // Base duration + id1 latency + id3 producer offset in next fragment + id3 latency + id3's target
        // fragment duration
        assertEquals(111 + 40 + 17 + 44 + 371, info.getCompletionTime().getDuration());
    }

}
