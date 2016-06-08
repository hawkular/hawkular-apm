/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.btxn.EvaluateURIAction;
import org.hawkular.apm.api.model.config.btxn.Processor;
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

        assertNull(handler.getIssues());
    }

    @Test
    public void testMismatchURIAndTemplate2() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/same/{name}/elements");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);
        Consumer consumer = new Consumer();
        consumer.setUri("/same/fred/elementsbutdiffvalues");

        assertFalse(handler.process(null, consumer, Direction.In, null, null));

        assertNull(handler.getIssues());
    }

    @Test
    public void testEvaluateURIPathAndRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/{name}/and/{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        Trace trace = new Trace();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/fred/and/5");

        assertTrue(handler.process(trace, consumer, Direction.In, null, null));

        assertEquals(action.getTemplate(), consumer.getUri());
        assertTrue(trace.hasProperty("name"));
        assertTrue(trace.hasProperty("num"));
        assertEquals("fred", trace.getProperties("name").iterator().next().getText());
        assertEquals("5", trace.getProperties("num").iterator().next().getText());

        assertNull(handler.getIssues());
    }

    @Test
    public void testEvaluateURIQueryStringAndNoRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/uri?{name}&{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        Trace trace = new Trace();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/uri");
        consumer.getDetails().put("http_query", "num=5&another=value&name=hello%20world");

        assertTrue(handler.process(trace, consumer, Direction.In, null, null));

        assertFalse(NodeUtil.isURIRewritten(consumer));

        assertTrue(trace.hasProperty("name"));
        assertTrue(trace.hasProperty("num"));
        assertEquals("hello world", trace.getProperties("name").iterator().next().getText());
        assertEquals("5", trace.getProperties("num").iterator().next().getText());

        assertFalse(trace.hasProperty("another"));

        assertNull(handler.getIssues());
    }

    @Test
    public void testEvaluateURIPathAndQueryStringWithRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/{pathParam}/uri?{name}&{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        Trace trace = new Trace();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/test%20param/uri");
        consumer.getDetails().put("http_query", "num=5&another=value&name=hello%20world");

        assertTrue(handler.process(trace, consumer, Direction.In, null, null));

        // URI should now only be path part of template
        assertEquals("/my/{pathParam}/uri", consumer.getUri());

        assertTrue(trace.hasProperty("pathParam"));
        assertTrue(trace.hasProperty("name"));
        assertTrue(trace.hasProperty("num"));
        assertEquals("test param", trace.getProperties("pathParam").iterator().next().getText());
        assertEquals("hello world", trace.getProperties("name").iterator().next().getText());
        assertEquals("5", trace.getProperties("num").iterator().next().getText());

        assertFalse(trace.hasProperty("another"));

        assertNull(handler.getIssues());
    }

    @Test
    public void testNoTemplate() {
        EvaluateURIAction action = new EvaluateURIAction();

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        handler.init(new Processor());

        assertNotNull(handler.getIssues());
    }
}
