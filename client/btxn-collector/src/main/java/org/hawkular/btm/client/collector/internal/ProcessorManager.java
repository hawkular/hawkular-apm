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
package org.hawkular.btm.client.collector.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.admin.BusinessTxnConfig;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;
import org.hawkular.btm.api.model.admin.Direction;
import org.hawkular.btm.api.model.admin.Processor;
import org.hawkular.btm.api.model.admin.ProcessorAction;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.InteractionNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.btxn.Service;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * This class manages the processors.
 *
 * @author gbrown
 */
public class ProcessorManager {

    private static Logger log = Logger.getLogger(ProcessorManager.class.getName());

    private Map<String, List<ProcessorWrapper>> processors = new HashMap<String, List<ProcessorWrapper>>();

    /**
     * This constructor initialises the processor manager with the configuration.
     *
     * @param config The configuration
     */
    public ProcessorManager(CollectorConfiguration config) {
        init(config);
    }

    /**
     * This method initialises the filter manager.
     *
     * @param config The configuration
     */
    protected void init(CollectorConfiguration config) {
        for (String btxn : config.getBusinessTransactions().keySet()) {
            BusinessTxnConfig btc = config.getBusinessTransactions().get(btxn);

            if (log.isLoggable(Level.FINE)) {
                log.fine("ProcessManager: initialise btxn '"+btxn+"' config="+btc
                        +" processors="+btc.getProcessors().size());
            }

            if (btc.getProcessors() != null && !btc.getProcessors().isEmpty()) {
                List<ProcessorWrapper> procs = new ArrayList<ProcessorWrapper>();

                for (int i = 0; i < btc.getProcessors().size(); i++) {
                    procs.add(new ProcessorWrapper(btc.getProcessors().get(i)));
                }

                processors.put(btxn, procs);
            }
        }
    }

