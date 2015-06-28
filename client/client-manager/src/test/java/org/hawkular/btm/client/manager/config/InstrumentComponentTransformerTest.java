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

import org.hawkular.btm.api.model.admin.CollectorAction.Direction;
import org.hawkular.btm.api.model.admin.InstrumentComponent;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentComponentTransformerTest {

    private static final String ACTION_PREFIX = "collector().";

    @Test
    public void testConvertToRuleActionRequest() {
        InstrumentComponent im = new InstrumentComponent();

        im.setComponentTypeExpression("\"MyComponent\"");
        im.setOperationExpression("\"MyOperation\"");
        im.setUriExpression("\"MyUri\"");

        InstrumentComponentTransformer transformer = new InstrumentComponentTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "componentStart(\"MyUri\",\"MyComponent\",\"MyOperation\")";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionResponse() {
        InstrumentComponent im = new InstrumentComponent();

        im.setComponentTypeExpression("\"MyComponent\"");
        im.setOperationExpression("\"MyOperation\"");
        im.setUriExpression("\"MyUri\"");
        im.setDirection(Direction.Response);

        InstrumentComponentTransformer transformer = new InstrumentComponentTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "componentEnd(\"MyUri\",\"MyComponent\",\"MyOperation\")";

        assertEquals(expected, transformed);
    }

}
