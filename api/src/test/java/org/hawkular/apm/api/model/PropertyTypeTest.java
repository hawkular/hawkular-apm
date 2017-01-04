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
package org.hawkular.apm.api.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author gbrown
 */
public class PropertyTypeTest {

    @Test
    public void testNumber() {
        assertEquals(PropertyType.Number, PropertyType.of(5));
        assertEquals(PropertyType.Number, PropertyType.of(12.6));
        assertEquals(PropertyType.Number, PropertyType.of(new Float(12)));
        assertEquals(PropertyType.Number, PropertyType.of(new Double(12)));
        assertEquals(PropertyType.Number, PropertyType.of(new Integer(12)));
    }

    @Test
    public void testBoolean() {
        assertEquals(PropertyType.Boolean, PropertyType.of(true));
        assertEquals(PropertyType.Boolean, PropertyType.of(new Boolean(true)));
    }

    @Test
    public void testBinary() {
        assertEquals(PropertyType.Binary, PropertyType.of("hello".getBytes()));
    }

    @Test
    public void testText() {
        assertEquals(PropertyType.Text, PropertyType.of("hello"));
        assertEquals(PropertyType.Text, PropertyType.of(new StringBuilder("hello")));
    }

}
