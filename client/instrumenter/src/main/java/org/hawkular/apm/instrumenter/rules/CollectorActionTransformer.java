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
package org.hawkular.apm.instrumenter.rules;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.CollectorAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentAction;

/**
 * This class transforms the InstrumentInvocation type.
 *
 * @author gbrown
 */
public abstract class CollectorActionTransformer implements InstrumentActionTransformer {

    @Override
    public String convertToRuleAction(InstrumentAction action) {
        // NOTE: This class could use a templating mechanism to provide the
        // boilerplate rule - however it will be executed on the client side,
        // so want to avoid any unnecessary dependencies

        CollectorAction collectorAction = (CollectorAction) action;
        StringBuilder builder = new StringBuilder(64);

        builder.append("collector().");
        builder.append(getEntity());

        if (collectorAction.getDirection() == Direction.In) {
            builder.append("Start(");
        } else {
            builder.append("End(");
        }

        builder.append("getRuleName(),");

        String[] params = getParameters(collectorAction);
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(params[i]);
        }

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
