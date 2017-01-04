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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.hawkular.apm.api.model.events.CompletionTime;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceCompletionDeriverTest {

    @Test
    public void testProcessSingleWithCommunications() {
        TraceCompletionInformation ci = new TraceCompletionInformation();
        ci.getCommunications().add(new TraceCompletionInformation.Communication());

        TraceCompletionDeriver deriver = new TraceCompletionDeriver();

        CompletionTime result = null;

        try {
            result = deriver.processOneToOne(null, ci);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNull(result);
    }

    @Test
    public void testProcessSingleWithNoCommunications() {
        TraceCompletionInformation ci = new TraceCompletionInformation();
        CompletionTime ct = new CompletionTime();
        ct.setId("ctid");
        ci.setCompletionTime(ct);

        TraceCompletionDeriver deriver = new TraceCompletionDeriver();

        CompletionTime result = null;

        try {
            result = deriver.processOneToOne(null, ci);
        } catch (Exception e) {
            fail("Failed: " + e);
        }

        assertNotNull(result);
        assertEquals(ct.getId(), result.getId());
    }

}
