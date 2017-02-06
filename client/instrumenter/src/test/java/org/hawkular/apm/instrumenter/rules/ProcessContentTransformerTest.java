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

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.ProcessContent;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ProcessContentTransformerTest {

    private static final String ACTION_PREFIX = "collector().";

    @Test
    public void testConvertToRuleActionIn() {
        ProcessContent im = new ProcessContent();

        im.getValueExpressions().add("$1");
        im.getValueExpressions().add("$2");

        ProcessContentTransformer transformer = new ProcessContentTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX +
                "processIn(getRuleName(),null,createArrayBuilder().add($1).add($2).get())";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionOut() {
        ProcessContent im = new ProcessContent();

        im.setDirection(Direction.Out);
        im.getValueExpressions().add("$1");
        im.getValueExpressions().add("$2");

        ProcessContentTransformer transformer = new ProcessContentTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX +
                "processOut(getRuleName(),null,createArrayBuilder().add($1).add($2).get())";

        assertEquals(expected, transformed);
    }

}
