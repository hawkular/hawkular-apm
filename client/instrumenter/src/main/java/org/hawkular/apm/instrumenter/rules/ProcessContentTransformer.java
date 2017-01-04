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
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.ProcessContent;

/**
 * This class transforms the ProcessContent action type.
 *
 * @author gbrown
 */
public class ProcessContentTransformer implements InstrumentActionTransformer {

    @Override
    public Class<? extends InstrumentAction> getActionType() {
        return ProcessContent.class;
    }

    @Override
    public String convertToRuleAction(InstrumentAction action) {
        ProcessContent pcAction = (ProcessContent) action;
        StringBuilder builder = new StringBuilder(128);

        builder.append("collector().");

        if (pcAction.getDirection() == Direction.In) {
            builder.append("processIn(");
        } else {
            builder.append("processOut(");
        }

        builder.append("getRuleName(),null,createArrayBuilder()");

        for (String expr : pcAction.getValueExpressions()) {
            builder.append(".add(");
            builder.append(expr);
            builder.append(')');
        }

        builder.append(".get()");

        builder.append(")");

        return builder.toString();
    }

}
