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
package org.hawkular.apm.server.processor.tracecompletiontime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.InteractionNode;
import org.hawkular.apm.api.model.trace.Producer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceCompletionInformationUtilTest {

    @Test
    public void testInitialiseLinks() {
        TraceCompletionInformation ci = new TraceCompletionInformation();

        long fragmentBaseTime = 100000000;

        Consumer consumer = new Consumer();
        consumer.setBaseTime(fragmentBaseTime);

        Producer p1 = new Producer();
        p1.setBaseTime(200000000);
        p1.addInteractionCorrelationId("p1id");
        consumer.getNodes().add(p1);

        Component comp1 = new Component();
        comp1.setBaseTime(300000000);
        consumer.getNodes().add(comp1);

        Producer p2 = new Producer();
        p2.setBaseTime(400000000);
        comp1.getNodes().add(p2);

        Producer p3 = new Producer();
        p3.setBaseTime(450000000);
        p3.addControlFlowCorrelationId("p3id");
        p3.getDetails().put(InteractionNode.DETAILS_PUBLISH, "true");
        comp1.getNodes().add(p3);

        TraceCompletionInformationUtil.initialiseLinks(ci, fragmentBaseTime, consumer, new StringBuilder("trace1:0"));

        assertEquals(7, ci.getCommunications().size());

        assertTrue(ci.getCommunications().get(0).getIds().contains("trace1:0"));
        assertTrue(ci.getCommunications().get(0).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((consumer.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(0).getBaseDuration());
        assertTrue(ci.getCommunications().get(1).getIds().contains("trace1:0:0"));
        assertTrue(ci.getCommunications().get(1).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((p1.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(1).getBaseDuration());
        assertTrue(ci.getCommunications().get(2).getIds().contains("p1id"));
        assertFalse(ci.getCommunications().get(2).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((p1.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(2).getBaseDuration());
        assertTrue(ci.getCommunications().get(3).getIds().contains("trace1:0:1"));
        assertTrue(ci.getCommunications().get(3).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((comp1.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(3).getBaseDuration());
        assertTrue(ci.getCommunications().get(4).getIds().contains("trace1:0:1:0"));
        assertTrue(ci.getCommunications().get(4).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((p2.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(4).getBaseDuration());
        assertTrue(ci.getCommunications().get(5).getIds().contains("trace1:0:1:1"));
        assertTrue(ci.getCommunications().get(5).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((p3.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(5).getBaseDuration());
        assertTrue(ci.getCommunications().get(6).getIds().contains("p3id"));
        assertTrue(ci.getCommunications().get(6).isMultipleConsumers());
        assertEquals(TimeUnit.MILLISECONDS.convert((p3.getBaseTime() - fragmentBaseTime),
                TimeUnit.NANOSECONDS), ci.getCommunications().get(6).getBaseDuration());
    }

}
