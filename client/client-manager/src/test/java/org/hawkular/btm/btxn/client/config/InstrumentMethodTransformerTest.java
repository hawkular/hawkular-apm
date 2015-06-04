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
package org.hawkular.btm.btxn.client.config;

import static org.junit.Assert.assertEquals;

import org.hawkular.btm.api.model.admin.InstrumentMethod;
import org.hawkular.btm.client.manager.ClientManager;
import org.hawkular.btm.client.manager.config.InstrumentMethodTransformer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentMethodTransformerTest {

    /**  */
    private static final String TEST_PARAM2 = "TestParam2";
    /**  */
    private static final String TEST_PARAM1 = "TestParam1";
    /**  */
    private static final String TEST_METHOD = "TestMethod";
    /**  */
    private static final String TEST_CLASS = "TestClass";
    /**  */
    private static final String TEST_RULE = "TestRule";

    private static final String ACTION = ClientManager.class.getName()+".collector().print(\"Hello BTM\")";

    @Test
    public void testConvertToRule() {
        InstrumentMethod im = new InstrumentMethod();

        im.setRuleName(TEST_RULE);
        im.setClassName(TEST_CLASS);
        im.setMethodName(TEST_METHOD);
        im.getParameterTypes().add(TEST_PARAM1);
        im.getParameterTypes().add(TEST_PARAM2);

        InstrumentMethodTransformer transformer = new InstrumentMethodTransformer();

        String transformed = transformer.convertToRule(im);

        String expected = "RULE " + TEST_RULE + "\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\nIF true\r\n"
                + "DO " + ACTION + "\r\n"
                + "ENDRULE\r\n";

        assertEquals(expected, transformed);
    }

}
