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
import org.hawkular.btm.api.model.admin.CollectorAction.Direction;
import org.hawkular.btm.api.model.admin.InstrumentConsumer;
import org.hawkular.btm.client.manager.ClientManager;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentConsumerTransformerTest {

    private static final String ACTION_PREFIX = ClientManager.class.getName() + ".collector().";

    @Test
    public void testConvertToRuleActionRequest() {
        InstrumentConsumer im = new InstrumentConsumer();

        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.getValueExpressions().add("$1");
        im.getValueExpressions().add("$2");

        InstrumentConsumerTransformer transformer = new InstrumentConsumerTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "consumerStart(\"MyEndpoint\",\"MyUri\","
                + ArrayBuilder.class.getName() + ".create().add($1).add($2).get())";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionResponse() {
        InstrumentConsumer im = new InstrumentConsumer();

        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.getValueExpressions().add("$!");
        im.setDirection(Direction.Response);

        InstrumentConsumerTransformer transformer = new InstrumentConsumerTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "consumerEnd(\"MyEndpoint\",\"MyUri\","
                + ArrayBuilder.class.getName() + ".create().add($!).get())";

        assertEquals(expected, transformed);
    }

}
