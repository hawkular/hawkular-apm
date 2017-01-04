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

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.Severity;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.trace.Issue;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.ProcessorIssue;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * @author gbrown
 */
public abstract class ProcessorActionHandler {

    private static final Logger log = Logger.getLogger(ProcessorActionHandler.class.getName());

    private ProcessorAction action;

    private ExpressionHandler predicate;

    private boolean usesHeaders = false;
    private boolean usesContent = false;

    private List<Issue> issues;

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
     * This method initialises the process action handler.
     *
     * @param processor The processor
     */
    public void init(Processor processor) {
        if (action.getPredicate() != null) {
            try {
                predicate = ExpressionHandlerFactory.getHandler(action.getPredicate());

                predicate.init(processor, getAction(), true);

                if (!isUsesHeaders()) {
                    setUsesHeaders(predicate.isUsesHeaders());
                }
                if (!isUsesContent()) {
                    setUsesContent(predicate.isUsesContent());
                }

            } catch (Throwable t) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Failed to initialise predicate for action '"
                            + action + "'", t);
                }

                ProcessorIssue pi = new ProcessorIssue();
                pi.setProcessor(processor.getDescription());
                pi.setAction(action.getDescription());
                pi.setSeverity(Severity.Error);
                pi.setDescription(t.getMessage());

                if (issues == null) {
                    issues = new ArrayList<Issue>();
                }
                issues.add(pi);
            }
        }
    }

    /**
     * This method processes the supplied information to extract the relevant
     * details.
     *
     * @param trace The trace
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return Whether the data was processed
     */
    public boolean process(Trace trace, Node node, Direction direction,
            Map<String, ?> headers, Object[] values) {

        // Associate any initialisation issues with the node
        if (issues != null) {
            node.getIssues().addAll(issues);
        }

        if (predicate != null) {
            return predicate.test(trace, node, direction, headers, values);
        }

        return true;
    }

}
