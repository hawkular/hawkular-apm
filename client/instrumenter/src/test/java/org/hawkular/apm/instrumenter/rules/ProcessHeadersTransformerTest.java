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
import org.hawkular.apm.api.model.config.instrumentation.jvm.ProcessHeaders;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ProcessHeadersTransformerTest {

    private static final String ACTION_PREFIX = "collector().";

    @Test
    public void testConvertToRuleActionIn() {
        ProcessHeaders im = new ProcessHeaders();

        im.setHeadersExpression("headers");

        ProcessHeadersTransformer transformer = new ProcessHeadersTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "processIn(getRuleName(),headers,null)";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionInWithOriginalType() {
        ProcessHeaders im = new ProcessHeaders();

        im.setHeadersExpression("headers");
        im.setOriginalType("org.Type");

        ProcessHeadersTransformer transformer = new ProcessHeadersTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "processIn(getRuleName(),getHeaders(\"org.Type\",headers),null)";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionOut() {
        ProcessHeaders im = new ProcessHeaders();

        im.setDirection(Direction.Out);
        im.setHeadersExpression("headers");

        ProcessHeadersTransformer transformer = new ProcessHeadersTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "processOut(getRuleName(),headers,null)";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionOutWithOriginalType() {
        ProcessHeaders im = new ProcessHeaders();

        im.setDirection(Direction.Out);
        im.setHeadersExpression("headers");
        im.setOriginalType("org.Type");

        ProcessHeadersTransformer transformer = new ProcessHeadersTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "processOut(getRuleName(),getHeaders(\"org.Type\",headers),null)";

        assertEquals(expected, transformed);
    }

}
