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

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.hawkular.btm.api.model.config.btxn.SetDetailAction;

/**
 * This handler is associated with the SetDetail action.
 *
 * @author gbrown
 */
public class SetDetailActionHandler extends ExpressionBasedActionHandler {

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public SetDetailActionHandler(ProcessorAction action) {
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
                node.getDetails().put(((SetDetailAction) getAction()).getName(), value);
                return true;
            }
        }
        return false;
    }

}
