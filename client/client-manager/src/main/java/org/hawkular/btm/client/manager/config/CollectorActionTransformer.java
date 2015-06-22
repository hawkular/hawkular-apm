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

import org.hawkular.btm.api.model.admin.CollectorAction;
import org.hawkular.btm.api.model.admin.CollectorAction.Direction;
import org.hawkular.btm.api.model.admin.InstrumentAction;

/**
 * This class transforms the InstrumentInvocation type.
 *
 * @author gbrown
 */
public abstract class CollectorActionTransformer implements InstrumentActionTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentActionTransformer#convertToRule(
     *                  org.hawkular.btm.api.model.admin.InstrumentAction)
     */
    @Override
    public String convertToRuleAction(InstrumentAction action) {
        // NOTE: This class could use a templating mechanism to provide the
        // boilerplate rule - however it will be executed on the client side,
        // so want to avoid any unnecessary dependencies

        CollectorAction collectorAction = (CollectorAction) action;
        StringBuilder builder = new StringBuilder();

        builder.append("collector().");
        builder.append(getEntity());

        if (collectorAction.getDirection() == Direction.Request) {
            builder.append("Start(");
        } else {
            builder.append("End(");
        }

        String[] params = getParameters(collectorAction);
        for (int i = 0; i < params.length; i++) {
            builder.append(params[i]);
            builder.append(',');
        }

        if (collectorAction.getHeadersExpression() == null) {
            builder.append("null");
        } else {
            builder.append(collectorAction.getHeadersExpression());
        }
        builder.append(',');

        builder.append("createArrayBuilder()");

        for (String expr : collectorAction.getValueExpressions()) {
            builder.append(".add(");
            builder.append(expr);
            builder.append(')');
        }

        builder.append(".get()");

        builder.append(")");

        return builder.toString();
    }

    /**
     * This method identifies the entity being instrumented.
     *
     * @return The entity
     */
    protected abstract String getEntity();

    /**
     * This method supplies the array of fixed parameters.
     *
     * @param invocation The invocation details
     * @return The array of fixed parameters
     */
    protected abstract String[] getParameters(CollectorAction invocation);

}
