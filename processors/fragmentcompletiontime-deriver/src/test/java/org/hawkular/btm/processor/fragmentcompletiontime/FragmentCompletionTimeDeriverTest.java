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
package org.hawkular.btm.processor.fragmentcompletiontime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FragmentCompletionTimeDeriverTest {

    @Test
    public void testProcessSingle() {
        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setId("btxnId");
        btxn.setName("btxnName");
        btxn.setStartTime(100);

        Consumer c = new Consumer();
        c.setUri("uri");
        c.setBaseTime(1);
        c.setDuration(200000000);
        c.setFault("myFault");
        c.setEndpointType("HTTP");

        btxn.getNodes().add(c);

        FragmentCompletionTimeDeriver deriver = new FragmentCompletionTimeDeriver();

        CompletionTime ct = null;

        try {
            ct = deriver.processSingle(null, btxn);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ct);

        assertEquals(btxn.getId(), ct.getId());
        assertEquals(btxn.getName(), ct.getBusinessTransaction());
        assertEquals(c.getEndpointType(), ct.getEndpointType());
        assertFalse(ct.isInternal());
        assertEquals(btxn.getStartTime(), ct.getTimestamp());
        assertEquals(c.getUri(), ct.getUri());
        assertEquals(200, ct.getDuration());
        assertEquals(c.getFault(), ct.getFault());
    }

    @Test
    public void testProcessSingleConsumerInternal() {
        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setId("btxnId");
        btxn.setName("btxnName");
        btxn.setStartTime(100);

        Consumer c = new Consumer();
        c.setUri("uri");
        c.setBaseTime(1);
        c.setDuration(200000000);

        btxn.getNodes().add(c);

        FragmentCompletionTimeDeriver deriver = new FragmentCompletionTimeDeriver();

        CompletionTime ct = null;

        try {
            ct = deriver.processSingle(null, btxn);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(ct);
        assertNull(ct.getEndpointType());
        assertTrue(ct.isInternal());
    }

}
