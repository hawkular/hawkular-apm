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
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentProducer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentProducerTransformerTest {

    private static final String ACTION_PREFIX = "collector().";

    @Test
    public void testConvertToRuleActionIn() {
        InstrumentProducer im = new InstrumentProducer();

        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.setOperationExpression("\"MyOperation\"");

        InstrumentProducerTransformer transformer = new InstrumentProducerTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "producerStart(getRuleName(),\"MyUri\",\"MyEndpoint\",\"MyOperation\",null)";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionInWithId() {
        InstrumentProducer im = new InstrumentProducer();

        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.setOperationExpression("\"MyOperation\"");
        im.setIdExpression("\"MyId\"");

        InstrumentProducerTransformer transformer = new InstrumentProducerTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX +
                "producerStart(getRuleName(),\"MyUri\",\"MyEndpoint\",\"MyOperation\",\"MyId\")";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionOut() {
        InstrumentProducer im = new InstrumentProducer();

        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.setOperationExpression("\"MyOperation\"");
        im.setDirection(Direction.Out);

        InstrumentProducerTransformer transformer = new InstrumentProducerTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "producerEnd(getRuleName(),\"MyUri\",\"MyEndpoint\",\"MyOperation\")";

        assertEquals(expected, transformed);
    }

    @Test
    public void testConvertToRuleActionOutWithId() {
        InstrumentProducer im = new InstrumentProducer();

        im.setEndpointTypeExpression("\"MyEndpoint\"");
        im.setUriExpression("\"MyUri\"");
        im.setOperationExpression("\"MyOperation\"");
        im.setDirection(Direction.Out);

        InstrumentProducerTransformer transformer = new InstrumentProducerTransformer();

        String transformed = transformer.convertToRuleAction(im);

        String expected = ACTION_PREFIX + "producerEnd(getRuleName(),\"MyUri\",\"MyEndpoint\",\"MyOperation\")";

        assertEquals(expected, transformed);
    }
}
