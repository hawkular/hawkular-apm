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
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CustomRuleITest extends ClientTestBase {

    private static final String HELLO_WORLD = "Hello World";

    @Test
    public void testExampleCall() {
        CustomComponent cc = new CustomComponent();
        cc.exampleCall(HELLO_WORLD);

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        Trace trace=getApmMockServer().getTraces().get(0);

        // Check contains single component
        assertEquals(1, trace.getNodes().size());
        assertEquals(Component.class, trace.getNodes().get(0).getClass());

        Component component = (Component)trace.getNodes().get(0);
        assertEquals("exampleCall", component.getOperation());

        Set<Property> props = component.getProperties("message");
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals(HELLO_WORLD, props.iterator().next().getValue());
    }

}
