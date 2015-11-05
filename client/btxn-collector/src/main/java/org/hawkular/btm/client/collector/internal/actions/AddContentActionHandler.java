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

import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.InteractionNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.AddContentAction;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;

/**
 * This handler is associated with the AddContent action.
 *
 * @author gbrown
 */
public class AddContentActionHandler extends ExpressionBasedActionHandler {

    private static final Logger log = Logger.getLogger(AddContentActionHandler.class.getName());

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public AddContentActionHandler(ProcessorAction action) {
        super(action);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.collector.internal.actions.ProcessorActionHandler#process(
     *      org.hawkular.btm.api.model.btxn.BusinessTransaction, org.hawkular.btm.api.model.btxn.Node,
     *      org.hawkular.btm.api.model.config.Direction, java.util.Map, java.lang.Object[])
     */
    @Override
    public boolean process(BusinessTransaction btxn, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (super.process(btxn, node, direction, headers, values)) {
            String value = getValue(btxn, node, direction, headers, values);
            if (value != null) {
                if (node.interactionNode()) {
                    if (direction == Direction.In) {
                        ((InteractionNode) node).getIn().addContent(((AddContentAction) getAction()).getName(),
                                ((AddContentAction) getAction()).getType(), value);
                    } else {
                        ((InteractionNode) node).getOut().addContent(
                                ((AddContentAction) getAction()).getName(),
                                ((AddContentAction) getAction()).getType(), value);
                    }
                    return true;
                } else {
                    log.warning("Attempt to add content to a non-interaction based node type '"
                            + node.getType() + "'");
                }
            }
        }
        return false;
    }

}