    /**
     * This method determines whether the business transaction, for the supplied node
     * and request/response direction, will process available information.
     *
     * @param btxn The business transaction
     * @param node The current node
     * @param direction The direction
     * @return Whether processing instructions have been defined
     */
    public boolean isProcessed(BusinessTransaction btxn, Node node, Direction direction) {
        boolean ret=getMatchedProcessor(btxn, node, direction) != null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: isProcessed btxn="+btxn+" node="+node
                    +" direction="+direction+"? "+ret);
        }

        return ret;
    }

    /**
     * This method determines whether the business transaction, for the supplied node
     * and request/response direction, will process content information.
     *
     * @param btxn The business transaction
     * @param node The current node
     * @param direction The direction
     * @return Whether content processing instructions have been defined
     */
    public boolean isContentProcessed(BusinessTransaction btxn, Node node, Direction direction) {
        ProcessorWrapper processor=getMatchedProcessor(btxn, node, direction);
        boolean ret=false;

        if (processor != null) {
            ret = processor.hasAddContentAction();
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: isContentProcessed btxn="+btxn+" node="+node
                    +" direction="+direction+"? "+ret);
        }

        return ret;
    }

    /**
     * This method returns the processor associated with the supplied business
     * transaction. node and interaction direction.
     *
     * @param btxn The business transaction
     * @param node The node
     * @param direction The direction
     * @return The processor associated with the details, or null if not found
     */
    protected ProcessorWrapper getMatchedProcessor(BusinessTransaction btxn, Node node, Direction direction) {
        ProcessorWrapper matchedProcessor=null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: getMatchedProcessor btxn="+btxn+" node="+node
                                +" direction="+direction);
        }

        if (btxn.getName() != null && processors.containsKey(btxn.getName())) {
            List<ProcessorWrapper> procs = processors.get(btxn.getName());

            for (int i = 0; matchedProcessor == null && i < procs.size(); i++) {
                if (procs.get(i).isProcessed(btxn, node, direction)) {
                    matchedProcessor = procs.get(i);
                }
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: getMatchedProcessor return="+matchedProcessor);
        }

        return matchedProcessor;
    }

    /**
     * This method processes the supplied information against the configured processor
     * details for the business transaction.
     *
     * @param btxn The business transaction
     * @param node The node being processed
     * @param direction The direction
     * @param headers The headers
     * @param values The values
     */
    public void process(BusinessTransaction btxn, Node node, Direction direction,
            Map<String, ?> headers, Object... values) {

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: process btxn="+btxn+" node="+node
                    +" direction="+direction+" headers="+headers+" values="+values
                    +" : available processors="+processors);
        }

        if (btxn.getName() != null && processors.containsKey(btxn.getName())) {
            List<ProcessorWrapper> procs = processors.get(btxn.getName());

            if (log.isLoggable(Level.FINEST)) {
                log.finest("ProcessManager: btxn name="+btxn.getName()+" processors="+procs);
            }

            for (int i = 0; i < procs.size(); i++) {
                procs.get(i).process(btxn, node, direction, headers, values);
            }
        }
    }

    /**
     * This class provides the execution behaviour associated with the
     * information defined in the collector configuration processor
     * definition.
     *
     * @author gbrown
     */
    public class ProcessorWrapper {

        private Processor processor;

        private Predicate<String> uriFilter = null;

        private Predicate<String> faultFilter = null;

        private List<ProcessorActionWrapper> actions = new ArrayList<ProcessorActionWrapper>();

        /**
         * This constructor is initialised with the processor.
         *
         * @param processor The processor
         */
        public ProcessorWrapper(Processor processor) {
            this.processor = processor;

            init();
        }

        /**
         * This method initialises the processor.
         */
        protected void init() {
            if (processor.getUriFilter() != null) {
                uriFilter = Pattern.compile(processor.getUriFilter()).asPredicate();
            }

            if (processor.getFaultFilter() != null) {
                faultFilter = Pattern.compile(processor.getFaultFilter()).asPredicate();
            }

            for (int i = 0; i < processor.getActions().size(); i++) {
                actions.add(new ProcessorActionWrapper(processor.getActions().get(i)));
            }
        }

        /**
         * This method checks that this processor matches the supplied business txn
         * name and node details.
         *
         * @param btxn The business transaction
         * @param node The node
         * @param direction The direction
         * @return Whether the supplied details would be processed by this processor
         */
        public boolean isProcessed(BusinessTransaction btxn, Node node, Direction direction) {
            boolean ret=false;

            if (processor.getNodeType() == node.getType()
                    && processor.getDirection() == direction) {

                // If URI filter regex expression defined, then verify whether
                // node URI matches
                if (uriFilter == null
                        || uriFilter.test(node.getUri())) {
                    ret = true;
                }
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("ProcessManager/Processor: isProcessed btxn="+btxn+" node="+node
                        +" direction="+direction+"? "+ret);
            }

            return ret;
        }

        /**
         * This method indicates whether this processor has an 'AddContent' action.
         *
         * @return Whether the processor has an 'AddContent' action
         */
        public boolean hasAddContentAction() {
            for (int i=0; i < actions.size(); i++) {
                if (actions.get(i).action.getActionType() == ProcessorAction.ActionType.AddContent) {
                    return true;
                }
            }
            return false;
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
         */
        public void process(BusinessTransaction btxn, Node node, Direction direction,
                Map<String, ?> headers, Object[] values) {

            if (log.isLoggable(Level.FINEST)) {
                log.finest("ProcessManager/Processor: process btxn="+btxn+" node="+node
                        +" direction="+direction+" headers="+headers+" values="+values);

                if (values != null) {
                    for (int i=0; i < values.length; i++) {
                        log.finest("        [value "+i+"] = "+values[i]);
                    }
                }
            }

            if (processor.getNodeType() == node.getType()
                    && processor.getDirection() == direction) {

                // If URI filter regex expression defined, then verify whether
                // node URI matches
                if (uriFilter != null
                        && !uriFilter.test(node.getUri())) {
                    return;
                }

                // Check if operation has been specified, and node is Service
                if (processor.getOperation() != null && node.getType() == NodeType.Service
                        && !processor.getOperation().equals(((Service)node).getOperation())) {
                    return;
                }

                // If fault filter not defined, then node cannot have a fault
                if (faultFilter == null && node.getFault() != null) {
                    return;
                }

                // If fault filter regex expression defined, then verify whether
                // node fault string matches.
                if (faultFilter != null && (node.getFault() == null
                        || !faultFilter.test(node.getFault()))) {
                    return;
                }

                for (int i = 0; i < actions.size(); i++) {
                    actions.get(i).process(btxn, node, direction, headers, values);
                }
            }
        }

        public String toString() {
            return processor.toString();
        }
    }

    /**
     * This class provides the execution behaviour associated with the
     * information defined in the collector configuration processor
     * definition.
     *
     * @author gbrown
     */
    public class ProcessorActionWrapper {

        private ProcessorAction action;

        private Object compiledPredicate = null;

        private Object compiledAction = null;

        private boolean usesHeaders=false;
        private boolean usesContent=false;

        /**
         * This constructor is initialised with the processor action.
         *
         * @param action The processor action
         */
        public ProcessorActionWrapper(ProcessorAction action) {
            this.action = action;

            init();
        }

        /**
         * This method initialises the processor action.
         */
        protected void init() {
            try {
                ParserContext ctx = new ParserContext();
                ctx.addPackageImport("org.hawkular.btm.client.collector.internal.helpers");

                if (action.getPredicate() != null) {
                    compiledPredicate = MVEL.compileExpression(action.getPredicate(), ctx);

                    if (compiledPredicate == null) {
                        log.severe("Failed to compile predicate '"+action.getPredicate()+"'");
                    } else if (log.isLoggable(Level.FINE)) {
                        log.fine("Initialised processor predicate '"+action.getPredicate()
                                +"' = "+compiledPredicate);
                    }

                    // Check if headers referenced
                    usesHeaders = action.getExpression().indexOf("headers.") != -1;
                    usesContent = action.getExpression().indexOf("values[") != -1;
                }
                if (action.getExpression() != null) {
                    compiledAction = MVEL.compileExpression(action.getExpression(), ctx);

                    if (compiledAction == null) {
                        log.severe("Failed to compile action '"+action.getExpression()+"'");
                    } else if (log.isLoggable(Level.FINE)) {
                        log.fine("Initialised processor action '"+action.getExpression()
                                +"' = "+compiledAction);
                    }

                    // Check if headers referenced
                    if (!usesHeaders) {
                        usesHeaders = action.getExpression().indexOf("headers.") != -1;
                    }
                    if (!usesContent) {
                        usesContent = action.getExpression().indexOf("values[") != -1;
                    }
                } else {
                    log.severe("No action expression defined for processor action=" + action);
                }
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Failed to compile processor predicate '"
                            +action.getPredicate()+"' or action '"+action.getExpression()+"'", t);
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
         */
        public void process(BusinessTransaction btxn, Node node, Direction direction,
                Map<String, ?> headers, Object[] values) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("ProcessManager/Processor/Action["+action+"]: process btxn="+btxn+" node="+node
                        +" direction="+direction+" headers="+headers+" values="+values);

                if (values != null) {
                    for (int i=0; i < values.length; i++) {
                        log.finest("        [value "+i+"] = "+values[i]);
                    }
                }
            }

            // If expressions don't use headers or content values, then just process each
            // time process action is called, otherwise determine if the headers or content
            // have been provided
            if (usesHeaders || usesContent) {
                // Check if headers supplied if expressions requires them
                if (usesHeaders && (headers == null || headers.isEmpty())) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("ProcessManager/Processor/Action["+action+"]: uses headers but not supplied");
                    }
                    return;
                }

                // Check if content values supplied if expressions requires them
                if (usesContent && (values == null || values.length == 0)) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("ProcessManager/Processor/Action["+action+"]: uses content values but not supplied");
                    }
                    return;
                }
            }

            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("btxn", btxn);
            vars.put("node", node);
            vars.put("headers", headers);
            vars.put("values", values);

            if (compiledPredicate != null
                    && !((Boolean) MVEL.executeExpression(compiledPredicate, vars))) {
                return;
            }

            Object result = MVEL.executeExpression(compiledAction, vars);

            if (action.getActionType() != null) {

                if (result == null) {
                    log.warning("Result for action type '" + action.getActionType()
                            + "' and action '" + action.getExpression() + "' was null");
                } else {
                    String value = null;

                    if (result.getClass() != String.class) {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("Converting result for action type '" + action.getActionType()
                                    + "' and action '" + action.getExpression()
                                    + "' to a String, was: "+result.getClass());
                        }
                        value = result.toString();
                    } else {
                        value = (String)result;
                    }

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("ProcessManager/Processor/Action: value="+value);
                    }

                    switch (action.getActionType()) {
                        case SetDetail:
                            node.getDetails().put(action.getName(), value);
                            break;
                        case SetFault:
                            node.setFault(value);
                            break;
                        case AddContent:
                            if (node.interactionNode()) {
                                if (direction == Direction.Request) {
                                    ((InteractionNode) node).getRequest().addContent(action.getName(),
                                            action.getType(), value);
                                } else {
                                    ((InteractionNode) node).getResponse().addContent(action.getName(),
                                            action.getType(), value);
                                }
                            } else {
                                log.warning("Attempt to add content to a non-interaction based node type '"
                                        + node.getType() + "'");
                            }
                            break;
                        case SetProperty:
                            btxn.getProperties().put(action.getName(), value);
                            break;
                        case AddCorrelationId:
                            node.getCorrelationIds().add(
                                    new CorrelationIdentifier(action.getScope(), value));
                            break;
                        default:
                            log.warning("Unhandled action type '"+action.getActionType()+"'");
                            break;
                    }
                }
            }
        }
    }
}
