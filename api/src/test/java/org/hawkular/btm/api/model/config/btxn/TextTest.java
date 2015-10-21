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
package org.hawkular.btm.api.model.config.btxn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author gbrown
 */
public class TextTest {

    @Test
    public void testPredicate() {
        TextExpression text = new TextExpression();
        try {
            text.predicateText();
            fail("Expecting exception");
        } catch (Throwable e) {
            // Ignore
        }
    }

    @Test
    public void testEvaluateValue() {
        TextExpression text = new TextExpression();
        text.setSource(DataSource.Content);
        text.setKey("0");

        assertEquals("Text.serialize(values[0])", text.evaluateText());
    }

    @Test
    public void testEvaluateHeader() {
        TextExpression text = new TextExpression();
        text.setSource(DataSource.Header);
        text.setKey("mykey");

        assertEquals("Text.serialize(headers.get(\"mykey\"))", text.evaluateText());
    }

}
