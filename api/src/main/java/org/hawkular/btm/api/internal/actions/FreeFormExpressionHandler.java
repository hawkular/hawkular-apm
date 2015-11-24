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
package org.hawkular.btm.api.internal.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.Severity;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Issue;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.ProcessorIssue;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.Expression;
import org.hawkular.btm.api.model.config.btxn.FreeFormExpression;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * This class provides the JSON expression handler implementation.
 *
 * @author gbrown
 */
public class FreeFormExpressionHandler extends ExpressionHandler {

    private static final Logger log = Logger.getLogger(FreeFormExpressionHandler.class.getName());

    private Object compiledExpression;

    /**
     * @param expression
     */
    public FreeFormExpressionHandler(Expression expression) {
        super(expression);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.internal.actions.ExpressionHandler#init(
     *              org.hawkular.btm.api.model.config.btxn.Processor,
     *              org.hawkular.btm.api.model.config.btxn.ProcessorAction, boolean)
     */
    @Override
    public void init(Processor processor, ProcessorAction action, boolean predicate) {
        try {
            ParserContext ctx = new ParserContext();
            ctx.addPackageImport("org.hawkular.btm.api.internal.actions.helpers");

            String text = ((FreeFormExpression) getExpression()).getValue();

            compiledExpression = MVEL.compileExpression(text, ctx);

            if (compiledExpression == null) {
                log.severe("Failed to compile compiledExpression '" + text + "'");
            } else if (log.isLoggable(Level.FINE)) {
                log.fine("Initialised free form expression '" + text
                        + "' = " + compiledExpression);
            }
        } catch (Throwable t) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Failed to initialise expression", t);
            }

            ProcessorIssue pi = new ProcessorIssue();
            pi.setProcessor(processor.getDescription());
            pi.setAction(action.getDescription());
            pi.setSeverity(Severity.Error);
            pi.setDescription(t.getMessage());

            if (getIssues() == null) {
                setIssues(new ArrayList<Issue>());
            }
            getIssues().add(pi);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.internal.actions.ExpressionHandler#predicate(
     *              org.hawkular.btm.api.model.btxn.BusinessTransaction,
     *              org.hawkular.btm.api.model.btxn.Node, org.hawkular.btm.api.model.config.Direction,
     *              java.util.Map, java.lang.Object[])
     */
    @Override
    public boolean test(BusinessTransaction btxn, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (compiledExpression != null) {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("btxn", btxn);
            vars.put("node", node);
            vars.put("headers", headers);
            vars.put("values", values);

            Object result = MVEL.executeExpression(compiledExpression, vars);

            if (result == null) {
                log.warning("Result for expression '" + getExpression() + "' was null");
            } else {
                boolean value = false;

                if (result.getClass() == Boolean.class) {
                    value = ((Boolean) result).booleanValue();
                } else if (result.getClass() == String.class) {
                    value = new Boolean((String) result);
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Expression '" + getExpression()
                                + "' returned non-boolean type: " + result.getClass());
                    }
                }

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Free form expression value=" + value);
                }

                return value;
            }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.internal.actions.ExpressionHandler#test(
     *              org.hawkular.btm.api.model.btxn.BusinessTransaction,
     *              org.hawkular.btm.api.model.btxn.Node, org.hawkular.btm.api.model.config.Direction,
     *              java.util.Map, java.lang.Object[])
     */
    @Override
    public String evaluate(BusinessTransaction btxn, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (compiledExpression != null) {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("btxn", btxn);
            vars.put("node", node);
            vars.put("headers", headers);
            vars.put("values", values);

            Object result = MVEL.executeExpression(compiledExpression, vars);

            if (result == null) {
                log.warning("Result for expression '" + getExpression() + "' was null");
            } else {
                String value = null;

                if (result.getClass() != String.class) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Converting result for expression '" + getExpression()
                                + "' to a String, was: " + result.getClass());
                    }
                    value = result.toString();
                } else {
                    value = (String) result;
                }

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Free form expression value=" + value);
                }

                return value;
            }
        }

        return null;
    }

}
