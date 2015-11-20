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
package org.hawkular.btm.api.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.EvaluateURIAction;
import org.hawkular.btm.api.utils.NodeUtil;
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

        BusinessTransaction btxn = new BusinessTransaction();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/fred/and/5");

        assertTrue(handler.process(btxn, consumer, Direction.In, null, null));

        assertEquals(action.getTemplate(), consumer.getUri());
        assertTrue(btxn.getProperties().containsKey("name"));
        assertTrue(btxn.getProperties().containsKey("num"));
        assertEquals("fred", btxn.getProperties().get("name"));
        assertEquals("5", btxn.getProperties().get("num"));
    }

    @Test
    public void testEvaluateURIQueryStringAndNoRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/uri?{name}&{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        BusinessTransaction btxn = new BusinessTransaction();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/uri");
        consumer.getDetails().put("http_query", "num=5&another=value&name=hello%20world");

        assertTrue(handler.process(btxn, consumer, Direction.In, null, null));

        assertFalse(NodeUtil.isURIRewritten(consumer));

        assertTrue(btxn.getProperties().containsKey("name"));
        assertTrue(btxn.getProperties().containsKey("num"));
        assertEquals("hello world", btxn.getProperties().get("name"));
        assertEquals("5", btxn.getProperties().get("num"));

        assertFalse(btxn.getProperties().containsKey("another"));
    }

    @Test
    public void testEvaluateURIPathAndQueryStringWithRewrite() {
        EvaluateURIAction action = new EvaluateURIAction();
        action.setTemplate("/my/{pathParam}/uri?{name}&{num}");

        EvaluateURIActionHandler handler = new EvaluateURIActionHandler(action);

        BusinessTransaction btxn = new BusinessTransaction();
        Consumer consumer = new Consumer();
        consumer.setUri("/my/test%20param/uri");
        consumer.getDetails().put("http_query", "num=5&another=value&name=hello%20world");

        assertTrue(handler.process(btxn, consumer, Direction.In, null, null));

        // URI should now only be path part of template
        assertEquals("/my/{pathParam}/uri", consumer.getUri());

        assertTrue(btxn.getProperties().containsKey("pathParam"));
        assertTrue(btxn.getProperties().containsKey("name"));
        assertTrue(btxn.getProperties().containsKey("num"));
        assertEquals("test param", btxn.getProperties().get("pathParam"));
        assertEquals("hello world", btxn.getProperties().get("name"));
        assertEquals("5", btxn.getProperties().get("num"));

        assertFalse(btxn.getProperties().containsKey("another"));
    }
}
