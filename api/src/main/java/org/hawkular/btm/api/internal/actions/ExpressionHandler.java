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

import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Issue;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.Expression;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;

/**
 * This abstract class provides the base implementation for expression handlers.
 *
 * @author gbrown
 */
public abstract class ExpressionHandler {

    private static final Logger log = Logger.getLogger(ExpressionHandler.class.getName());

    private Expression expression;

    private List<Issue> issues;

    public ExpressionHandler(Expression expression) {
        this.setExpression(expression);
    }

    /**
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * @param expression the expression to set
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    /**
     * This method indicates whether the expression uses headers.
     *
     * @return Whether headers are used
     */
    public boolean isUsesHeaders() {
        return false;
    }

    /**
     * This method indicates whether the expression uses content.
     *
     * @return Whether content is used
     */
    public boolean isUsesContent() {
        return false;
    }

    /**
     * @return the issues
     */
    public List<Issue> getIssues() {
        return issues;
    }

    /**
     * @param issues the issues to set
     */
    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    /**
     * This method initialises the expression handler.
     *
     * @param processor The processor
     * @param action The action
     * @param predicate Whether the expression is a predicate
     */
    public abstract void init(Processor processor, ProcessorAction action, boolean predicate);

    /**
     * This method evaluates the supplied information against this
     * expression representing a predicate.
     *
     * @param btxn The business transaction
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return The predicate result
     */
    public abstract boolean test(BusinessTransaction btxn, Node node, Direction direction,
            Map<String, ?> headers, Object[] values);

    /**
     * This method evaluates the supplied information against this
     * expression.
     *
     * @param btxn The business transaction
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return The evaluated result
     */
    public abstract String evaluate(BusinessTransaction btxn, Node node, Direction direction,
            Map<String, ?> headers, Object[] values);

}
