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
package org.hawkular.btm.client.collector.internal.actions;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.ExpressionBasedAction;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * @author gbrown
 */
public abstract class ExpressionBasedActionHandler extends ProcessorActionHandler {

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
     */
    @Override
    public void init() {
        super.init();
        if (((ExpressionBasedAction) getAction()).getExpression() != null) {
            try {
                ParserContext ctx = new ParserContext();
                ctx.addPackageImport("org.hawkular.btm.client.collector.internal.helpers");

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
                log.log(Level.SEVERE, "Failed to compile expression for action '"
                        + getAction() + "'", t);
            }
        } else {
            log.severe("No action expression defined for processor action=" + getAction());
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
