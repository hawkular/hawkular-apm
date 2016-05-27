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
package org.hawkular.btm.api.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.LiteralExpression;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.SetDetailAction;
import org.hawkular.btm.api.model.trace.Consumer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class SetDetailActionHandlerTest {

    /**  */
    private static final String TEST_NAME_1 = "testname1";

    /**  */
    private static final String TEST_VALUE_1 = "testvalue1";

    @Test
    public void testSetDetail() {
        SetDetailAction action = new SetDetailAction();
        action.setName(TEST_NAME_1);
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        SetDetailActionHandler handler = new SetDetailActionHandler(action);

        handler.init(null);

        Consumer node = new Consumer();

        handler.process(null, node, Direction.In, null, null);

        assertEquals(1, node.getDetails().size());
        assertTrue(node.getDetails().containsKey(TEST_NAME_1));
        assertEquals(TEST_VALUE_1, node.getDetails().get(TEST_NAME_1));

        assertNull(handler.getIssues());
    }

    @Test
    public void testNoName() {
        SetDetailAction action = new SetDetailAction();
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        SetDetailActionHandler handler = new SetDetailActionHandler(action);

        handler.init(new Processor());

        assertNotNull(handler.getIssues());
    }
}
