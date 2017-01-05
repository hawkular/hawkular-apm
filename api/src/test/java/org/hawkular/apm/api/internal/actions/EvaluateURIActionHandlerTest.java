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
package org.hawkular.apm.api.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.EvaluateURIAction;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.junit.Test;

/**
 * @author gbrown
 */
public class EvaluateURIActionHandlerTest {

    @Test
    public void testMismatchURIAndTemplate1() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/not/same/elements");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);
        Consumer consumer = new Consumer();
        consumer.setUri("/not/same");

        assertFalse(handler.process(null, consumer, Direction.In, null, null));
    }

    @Test
    public void testMismatchURIAndTemplate2() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/same/{name}/elements");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);
        Consumer consumer = new Consumer();
        consumer.setUri("/same/fred/elementsbutdiffvalues");

        assertFalse(handler.process(null, consumer, Direction.In, null, null));
    }

    @Test
    public void testEvaluateURIPathAndRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/{name}/and/{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        Trace trace = new Trace();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/fred/and/5");
        trace.getNodes().add(consumer);

        assertTrue(handler.process(trace, consumer, Direction.In, null, null));

        assertEquals(action.getTemplate(), consumer.getUri());
        assertTrue(consumer.hasProperty("name"));
        assertTrue(consumer.hasProperty("num"));
        assertEquals("fred", consumer.getProperties("name").iterator().next().getValue());
        assertEquals("5", consumer.getProperties("num").iterator().next().getValue());
    }

    @Test
    public void testEvaluateURIQueryStringAndNoRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/uri?{name}&{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        Trace trace = new Trace();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/uri");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_QUERY, "num=5&another=value&name=hello%20world"));
        trace.getNodes().add(consumer);

        assertTrue(handler.process(trace, consumer, Direction.In, null, null));

        assertFalse(NodeUtil.isURIRewritten(consumer));

        assertTrue(consumer.hasProperty("name"));
        assertTrue(consumer.hasProperty("num"));
        assertEquals("hello world", consumer.getProperties("name").iterator().next().getValue());
        assertEquals("5", consumer.getProperties("num").iterator().next().getValue());

        assertFalse(trace.hasProperty("another"));
    }

    @Test
    public void testEvaluateURIPathAndQueryStringWithRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/{pathParam}/uri?{name}&{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        Trace trace = new Trace();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/test%20param/uri");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_QUERY, "num=5&another=value&name=hello%20world"));
        trace.getNodes().add(consumer);

        assertTrue(handler.process(trace, consumer, Direction.In, null, null));

        // URI should now only be path part of template
        assertEquals("/my/{pathParam}/uri", consumer.getUri());

        assertTrue(consumer.hasProperty("pathParam"));
        assertTrue(consumer.hasProperty("name"));
        assertTrue(consumer.hasProperty("num"));
        assertEquals("test param", consumer.getProperties("pathParam").iterator().next().getValue());
        assertEquals("hello world", consumer.getProperties("name").iterator().next().getValue());
        assertEquals("5", consumer.getProperties("num").iterator().next().getValue());

        assertFalse(trace.hasProperty("another"));
    }

    @Test
    public void testNoTemplate() {
        EvaluateURIAction action = new EvaluateURIAction();

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        handler.init(new Processor());
    }
}
