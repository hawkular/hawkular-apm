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
package org.hawkular.apm.instrumenter.rules;

import static org.junit.Assert.assertEquals;

import org.hawkular.apm.api.model.config.instrumentation.Instrumentation;
import org.hawkular.apm.api.model.config.instrumentation.jvm.FreeFormAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentBind;
import org.hawkular.apm.api.model.config.instrumentation.jvm.JVM;
import org.hawkular.apm.instrumenter.RuleHelper;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TransformerTest {

    private static final String ANY_PARAMETERS = "*";
    private static final String BIND_EXPR2 = "BindExpr2";
    private static final String BIND_TYPE2 = "BindType2";
    private static final String BIND_NAME2 = "BindName2";
    private static final String BIND_EXPR1 = "BindExpr1";
    private static final String BIND_TYPE1 = "BindType1";
    private static final String BIND_NAME1 = "BindName1";
    private static final String TEST_CONDITION_1 = "$1.getAttributes().contains(\"BTMID\")";
    private static final String TEST_PARAM2 = "TestParam2";
    private static final String TEST_PARAM1 = "TestParam1";
    private static final String TEST_METHOD = "TestMethod";
    private static final String TEST_CLASS = "TestClass";
    private static final String TEST_RULE = "TestRule";

    @Test
    public void testTransformNoParameters() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "()\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\nIF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoParametersInvalidVersion() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        ir.setFromVersion("2.0.0");
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, "1.0.0");

        String expected = "COMPILE\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoParametersInvalidNullVersion() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        ir.setToVersion("2.0.0");
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoParametersValidVersion() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        ir.setFromVersion("2.0.0");
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, "2.0.0");

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "()\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\nIF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoParametersValidNullVersion() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        ir.setFromVersion("2.0.0");
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "()\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\nIF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformAnyParameters() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.getParameterTypes().add(ANY_PARAMETERS);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\nIF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoConditionLocationEntry() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getParameterTypes().add(TEST_PARAM1);
        ir.getParameterTypes().add(TEST_PARAM2);
        ir.getActions().add(im);
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\nIF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformConditionLocationExit() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("EXIT");
        ir.setCondition(TEST_CONDITION_1);
        ir.getParameterTypes().add(TEST_PARAM1);
        ir.getParameterTypes().add(TEST_PARAM2);
        ir.getActions().add(im);
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT EXIT\r\nIF "
                + TEST_CONDITION_1 + "\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformConditionLocationExceptionExit() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("EXCEPTION EXIT");
        ir.setCondition(TEST_CONDITION_1);
        ir.getParameterTypes().add(TEST_PARAM1);
        ir.getParameterTypes().add(TEST_PARAM2);
        ir.getActions().add(im);
        im.setAction("Action1");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT EXCEPTION EXIT\r\nIF "
                + TEST_CONDITION_1 + "\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformMultipleActions() {
        JVM ir = new JVM();
        FreeFormAction im1 = new FreeFormAction();
        FreeFormAction im2 = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setHelper("TestHelper");
        ir.setLocation("ENTRY");
        ir.setCondition(TEST_CONDITION_1);
        ir.getParameterTypes().add(TEST_PARAM1);
        ir.getParameterTypes().add(TEST_PARAM2);
        ir.getActions().add(im1);
        ir.getActions().add(im2);

        im1.setAction("Action1");
        im2.setAction("Action2");

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\n"
                + "HELPER TestHelper\r\n"
                + "AT ENTRY\r\nIF "
                + TEST_CONDITION_1 + "\r\n"
                + "DO\r\n  " + im1.getAction() + ";\r\n"
                + "  " + im2.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformBind() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getParameterTypes().add(TEST_PARAM1);
        ir.getParameterTypes().add(TEST_PARAM2);
        ir.getActions().add(im);
        im.setAction("Action1");

        InstrumentBind bind1 = new InstrumentBind();
        bind1.setName(BIND_NAME1);
        bind1.setType(BIND_TYPE1);
        bind1.setExpression(BIND_EXPR1);
        ir.getBinds().add(bind1);

        InstrumentBind bind2 = new InstrumentBind();
        bind2.setName(BIND_NAME2);
        bind2.setType(BIND_TYPE2);
        bind2.setExpression(BIND_EXPR2);
        ir.getBinds().add(bind2);

        Instrumentation in = new Instrumentation();
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\n"
                + "BIND " + BIND_NAME1 + " : " + BIND_TYPE1 + " = " + BIND_EXPR1 + ";\r\n"
                + "     " + BIND_NAME2 + " : " + BIND_TYPE2 + " = " + BIND_EXPR2 + ";\r\n"
                + "IF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoCompileScript() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        im.setAction("Action1");
        ir.setCompile(false);

        Instrumentation in = new Instrumentation();
        in.setCompile(false);
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "NOCOMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "()\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\nIF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformCompileScriptNoCompileRule() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        im.setAction("Action1");
        ir.setCompile(false);

        Instrumentation in = new Instrumentation();
        in.setCompile(true);
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "COMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "()\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\n"
                + "NOCOMPILE\r\n"
                + "IF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }

    @Test
    public void testTransformNoCompileScriptCompileRule() {
        JVM ir = new JVM();
        FreeFormAction im = new FreeFormAction();

        ir.setRuleName(TEST_RULE);
        ir.setClassName(TEST_CLASS);
        ir.setMethodName(TEST_METHOD);
        ir.setLocation("ENTRY");
        ir.getActions().add(im);
        im.setAction("Action1");
        ir.setCompile(true);

        Instrumentation in = new Instrumentation();
        in.setCompile(false);
        in.getRules().add(ir);

        RuleTransformer transformer = new RuleTransformer();

        String transformed = transformer.transform("test", in, null);

        String expected = "NOCOMPILE\r\n\r\n"
                + "RULE test(1) " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "()\r\n"
                + "HELPER " + RuleHelper.class.getName() + "\r\n"
                + "AT ENTRY\r\n"
                + "COMPILE\r\n"
                + "IF TRUE\r\n"
                + "DO\r\n  " + im.getAction() + "\r\n"
                + "ENDRULE\r\n\r\n";

        assertEquals(expected, transformed);
    }
}
