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
package org.hawkular.btm.processor.communicationdetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.events.CommunicationDetails;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationDetailsDeriverTest {

    private static Cache<String, ProducerInfo> cache = new DefaultCacheManager().getCache();

    /**  */
    private static final String BTXN_NAME = "btxnName";

    @Test
    public void testInitialise() {
        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(System.currentTimeMillis());

        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setBaseTime(System.nanoTime());

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        btxn1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(System.nanoTime());

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        deriver.initialise(btxns);

        assertTrue(deriver.getProducerinfoCache().containsKey("pid1"));
        assertFalse(deriver.getProducerinfoCache().containsKey("cid1"));
    }

    @Test
    public void testInitialiseClientFragment() {
        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(System.currentTimeMillis());

        btxns.add(btxn1);

        Component c1 = new Component();
        btxn1.getNodes().add(c1);

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

        deriver.initialise(btxns);

        assertTrue(deriver.getProducerinfoCache().containsKey("pid1"));
        assertTrue(deriver.getProducerinfoCache().containsKey("pid2"));

        ProducerInfo pi1 = deriver.getProducerinfoCache().get("pid1");
        ProducerInfo pi2 = deriver.getProducerinfoCache().get("pid2");

        assertEquals(CommunicationDetailsDeriver.CLIENT_PREFIX+"p1", pi1.getOriginUri());

        // Check that producer info 2 has same origin URI as p1, as they
        // are from the same fragment (without a consumer) so are being identified
        // as a client of the first producer URI found (see HWKBTM-353).
        assertEquals(CommunicationDetailsDeriver.CLIENT_PREFIX+"p1", pi2.getOriginUri());
    }

    @Test
    public void testInitialiseServerFragment() {
        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(System.currentTimeMillis());

        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("consumerURI");
        btxn1.getNodes().add(c1);

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

        deriver.initialise(btxns);

        assertTrue(deriver.getProducerinfoCache().containsKey("pid1"));
        assertTrue(deriver.getProducerinfoCache().containsKey("pid2"));

        ProducerInfo pi1 = deriver.getProducerinfoCache().get("pid1");
        ProducerInfo pi2 = deriver.getProducerinfoCache().get("pid2");

        assertEquals("consumerURI", pi1.getOriginUri());
        assertEquals("consumerURI", pi2.getOriginUri());
    }

    @Test
    public void testProcessSingleNoProducer() {
        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxns.add(btxn1);

        Consumer c1 = new Consumer();

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        btxn1.getNodes().add(c1);

        Producer p1 = new Producer();

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        try {
            deriver.processSingle(btxn1);
            fail("Should have thrown exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testProcessSingle() {
        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);

        List<BusinessTransaction> btxns1 = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(System.currentTimeMillis());

        btxns1.add(btxn1);

        btxn1.setName(BTXN_NAME);
        btxn1.setId("btxn1");
        btxn1.setHostName("host1");
        btxn1.setHostAddress("addr1");

        Consumer c1 = new Consumer();
        c1.setUri("FirstURI");
        c1.setBaseTime(System.nanoTime());

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("cid1");
        c1.getCorrelationIds().add(cid1);

        btxn1.getNodes().add(c1);

        Producer p1 = new Producer();
        p1.setBaseTime(System.nanoTime());
        p1.setDuration(2000);

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        c1.getNodes().add(p1);

        List<BusinessTransaction> btxns2 = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxns2.add(btxn2);

        btxn2.setName(BTXN_NAME);
        btxn2.setId("btxn2");
        btxn2.setHostName("host2");
        btxn2.setHostAddress("addr2");
        btxn2.getProperties().put("prop1", "value1");

        Consumer c2 = new Consumer();
        c2.setUri("SecondURI");
        c2.setDuration(1200);

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        btxn2.getNodes().add(c2);

        CommunicationDetails details = null;
        try {
            deriver.initialise(btxns1);
            deriver.initialise(btxns2);
            details = deriver.processSingle(btxn2);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertNotNull(details);

        assertEquals("pid1", details.getId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals("FirstURI", details.getOriginUri());
        assertEquals("SecondURI", details.getUri());
        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.getProperties().containsKey("prop1"));
        assertEquals("btxn1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("btxn2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());

        long timestamp = btxn1.getStartTime() + TimeUnit.MILLISECONDS.convert(p1.getBaseTime() -
                c1.getBaseTime(), TimeUnit.NANOSECONDS);
        assertEquals(timestamp, details.getTimestamp());
    }

    @Test
    public void testProcessSingleWithClient() {
        CommunicationDetailsDeriver deriver = new CommunicationDetailsDeriver();
        deriver.setProducerInfoCache(cache);

        List<BusinessTransaction> btxns1 = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(System.currentTimeMillis());

        btxns1.add(btxn1);

        btxn1.setName(BTXN_NAME);
        btxn1.setId("btxn1");
        btxn1.setHostName("host1");
        btxn1.setHostAddress("addr1");

        Producer p1 = new Producer();
        p1.setUri("TheURI");
        p1.setBaseTime(System.nanoTime());
        p1.setDuration(2000);

        CorrelationIdentifier pid1 = new CorrelationIdentifier();
        pid1.setScope(Scope.Interaction);
        pid1.setValue("pid1");
        p1.getCorrelationIds().add(pid1);

        btxn1.getNodes().add(p1);

        List<BusinessTransaction> btxns2 = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxns2.add(btxn2);

        btxn2.setName(BTXN_NAME);
        btxn2.setId("btxn2");
        btxn2.setHostName("host2");
        btxn2.setHostAddress("addr2");
        btxn2.getProperties().put("prop1", "value1");

        Consumer c2 = new Consumer();
        c2.setUri("TheURI");
        c2.setDuration(1200);

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("pid1");
        c2.getCorrelationIds().add(cid2);

        btxn2.getNodes().add(c2);

        CommunicationDetails details = null;
        try {
            deriver.initialise(btxns1);
            deriver.initialise(btxns2);
            details = deriver.processSingle(btxn2);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertNotNull(details);

        assertEquals("pid1", details.getId());
        assertEquals(BTXN_NAME, details.getBusinessTransaction());
        assertEquals(CommunicationDetailsDeriver.CLIENT_PREFIX+"TheURI", details.getOriginUri());
        assertEquals("TheURI", details.getUri());
        assertTrue(c2.getDuration() == details.getConsumerDuration());
        assertTrue(p1.getDuration() == details.getProducerDuration());
        assertTrue(400 == details.getLatency());
        assertTrue(details.getProperties().containsKey("prop1"));
        assertEquals("btxn1", details.getSourceFragmentId());
        assertEquals("host1", details.getSourceHostName());
        assertEquals("addr1", details.getSourceHostAddress());
        assertEquals("btxn2", details.getTargetFragmentId());
        assertEquals("host2", details.getTargetHostName());
        assertEquals("addr2", details.getTargetHostAddress());
    }

}
