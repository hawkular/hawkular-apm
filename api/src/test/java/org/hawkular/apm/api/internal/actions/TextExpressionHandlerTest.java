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
package org.hawkular.apm.api.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.DataSource;
import org.hawkular.apm.api.model.config.txn.TextExpression;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TextExpressionHandlerTest {

    @Test
    public void testPredicateValue() {
        TextExpression text = new TextExpression();
        text.setSource(DataSource.Content);
        text.setKey("0");

        Object[] values = new Object[1];
        values[0] = "true";

        TextExpressionHandler handler = new TextExpressionHandler(text);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, null, values));
    }

    @Test
    public void testEvaluateValue() {
        TextExpression text = new TextExpression();
        text.setSource(DataSource.Content);
        text.setKey("0");

        Object[] values = new Object[1];
        values[0] = "hello";

        TextExpressionHandler handler = new TextExpressionHandler(text);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals("hello", result);
    }

    @Test
    public void testPredicateHeader() {
        TextExpression text = new TextExpression();
        text.setSource(DataSource.Header);
        text.setKey("mykey");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("mykey", "true");

        TextExpressionHandler handler = new TextExpressionHandler(text);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, headers, null));
    }

    @Test
    public void testEvaluateHeader() {
        TextExpression text = new TextExpression();
        text.setSource(DataSource.Header);
        text.setKey("mykey");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("mykey", "hello");

        TextExpressionHandler handler = new TextExpressionHandler(text);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, headers, null);

        assertNotNull(result);
        assertEquals("hello", result);
    }

}
