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
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * @author gbrown
 */
public abstract class ProcessorActionHandler {

    private static final Logger log = Logger.getLogger(ProcessorActionHandler.class.getName());

    private ProcessorAction action;

    private Object compiledPredicate = null;

    private boolean usesHeaders = false;
    private boolean usesContent = false;

    public ProcessorActionHandler(ProcessorAction action) {
        this.setAction(action);
    }

    /**
     * @return the action
     */
    public ProcessorAction getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    protected void setAction(ProcessorAction action) {
        this.action = action;
    }

    /**
     * @return the usesHeaders
     */
    public boolean isUsesHeaders() {
        return usesHeaders;
    }

    /**
     * @param usesHeaders the usesHeaders to set
     */
    public void setUsesHeaders(boolean usesHeaders) {
        this.usesHeaders = usesHeaders;
    }

    /**
     * @return the usesContent
     */
    public boolean isUsesContent() {
        return usesContent;
    }

    /**
     * @param usesContent the usesContent to set
     */
    public void setUsesContent(boolean usesContent) {
        this.usesContent = usesContent;
    }

    /**
     * This method initialises the process action handler.
     */
    public void init() {
        if (action.getPredicate() != null) {
            try {
                ParserContext ctx = new ParserContext();
                ctx.addPackageImport("org.hawkular.btm.client.collector.internal.helpers");

                String text = action.getPredicate().predicateText();

                compiledPredicate = MVEL.compileExpression(text, ctx);

                if (compiledPredicate == null) {
                    log.severe("Failed to compile action predicate '" + text + "'");
                } else if (log.isLoggable(Level.FINE)) {
                    log.fine("Initialised processor action predicate '" + text
                            + "' = " + compiledPredicate);
                }

                // Check if headers referenced
                setUsesHeaders(text.indexOf("headers.") != -1);
                setUsesContent(text.indexOf("values[") != -1);
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Failed to compile predicate for action '"
                        + action + "'", t);
            }
        }
    }

    /**
     * This method processes the supplied information to extract the relevant
     * details.
     *
     * @param btxn The business transaction
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return Whether the data was processed
     */
    public boolean process(BusinessTransaction btxn, Node node, Direction direction,
            Map<String, ?> headers, Object[] values) {
        if (compiledPredicate != null) {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("btxn", btxn);
            vars.put("node", node);
            vars.put("headers", headers);
            vars.put("values", values);

            return (Boolean) MVEL.executeExpression(compiledPredicate, vars);
        }

        return true;
    }

}
