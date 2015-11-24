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
package org.hawkular.btm.api.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.ProcessorIssue;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.FreeFormExpression;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.SetPropertyAction;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FreeFormExpressionHandlerTest {

    @Test
    public void testPredicateTrue() {
        FreeFormExpression expr = new FreeFormExpression();
        expr.setValue("true");

        FreeFormExpressionHandler handler = new FreeFormExpressionHandler(expr);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, null, null));
        assertNull(handler.getIssues());
    }

    @Test
    public void testPredicateJSONTrue() {
        FreeFormExpression expr = new FreeFormExpression();
        expr.setValue("JSON.predicate(\"$.bool\",values[0])");

        Object[] values = new Object[1];
        values[0] = "{ \"bool\": true }";

        FreeFormExpressionHandler handler = new FreeFormExpressionHandler(expr);
        handler.init(null, null, true);

        assertTrue(handler.test(null, null, Direction.In, null, values));
        assertNull(handler.getIssues());
    }

    @Test
    public void testEvaluateValue() {
        FreeFormExpression expr = new FreeFormExpression();
        expr.setValue("\"hello\"");

        FreeFormExpressionHandler handler = new FreeFormExpressionHandler(expr);
        handler.init(null, null, false);

        assertEquals("hello", handler.evaluate(null, null, Direction.In, null, null));
        assertNull(handler.getIssues());
    }

    @Test
    public void testEvaluateJSONValue() {
        FreeFormExpression expr = new FreeFormExpression();
        expr.setValue("JSON.evaluate(\"$.name\",values[0])");

        Object[] values = new Object[1];
        values[0] = "{ \"name\": \"hello\" }";

        FreeFormExpressionHandler handler = new FreeFormExpressionHandler(expr);
        handler.init(null, null, false);

        assertEquals("hello", handler.evaluate(null, null, Direction.In, null, values));
        assertNull(handler.getIssues());
    }

    @Test
    public void testEvaluateInvalidExpression() {
        FreeFormExpression expr = new FreeFormExpression();
        expr.setValue("&");

        Processor processor = new Processor();
        processor.setDescription("Processor Description");

        SetPropertyAction action = new SetPropertyAction();
        action.setDescription("Action Description");

        FreeFormExpressionHandler handler = new FreeFormExpressionHandler(expr);
        handler.init(processor, action, false);

        assertEquals(null, handler.evaluate(null, null, Direction.In, null, null));
        assertEquals(1, handler.getIssues().size());
        assertEquals(processor.getDescription(), ((ProcessorIssue) handler.getIssues().get(0)).getProcessor());
        assertEquals(action.getDescription(), ((ProcessorIssue) handler.getIssues().get(0)).getAction());
    }

}
