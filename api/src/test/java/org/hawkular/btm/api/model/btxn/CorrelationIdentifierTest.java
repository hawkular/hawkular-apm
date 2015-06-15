/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.model.btxn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CorrelationIdentifierTest {

    @Test
    public void testIsOverlapNoDurationsDiffBaseTimes() {
        assertFalse("Base times/durations shouldn't overlap",
                CorrelationIdentifier.isOverlap(100, 0, 200, 0));
    }

    @Test
    public void testIsOverlapWithDurationsDiffBaseTimes() {
        assertFalse("Base times/durations shouldn't overlap",
                CorrelationIdentifier.isOverlap(100, 50, 200, 50));
    }

    @Test
    public void testIsOverlapBT1WithDurationOverlapBT2NoDuration() {
        assertTrue("Base times/durations should overlap",
                CorrelationIdentifier.isOverlap(100, 150, 200, 0));
    }

    @Test
    public void testIsOverlapBT1WithDurationOverlapBT2WithDuration() {
        assertTrue("Base times/durations should overlap",
                CorrelationIdentifier.isOverlap(100, 150, 200, 100));
    }

    @Test
    public void testIsOverlapBT1NoDurationOverlapBT2WithDuration() {
        assertTrue("Base times/durations should overlap",
                CorrelationIdentifier.isOverlap(250, 0, 200, 100));
    }

    @Test
    public void testMatchGlobalSameValueNoDuration() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Global);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Global);
        id2.setValue("Test");

        assertTrue("Global ids should match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchGlobalDiffValueNoDuration() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Global);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Global);
        id2.setValue("Other");

        assertFalse("Global ids should NOT match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchGlobalSameValueWithDurationOverlap() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Global);
        id1.setValue("Test");
        id1.setDuration(200);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Global);
        id2.setValue("Test");
        id2.setDuration(200);

        assertTrue("Global ids should match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchGlobalSameValueWithDurationNoOverlap() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Global);
        id1.setValue("Test");
        id1.setDuration(20);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Global);
        id2.setValue("Test");
        id2.setDuration(20);

        assertFalse("Global ids should NOT match as no overlap", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchInteractionSameValueNoDuration() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("Test");

        assertTrue("Interaction ids should match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchInteractionDiffValueNoDuration() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("Other");

        assertFalse("Interaction ids should NOT match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchInteractionSameValueWithDurationOverlap() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("Test");
        id1.setDuration(200);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("Test");
        id2.setDuration(200);

        assertTrue("Interaction ids should match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchInteractionSameValueWithDurationNoOverlap() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("Test");
        id1.setDuration(20);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("Test");
        id2.setDuration(20);

        assertFalse("Interaction ids should NOT match as no overlap", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchLocalSameValueNoDuration() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Local);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Local);
        id2.setValue("Test");

        assertTrue("Local ids should match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchLocalDiffValueNoDuration() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Local);
        id1.setValue("Test");

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Local);
        id2.setValue("Other");

        assertFalse("Local ids should NOT match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchLocalSameValueWithDurationOverlap() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Local);
        id1.setValue("Test");
        id1.setDuration(200);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Local);
        id2.setValue("Test");
        id2.setDuration(200);

        assertTrue("Local ids should match", id1.match(100, id2, 200));
    }

    @Test
    public void testMatchLocalSameValueWithDurationNoOverlap() {
        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Local);
        id1.setValue("Test");
        id1.setDuration(20);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Local);
        id2.setValue("Test");
        id2.setDuration(20);

        assertFalse("Local ids should NOT match as no overlap", id1.match(100, id2, 200));
    }

}
