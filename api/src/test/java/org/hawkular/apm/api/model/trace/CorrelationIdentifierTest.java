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
package org.hawkular.apm.api.model.trace;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CorrelationIdentifierTest {

    @Test
    public void testMatchInteractionSameValue() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("Test");

        assertTrue("Interaction ids should match", id1.equals(id2));
    }

    @Test
    public void testMatchInteractionDiffValue() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("Other");

        assertFalse("Interaction ids should NOT match", id1.equals(id2));
    }

}
