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
package org.hawkular.apm.instrumenter;

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

        String str = helper.<String> cast("hello", String.class);

        assertEquals("hello", str);
    }

    @Test
    public void testCastIncorrect() {
        RuleHelper helper = new RuleHelper(null);

        TestObject1 to1 = new TestObject1();

        TestObject2 to2 = helper.<TestObject2> cast(to1, TestObject2.class);

        assertNull(to2);
    }

    @Test
    public void testRemoveSuffix() {
        String original = "HelloEndpoint";
        String suffix = "Endpoint";
        String expected = "Hello";

        RuleHelper helper = new RuleHelper(null);

        assertEquals(expected, helper.removeSuffix(original, suffix));
    }

    @Test
    public void testRemoveAfter() {
        String original = "Hello$$$World";
        String marker = "$$$";
        String expected = "Hello";

        RuleHelper helper = new RuleHelper(null);

        assertEquals(expected, helper.removeAfter(original, marker));
    }

    @Test
    public void testFormatSQL() {
        RuleHelper helper = new RuleHelper(null);

        String pretext = "prep79: ";
        String sql = "select performanc0_.show_id as show_id3_0_1_, performanc0_.id as id1_5_1_, performanc0_.id "
                + "as id1_5_0_, performanc0_.date as date2_5_0_, performanc0_.show_id as show_id3_5_0_ from "
                + "Performance performanc0_ where performanc0_.show_id=? order by performanc0_.date {1: 1}";

        String result = helper.formatSQL(pretext + sql, null);

        assertEquals(sql, result);
    }

    @Test
    public void testFormatSQL2() {
        RuleHelper helper = new RuleHelper(null);

        String pretext = "prep102: ";
        String sql = "select mediaitem0_.id as id1_4_0_, mediaitem0_.mediaType as mediaTyp2_4_0_, mediaitem0_.url "
                + "as url3_4_0_ from MediaItem mediaitem0_ where mediaitem0_.id=? {1: 22}";

        String result = helper.formatSQL(pretext + sql, null);

        assertEquals(sql, result);
    }

    @Test
    public void testFormatSQLWithBinary() {
        RuleHelper helper = new RuleHelper(null);

        String pretext = "update SectionAllocation set allocated=?, occupiedCount=?, performance_id=?, "
                + "section_id=?, version=? where id=? and version=? {1: ";
        String binary = "X'aced0005757200035b5b4afe76f8764a55dfbd020000787000000014757200025b4a782004b512b1759"
                + "3020000787000000064ffffffffffffffffffffffffffffffff00000000000000000000000000000000000000"
                + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
                + "00000000000000000000000000000000000000000000000000000000007571007e000200000064000000000000"
                + "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000'";
        String posttext = ", 2: 4, 3: 1, 4: 1, 5: 2, 6: 1, 7:   }";

        String pre = pretext + binary + posttext;
        String expected = pretext + RuleHelper.BINARY_SQL_MARKER + posttext;

        String result = helper.formatSQL(pre, null);

        assertEquals(expected, result);
    }

    @Test
    public void testFormatSQLWithSupplidExpression() {
        RuleHelper helper = new RuleHelper(null);

        String expr = "theExpression";

        String context = "theContext";

        String result = helper.formatSQL(context, expr);

        assertEquals(expr, result);
    }

    public class TestObject1 {
    }

    public class TestObject2 {
    }
}
