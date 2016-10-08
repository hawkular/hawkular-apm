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
package org.hawkular.apm.server.processor.fragmentcompletiontime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Trace;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FragmentCompletionTimeDeriverTest {

    @Test
    public void testProcessSingle() {
        Trace trace = new Trace();
        trace.setId("btxnId");
        trace.setBusinessTransaction("btxnName");
        trace.setTimestamp(100000);

        Consumer c = new Consumer();
        c.setUri("uri");
        c.setTimestamp(1);
        c.setDuration(200000);
        c.getProperties().add(new Property(Constants.PROP_FAULT, "myFault"));
        c.setEndpointType("HTTP");

        trace.getNodes().add(c);

        FragmentCompletionTimeDeriver deriver = new FragmentCompletionTimeDeriver();

        CompletionTime ct = null;

        try {
            ct = deriver.processOneToOne(null, trace);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ct);

        assertEquals(trace.getId(), ct.getId());
        assertEquals(trace.getBusinessTransaction(), ct.getBusinessTransaction());
        assertEquals(c.getEndpointType(), ct.getEndpointType());
        assertFalse(ct.isInternal());
        assertEquals(trace.getTimestamp(), ct.getTimestamp());
        assertEquals(c.getUri(), ct.getUri());
        assertEquals(200000, ct.getDuration());
        assertEquals(c.getProperties(Constants.PROP_FAULT), ct.getProperties(Constants.PROP_FAULT));
    }

    @Test
    public void testProcessSingleConsumerInternal() {
        Trace trace = new Trace();
        trace.setId("btxnId");
        trace.setBusinessTransaction("btxnName");
        trace.setTimestamp(100);

        Consumer c = new Consumer();
        c.setUri("uri");
        c.setTimestamp(1);
        c.setDuration(200000000);

        trace.getNodes().add(c);

        FragmentCompletionTimeDeriver deriver = new FragmentCompletionTimeDeriver();

        CompletionTime ct = null;

        try {
            ct = deriver.processOneToOne(null, trace);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ct);
        assertNull(ct.getEndpointType());
        assertTrue(ct.isInternal());
    }

}
