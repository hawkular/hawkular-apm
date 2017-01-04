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
package org.hawkular.apm.api.model.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.hawkular.apm.api.model.config.instrumentation.Instrumentation;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CollectorConfigurationTest {

    @Test
    public void testMergeNoConflict() {
        CollectorConfiguration c1 = new CollectorConfiguration();
        c1.getInstrumentation().put("i1", new Instrumentation());

        CollectorConfiguration c2 = new CollectorConfiguration();
        c2.getInstrumentation().put("i2", new Instrumentation());

        c1.merge(c2, false);

        assertEquals(2, c1.getInstrumentation().size());
    }

    @Test
    public void testMergeConflictOverwrite() {
        CollectorConfiguration c1 = new CollectorConfiguration();
        c1.getInstrumentation().put("i1", new Instrumentation());

        Instrumentation inst = new Instrumentation();
        CollectorConfiguration c2 = new CollectorConfiguration();
        c2.getInstrumentation().put("i1", inst);

        c1.merge(c2, true);

        assertEquals(1, c1.getInstrumentation().size());
        assertEquals(inst, c1.getInstrumentation().get("i1"));
    }

    @Test
    public void testMergeConflictNoOverwrite() {
        CollectorConfiguration c1 = new CollectorConfiguration();
        c1.getInstrumentation().put("i1", new Instrumentation());

        Instrumentation inst = new Instrumentation();
        CollectorConfiguration c2 = new CollectorConfiguration();
        c2.getInstrumentation().put("i1", inst);

        try {
            c1.merge(c2, false);
            fail("Should have caused exception");
        } catch (IllegalArgumentException iae) {
            // Ignore
        }
    }

}
