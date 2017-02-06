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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author gbrown
 */
public class NodeTest {

    @Test
    public void testEndTime() {
        Consumer node = new Consumer();
        node.setTimestamp(1000);
        node.setDuration(500);

        assertEquals(1500, node.endTime());
    }

    @Test
    public void testCompletedTime() {
        Consumer node1 = new Consumer();
        node1.setTimestamp(1000);
        node1.setDuration(500);

        Producer node2 = new Producer();
        node2.setTimestamp(1200);
        node2.setDuration(800);
        node1.getNodes().add(node2);

        assertEquals(2000, node1.completedTime());
    }

    @Test
    public void testCompletedDuration() {
        Consumer node1 = new Consumer();
        node1.setTimestamp(1000);
        node1.setDuration(500);

        Producer node2 = new Producer();
        node2.setTimestamp(1200);
        node2.setDuration(800);
        node1.getNodes().add(node2);

        assertEquals(1000, node1.completedDuration());
    }

}
