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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.DataExpression;
import org.hawkular.apm.api.model.config.txn.DataSource;
import org.hawkular.apm.api.model.config.txn.Expression;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * This class provides the JSON expression handler implementation.
 *
 * @author gbrown
 */
public abstract class DataExpressionHandler extends ExpressionHandler {

    private DataSource source;
    private String key;
    private int index = 0;

    /**
     * @param expression
     */
    public DataExpressionHandler(Expression expression) {
        super(expression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUsesHeaders() {
        return ((DataExpression) getExpression()).getSource() == DataSource.Header;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUsesContent() {
        return ((DataExpression) getExpression()).getSource() == DataSource.Content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConfigMessage> init(Processor processor, ProcessorAction action, boolean predicate) {
        DataExpression expr = (DataExpression) getExpression();

        source = expr.getSource();
        key = expr.getKey();

        if (source == DataSource.Content) {
            index = Integer.parseInt(key);
        }

        return new ArrayList<>();
    }

    /**
     * This method returns the data value associated with the requested data
     * source and key..
     *
     * @param trace The trace
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return The required data value
     */
    protected Object getDataValue(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (source == DataSource.Content) {
            return values[index];
        } else if (source == DataSource.Header) {
            return headers.get(key);
        }
        return null;
    }

}
