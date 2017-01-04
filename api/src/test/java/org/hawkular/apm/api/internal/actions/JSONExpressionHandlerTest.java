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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.DataSource;
import org.hawkular.apm.api.model.config.txn.JSONExpression;
import org.junit.Test;

/**
 * @author gbrown
 */
public class JSONExpressionHandlerTest {

    @Test
    public void testPredicateValue() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Content);
        json.setKey("0");
        json.setJsonpath("$.bool");

        Object[] values = new Object[1];
        values[0] = "{ \"bool\": true }";

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, null, values));
    }

    @Test
    public void testPredicateValueNoJsonpath() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Content);
        json.setKey("0");

        Object[] values = new Object[1];
        values[0] = "{ \"bool\": true }";

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        assertFalse(handler.test(null, null, Direction.In, null, values));
    }

    @Test
    public void testEvaluateValue() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Content);
        json.setKey("0");
        json.setJsonpath("$.name");

        Object[] values = new Object[1];
        values[0] = "{ \"name\": \"hello\" }";

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals("hello", result);
    }

    @Test
    public void testEvaluateValueNoJsonpath() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Content);
        json.setKey("0");

        Object[] values = new Object[1];
        values[0] = "{ \"name\": \"hello\" }";

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals(values[0], result);
    }

    @Test
    public void testEvaluateValueEmptyJsonpath() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Content);
        json.setKey("0");
        json.setJsonpath(" ");

        Object[] values = new Object[1];
        values[0] = "{ \"name\": \"hello\" }";

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals(values[0], result);
    }

    @Test
    public void testPredicateHeader() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Header);
        json.setKey("mykey");
        json.setJsonpath("$.bool");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("mykey", "{ \"bool\": true }");

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, headers, null));
    }

    @Test
    public void testEvaluateHeader() {
        JSONExpression json = new JSONExpression();
        json.setSource(DataSource.Header);
        json.setKey("mykey");
        json.setJsonpath("$.name");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("mykey", "{ \"name\": \"hello\" }");

        JSONExpressionHandler handler = new JSONExpressionHandler(json);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, headers, null);

        assertNotNull(result);
        assertEquals("hello", result);
    }

}
