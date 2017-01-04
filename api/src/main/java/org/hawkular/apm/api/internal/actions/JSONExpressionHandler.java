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

import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.internal.actions.helpers.JSON;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.Expression;
import org.hawkular.apm.api.model.config.txn.JSONExpression;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * This class provides the JSON expression handler implementation.
 *
 * @author gbrown
 */
public class JSONExpressionHandler extends DataExpressionHandler {

    /**
     * @param expression
     */
    public JSONExpressionHandler(Expression expression) {
        super(expression);
    }

    @Override
    public List<ConfigMessage> init(Processor processor, ProcessorAction action, boolean predicate) {
        return super.init(processor, action, predicate);
    }

    @Override
    public boolean test(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        return JSON.predicate(((JSONExpression)getExpression()).getJsonpath(),
                            getDataValue(trace, node, direction, headers, values));
    }

    @Override
    public String evaluate(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        return JSON.evaluate(((JSONExpression)getExpression()).getJsonpath(),
                            getDataValue(trace, node, direction, headers, values));
    }

}
