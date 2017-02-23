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
package org.hawkular.apm.tests.agent.opentracing.custom;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hawkular.apm.tests.agent.opentracing.common.OpenTracingAgentTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

import io.opentracing.mock.MockSpan;

/**
 * @author gbrown
 */
public class CustomRuleITest extends OpenTracingAgentTestBase {

    private static final String HELLO_WORLD = "Hello World";

    @Test
    public void testExampleCall() {
        CustomComponent cc = new CustomComponent();
        cc.exampleCall(HELLO_WORLD);

        Wait.until(() -> getTracer().finishedSpans().size() == 1);

        List<MockSpan> spans = getTracer().finishedSpans();
        assertEquals(1, spans.size());
        assertEquals("exampleCall", spans.get(0).operationName());
        assertEquals(HELLO_WORLD, spans.get(0).tags().get("message"));
    }

}
