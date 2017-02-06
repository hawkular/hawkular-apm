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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.AddContentAction;
import org.hawkular.apm.api.model.config.txn.LiteralExpression;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.trace.Consumer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AddContentActionHandlerTest {

    private static final String TEST_TYPE_1 = "testtype1";

    private static final String TEST_NAME_1 = "testname1";

    private static final String TEST_VALUE_1 = "testvalue1";

    @Test
    public void testIn() {
        AddContentAction action = new AddContentAction();
        action.setName(TEST_NAME_1);
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        AddContentActionHandler handler = new AddContentActionHandler(action);

        handler.init(null);

        Consumer node = new Consumer();

        handler.process(null, node, Direction.In, null, null);

        assertEquals(1, node.getIn().getContent().size());
        assertTrue(node.getIn().getContent().containsKey(TEST_NAME_1));
        assertEquals(TEST_VALUE_1, node.getIn().getContent().get(TEST_NAME_1).getValue());
        assertNull(node.getIn().getContent().get(TEST_NAME_1).getType());
    }

    @Test
    public void testInWithType() {
        AddContentAction action = new AddContentAction();
        action.setName(TEST_NAME_1);
        action.setType(TEST_TYPE_1);
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        AddContentActionHandler handler = new AddContentActionHandler(action);

        handler.init(null);

        Consumer node = new Consumer();

        handler.process(null, node, Direction.In, null, null);

        assertEquals(1, node.getIn().getContent().size());
        assertTrue(node.getIn().getContent().containsKey(TEST_NAME_1));
        assertEquals(TEST_VALUE_1, node.getIn().getContent().get(TEST_NAME_1).getValue());
        assertEquals(TEST_TYPE_1, node.getIn().getContent().get(TEST_NAME_1).getType());
    }

    @Test
    public void testOut() {
        AddContentAction action = new AddContentAction();
        action.setName(TEST_NAME_1);
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        AddContentActionHandler handler = new AddContentActionHandler(action);

        handler.init(null);

        Consumer node = new Consumer();

        handler.process(null, node, Direction.Out, null, null);

        assertEquals(1, node.getOut().getContent().size());
        assertTrue(node.getOut().getContent().containsKey(TEST_NAME_1));
        assertEquals(TEST_VALUE_1, node.getOut().getContent().get(TEST_NAME_1).getValue());
        assertNull(node.getOut().getContent().get(TEST_NAME_1).getType());
    }

    @Test
    public void testNoName() {
        AddContentAction action = new AddContentAction();
        action.setExpression(new LiteralExpression().setValue(TEST_VALUE_1));

        AddContentActionHandler handler = new AddContentActionHandler(action);

        handler.init(new Processor());
    }
}
