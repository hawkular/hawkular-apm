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
import org.hawkular.btm.api.model.admin.InstrumentService;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentRuleTransformerTest {

    private static final String ACTION_PREFIX = "collector().";

    @Test
    public void testConvertToRuleActionRequest() {
        InstrumentService im = new InstrumentService();

        im.setUriExpression("\"MyService\"");
        im.setOperationExpression("\"MyOperation\"");
        im.getValueExpressions().add("$1");
        im.getValueExpressions().add("$2");
        im.setDirection(Direction.Request);

        InstrumentServiceTransformer transformer = new InstrumentServiceTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "serviceStart(\"MyService\",\"MyOperation\",null,"
                + "createArrayBuilder().add($1).add($2).get())";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionResponse() {
        InstrumentService im = new InstrumentService();

        im.setUriExpression("\"MyService\"");
        im.setOperationExpression("\"MyOperation\"");
        im.getValueExpressions().add("$!");
        im.setDirection(Direction.Response);

        InstrumentServiceTransformer transformer = new InstrumentServiceTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "serviceEnd(\"MyService\",\"MyOperation\",null,"
                + "createArrayBuilder().add($!).get())";

        assertEquals(expected, transformed);
    }

}
