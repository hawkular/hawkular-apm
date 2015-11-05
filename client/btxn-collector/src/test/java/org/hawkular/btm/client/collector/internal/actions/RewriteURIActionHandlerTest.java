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
package org.hawkular.btm.client.collector.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.RewriteURIAction;
import org.junit.Test;

/**
 * @author gbrown
 */
public class RewriteURIActionHandlerTest {

    @Test
    public void testMismatchURIAndTemplate1() {
        RewriteURIAction action = new RewriteURIAction();
        action.setTemplate("/not/same/elements");

        RewriteURIActionHandler handler = new RewriteURIActionHandler(action);
        Consumer consumer = new Consumer();
        consumer.setUri("/not/same");

        assertFalse(handler.process(null, consumer, Direction.In, null, null));
    }

    @Test
    public void testMismatchURIAndTemplate2() {
        RewriteURIAction action = new RewriteURIAction();
        action.setTemplate("/same/{name}/elements");

        RewriteURIActionHandler handler = new RewriteURIActionHandler(action);
        Consumer consumer = new Consumer();
        consumer.setUri("/same/fred/elementsbutdiffvalues");

        assertFalse(handler.process(null, consumer, Direction.In, null, null));
    }

    @Test
    public void testURIRewrite() {
        RewriteURIAction action = new RewriteURIAction();
        action.setTemplate("/my/{name}/and/{num}");

        RewriteURIActionHandler handler = new RewriteURIActionHandler(action);

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
}
