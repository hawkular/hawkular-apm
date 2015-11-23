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
import org.hawkular.btm.api.model.config.btxn.ExpressionBasedAction;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * @author gbrown
 */
public abstract class ExpressionBasedActionHandler extends ProcessorActionHandler {

    /**  */
    public static final String EXPRESSION_HAS_NOT_BEEN_DEFINED = "Expression has not been defined";

    private static final Logger log = Logger.getLogger(ExpressionBasedActionHandler.class.getName());

    private Object compiledAction = null;

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public ExpressionBasedActionHandler(ProcessorAction action) {
        super(action);
    }

    /**
     * This method initialises the process action handler.
     *
     * @param processor The processor
     */
    @Override
    public void init(Processor processor) {
        super.init(processor);
        if (((ExpressionBasedAction) getAction()).getExpression() != null) {
            try {
                ParserContext ctx = new ParserContext();
                ctx.addPackageImport("org.hawkular.btm.api.internal.actions.helpers");

                String text = ((ExpressionBasedAction) getAction()).getExpression().evaluateText();

                compiledAction = MVEL.compileExpression(text, ctx);

                if (compiledAction == null) {
                    log.severe("Failed to compile action '" + text + "'");
                } else if (log.isLoggable(Level.FINE)) {
                    log.fine("Initialised processor action '" + text
                            + "' = " + compiledAction);
                }

                // Check if headers referenced
                if (!isUsesHeaders()) {
                    setUsesHeaders(text.indexOf("headers.") != -1);
                }
                if (!isUsesContent()) {
                    setUsesContent(text.indexOf("values[") != -1);
                }
            } catch (Throwable t) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Failed to compile expression for action '"
                            + getAction() + "'", t);
                }

                ProcessorIssue pi = new ProcessorIssue();
                pi.setProcessor(processor.getDescription());
                pi.setAction(getAction().getDescription());
                pi.setField("expression");
                pi.setSeverity(Severity.Error);
                pi.setDescription(t.getMessage());

                if (getIssues() == null) {
                    setIssues(new ArrayList<Issue>());
                }
                getIssues().add(pi);
            }
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.fine("No action expression defined for processor action= "
                        + getAction());
            }

            ProcessorIssue pi = new ProcessorIssue();
            pi.setProcessor(processor.getDescription());
            pi.setAction(getAction().getDescription());
            pi.setField("expression");
            pi.setSeverity(Severity.Error);
            pi.setDescription(EXPRESSION_HAS_NOT_BEEN_DEFINED);

            if (getIssues() == null) {
                setIssues(new ArrayList<Issue>());
            }
            getIssues().add(pi);
        }
    }

    /**
     * This method returns the value, associated with the expression, for the
     * supplied data.
     *
     * @param btxn The business transaction
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return The result of the expression
     */
    protected String getValue(BusinessTransaction btxn, Node node, Direction direction,
            Map<String, ?> headers, Object[] values) {
        if (compiledAction != null) {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("btxn", btxn);
            vars.put("node", node);
            vars.put("headers", headers);
            vars.put("values", values);

            Object result = MVEL.executeExpression(compiledAction, vars);

            if (result == null) {
                log.warning("Result for action '" + getAction() + "' was null");
            } else {
                String value = null;

                if (result.getClass() != String.class) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Converting result for action '" + getAction()
                                + "' to a String, was: " + result.getClass());
                    }
                    value = result.toString();
                } else {
                    value = (String) result;
                }

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("ProcessManager/Processor/Action: value=" + value);
                }

                return value;
            }
        }

        return null;
    }
}
