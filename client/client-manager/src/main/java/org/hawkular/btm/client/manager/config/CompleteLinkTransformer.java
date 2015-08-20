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
package org.hawkular.btm.client.manager.config;

import org.hawkular.btm.api.model.admin.CompleteLink;
import org.hawkular.btm.api.model.admin.InstrumentAction;

/**
 * This class transforms the CompleteLink action type.
 *
 * @author gbrown
 */
public class CompleteLinkTransformer implements InstrumentActionTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentActionTransformer#getActionType()
     */
    @Override
    public Class<? extends InstrumentAction> getActionType() {
        return CompleteLink.class;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentActionTransformer#convertToRuleAction(
     *                      org.hawkular.btm.api.model.admin.InstrumentAction)
     */
    @Override
    public String convertToRuleAction(InstrumentAction action) {
        CompleteLink linkAction = (CompleteLink) action;
        StringBuilder builder = new StringBuilder();

        builder.append("completeLink(");

        builder.append(linkAction.getIdExpression());

        builder.append(")");

        return builder.toString();
    }

}
