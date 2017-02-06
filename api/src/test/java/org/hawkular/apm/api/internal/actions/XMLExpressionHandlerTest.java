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
import org.hawkular.apm.api.model.config.txn.XMLExpression;
import org.junit.Test;

/**
 * @author gbrown
 */
public class XMLExpressionHandlerTest {

    @Test
    public void testPredicateValue() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Content);
        xml.setKey("0");
        xml.setXpath("true()");

        Object[] values = new Object[1];
        values[0] = "<doc/>";

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, null, values));
    }

    @Test
    public void testPredicateValueNoXpath() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Content);
        xml.setKey("0");

        Object[] values = new Object[1];
        values[0] = "<doc/>";

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        assertFalse(handler.test(null, null, Direction.In, null, values));
    }

    @Test
    public void testEvaluateValue() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Content);
        xml.setKey("0");
        xml.setXpath("/doc/@name");

        Object[] values = new Object[1];
        values[0] = "<doc name=\"hello\" />";

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals("hello", result);
    }

    @Test
    public void testEvaluateValueNoXpath() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Content);
        xml.setKey("0");

        Object[] values = new Object[1];
        values[0] = "<doc name=\"hello\" />";

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals(values[0], result);
    }

    @Test
    public void testEvaluateValueEmptyXpath() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Content);
        xml.setKey("0");
        xml.setXpath(" ");

        Object[] values = new Object[1];
        values[0] = "<doc name=\"hello\" />";

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, null, values);

        assertNotNull(result);
        assertEquals(values[0], result);
    }

    @Test
    public void testPredicateHeader() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Header);
        xml.setKey("mykey");
        xml.setXpath("true()");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("mykey", "<doc/>");

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, headers, null));
    }

    @Test
    public void testEvaluateHeader() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Header);
        xml.setKey("mykey");
        xml.setXpath("/doc/@name");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("mykey", "<doc name=\"hello\" />");

        XMLExpressionHandler handler = new XMLExpressionHandler(xml);
        handler.init(null, null, true);

        String result = handler.evaluate(null, null, Direction.In, headers, null);

        assertNotNull(result);
        assertEquals("hello", result);
    }

}
