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
package org.hawkular.apm.processor.alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hawkular.apm.api.model.Property;
import org.junit.Test;

/**
 * @author gbrown
 */
public class EventTest {

    @Test
    public void testSingleValueProperty() {
        Property p1 = new Property("myprop", "1");
        Event evt = new Event();
        evt.initTagsFromProperties(Collections.singleton(p1));
        String tag = evt.getTags().get("myprop");
        assertNotNull(tag);
        assertEquals("1", tag);
    }

    @Test
    public void testMultiValueProperty() {
        Property p1 = new Property("myprop", "1");
        Property p2 = new Property("myprop", "2");
        Set<Property> properties = new HashSet<>();
        properties.add(p1);
        properties.add(p2);
        Event evt = new Event();
        evt.initTagsFromProperties(properties);
        String tag = evt.getTags().get("myprop");
        assertNotNull(tag);
        assertTrue("1,2".equals(tag) || "2,1".equals(tag));
    }

    @Test
    public void testNullValueProperty() {
        Property p = new Property("myprop", null);
        Event evt = new Event();
        evt.initTagsFromProperties(Collections.singleton(p));
        assertTrue(evt.getTags().isEmpty());
    }

}
