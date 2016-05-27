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

import java.util.Map;

import org.hawkular.btm.api.internal.actions.helpers.JSON;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.Expression;
import org.hawkular.btm.api.model.config.btxn.JSONExpression;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.hawkular.btm.api.model.trace.Node;
import org.hawkular.btm.api.model.trace.Trace;

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

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.internal.actions.ExpressionHandler#init(
     *              org.hawkular.btm.api.model.config.btxn.Processor,
     *              org.hawkular.btm.api.model.config.btxn.ProcessorAction, boolean)
     */
    @Override
    public void init(Processor processor, ProcessorAction action, boolean predicate) {
        super.init(processor, action, predicate);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.internal.actions.ExpressionHandler#predicate(
     *              org.hawkular.btm.api.model.trace.Trace,
     *              org.hawkular.btm.api.model.trace.Node, org.hawkular.btm.api.model.config.Direction,
     *              java.util.Map, java.lang.Object[])
     */
    @Override
    public boolean test(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        return JSON.predicate(((JSONExpression)getExpression()).getJsonpath(),
                            getDataValue(trace, node, direction, headers, values));
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.internal.actions.ExpressionHandler#test(
     *              org.hawkular.btm.api.model.trace.Trace,
     *              org.hawkular.btm.api.model.trace.Node, org.hawkular.btm.api.model.config.Direction,
     *              java.util.Map, java.lang.Object[])
     */
    @Override
    public String evaluate(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        return JSON.evaluate(((JSONExpression)getExpression()).getJsonpath(),
                            getDataValue(trace, node, direction, headers, values));
    }

}
