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
package org.hawkular.btm.client.manager.config;

import static org.junit.Assert.assertEquals;

import org.hawkular.btm.api.internal.client.ArrayBuilder;
import org.hawkular.btm.api.model.admin.InstrumentConsumer;
import org.hawkular.btm.client.manager.ClientManager;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentConsumerTransformerTest {

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

    private static final String ACTION_PREFIX = ClientManager.class.getName()+".collector().";

    @Test
    public void testConvertToRule() {
        InstrumentConsumer im = new InstrumentConsumer();

        im.setRuleName(TEST_RULE);
        im.setClassName(TEST_CLASS);
        im.setMethodName(TEST_METHOD);
        im.getParameterTypes().add(TEST_PARAM1);
        im.getParameterTypes().add(TEST_PARAM2);
        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.getRequestValueExpressions().add("$1");
        im.getRequestValueExpressions().add("$2");
        im.getResponseValueExpressions().add("$!");

        InstrumentConsumerTransformer transformer = new InstrumentConsumerTransformer();

        String transformed = transformer.convertToRule(im);

        String startActionMethod="consumerStart(\"MyEndpoint\",\"MyUri\","
                + ArrayBuilder.class.getName() + ".create().add($1).add($2).get())";
        String endActionMethod="consumerEnd(\"MyEndpoint\",\"MyUri\","
                + ArrayBuilder.class.getName() + ".create().add($!).get())";

        String expected = "RULE " + TEST_RULE + "_entry\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\nAT ENTRY\r\nIF true\r\n"
                + "DO " + ACTION_PREFIX + startActionMethod + "\r\n"
                + "ENDRULE\r\n\r\n"
                + "RULE " + TEST_RULE + "_exit\r\nCLASS " + TEST_CLASS + "\r\n"
                + "METHOD " + TEST_METHOD + "(" + TEST_PARAM1 + "," + TEST_PARAM2 + ")\r\nAT EXIT\r\nIF true\r\n"
                + "DO " + ACTION_PREFIX + endActionMethod + "\r\n"
                + "ENDRULE\r\n";

        assertEquals(expected, transformed);
    }

}
