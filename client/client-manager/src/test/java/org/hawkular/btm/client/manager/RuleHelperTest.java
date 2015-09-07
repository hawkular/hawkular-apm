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
package org.hawkular.btm.client.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

/**
 * @author gbrown
 */
public class RuleHelperTest {

    @Test
    public void testHeaders() {
        RuleHelper helper = new RuleHelper(null);

        TestHeadersObject target = new TestHeadersObject();
        target.getProperties().put("hello", "world");

        Map<String, String> headers = helper.getHeaders(TestHeadersObject.class.getName(), target);

        assertNotNull(headers);
        assertEquals(1, headers.size());
        assertEquals("world", headers.get("hello"));
    }

    @Test
    public void testCast() {
        RuleHelper helper = new RuleHelper(null);

        String str=helper.<String>cast("hello", String.class);

        assertEquals("hello", str);
    }

    @Test
    public void testCastIncorrect() {
        RuleHelper helper = new RuleHelper(null);

        TestObject1 to1=new TestObject1();

        TestObject2 to2=helper.<TestObject2>cast(to1, TestObject2.class);

        assertNull(to2);
    }

    public class TestObject1 {
    }

    public class TestObject2 {
    }
}
