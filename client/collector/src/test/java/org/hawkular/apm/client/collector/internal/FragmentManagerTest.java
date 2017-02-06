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
package org.hawkular.apm.client.collector.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * @author gbrown
 */
public class FragmentManagerTest {

    @Test
    public void testGetSameBuilder() {
        FragmentManager manager = new FragmentManager();

        FragmentBuilder builder1 = manager.getFragmentBuilder();

        FragmentBuilder builder2 = manager.getFragmentBuilder();

        assertEquals("Should be same builder", builder1, builder2);
    }

    @Test
    public void testGetDifferentBuilder() {
        FragmentManager manager = new FragmentManager();

        FragmentBuilder builder1 = manager.getFragmentBuilder();

        manager.clear();

        FragmentBuilder builder2 = manager.getFragmentBuilder();

        assertNotEquals("Should NOT be same builder", builder1, builder2);
    }

}
