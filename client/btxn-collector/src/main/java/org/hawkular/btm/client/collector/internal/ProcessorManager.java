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
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.InteractionNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.Processor;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
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
            init(btxn, btc);
        }
    }

    /**
     * This method initialises the processors associated with the supplied
     * business transaction configuration.
     *
     * @param btxn The business transaction name
     * @param btc The configuration
     */
    public void init(String btxn, BusinessTxnConfig btc) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("ProcessManager: initialise btxn '" + btxn + "' config=" + btc
                    + " processors=" + btc.getProcessors().size());
        }

        if (btc.getProcessors() != null && !btc.getProcessors().isEmpty()) {
            List<ProcessorWrapper> procs = new ArrayList<ProcessorWrapper>();

            for (int i = 0; i < btc.getProcessors().size(); i++) {
                procs.add(new ProcessorWrapper(btc.getProcessors().get(i)));
            }

            synchronized (processors) {
                processors.put(btxn, procs);
            }
        } else {
            synchronized (processors) {
                processors.remove(btxn);
            }
        }
    }

    /**
     * This method determines whether the business transaction, for the supplied node
     * and in/out direction, will process available information.
     *
     * @param btxn The business transaction
     * @param node The current node
     * @param direction The direction
     * @return Whether processing instructions have been defined
     */
    public boolean isProcessed(BusinessTransaction btxn, Node node, Direction direction) {
        boolean ret = false;

        if (btxn.getName() != null) {
            List<ProcessorWrapper> procs = null;

            synchronized (processors) {
                procs = processors.get(btxn.getName());
            }

            if (procs != null) {
                for (int i = 0; !ret && i < procs.size(); i++) {
                    ret = procs.get(i).isProcessed(btxn, node, direction);
                }
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: isProcessed btxn=" + btxn + " node=" + node
                    + " direction=" + direction + "? " + ret);
        }

        return ret;
    }

    /**
     * This method determines whether the business transaction, for the supplied node
     * and in/out direction, will process content information.
     *
     * @param btxn The business transaction
     * @param node The current node
     * @param direction The direction
     * @return Whether content processing instructions have been defined
     */
    public boolean isContentProcessed(BusinessTransaction btxn, Node node, Direction direction) {
        boolean ret = false;

        if (btxn.getName() != null) {
            List<ProcessorWrapper> procs = null;

            synchronized (processors) {
                procs = processors.get(btxn.getName());
            }

            if (procs != null) {
                for (int i = 0; !ret && i < procs.size(); i++) {
                    ret = procs.get(i).isProcessed(btxn, node, direction)
                            && procs.get(i).usesContent();
                }
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ProcessManager: isContentProcessed btxn=" + btxn + " node=" + node
                    + " direction=" + direction + "? " + ret);
        }

        return ret;
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
            log.finest("ProcessManager: process btxn=" + btxn + " node=" + node
                    + " direction=" + direction + " headers=" + headers + " values=" + values
                    + " : available processors=" + processors);
        }

        if (btxn.getName() != null) {
            List<ProcessorWrapper> procs = null;

            synchronized (processors) {
                procs = processors.get(btxn.getName());
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("ProcessManager: btxn name=" + btxn.getName() + " processors=" + procs);
            }

            if (procs != null) {
                for (int i = 0; i < procs.size(); i++) {
                    procs.get(i).process(btxn, node, direction, headers, values);
                }
            }
        }
    }

    /**
     * @return the processors
     */
    protected Map<String, List<ProcessorWrapper>> getProcessors() {
        return processors;
    }

    /**
     * @param processors the processors to set
     */
    protected void setProcessors(Map<String, List<ProcessorWrapper>> processors) {
        this.processors = processors;
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

        private Object compiledPredicate = null;

        private List<ProcessorActionWrapper> actions = new ArrayList<ProcessorActionWrapper>();

        private boolean usesHeaders = false;
        private boolean usesContent = false;

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

            try {
                ParserContext ctx = new ParserContext();
                ctx.addPackageImport("org.hawkular.btm.client.collector.internal.helpers");

                if (processor.getPredicate() != null) {
                    String text = processor.getPredicate().predicateText();

                    compiledPredicate = MVEL.compileExpression(text, ctx);

                    if (compiledPredicate == null) {
                        log.severe("Failed to compile pr ocessorpredicate '" + text + "'");
                    } else if (log.isLoggable(Level.FINE)) {
                        log.fine("Initialised processor predicate '" + text
                                + "' = " + compiledPredicate);
                    }

                    // Check if headers referenced
                    usesHeaders = text.indexOf("headers.") != -1;
                    usesContent = text.indexOf("values[") != -1;
                }
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Failed to compile processor predicate '"
                        + processor.getPredicate() + "'", t);
            }

            for (int i = 0; i < processor.getActions().size(); i++) {
                ProcessorActionWrapper paw = new ProcessorActionWrapper(processor.getActions().get(i));
                if (!usesHeaders) {
                    usesHeaders = paw.usesHeaders();
                }
                if (!usesContent) {
                    usesContent = paw.usesContent();
                }
                actions.add(paw);
            }
        }

        /**
         * This method returns the processor.
         *
         * @return The processor
         */
        protected Processor getProcessor() {
            return processor;
        }

        /**
         * This method indicates whether the process action uses headers.
         *
         * @return Whether headers are used
         */
        public boolean usesHeaders() {
            return usesHeaders;
        }

        /**
         * This method indicates whether the process action uses content values.
         *
         * @return Whether content is used
         */
        public boolean usesContent() {
            return usesContent;
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
            boolean ret = false;

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
                log.finest("ProcessManager/Processor: isProcessed btxn=" + btxn + " node=" + node
                        + " direction=" + direction + "? " + ret);
            }

            return ret;
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
                log.finest("ProcessManager/Processor: process btxn=" + btxn + " node=" + node
                        + " direction=" + direction + " headers=" + headers + " values=" + values);

                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        log.finest("        [value " + i + "] = " + values[i]);
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

                // Check if operation has been specified, and node is Component
                if (processor.getOperation() != null && node.getType() == NodeType.Component
                        && !processor.getOperation().equals(((Component) node).getOperation())) {
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

                if (compiledPredicate != null) {
                    Map<String, Object> vars = new HashMap<String, Object>();
                    vars.put("btxn", btxn);
                    vars.put("node", node);
                    vars.put("headers", headers);
                    vars.put("values", values);

                    if (!((Boolean) MVEL.executeExpression(compiledPredicate, vars))) {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("ProcessManager/Processor: process - predicate returned false");
                        }
                        return;
                    }
                }

                for (int i = 0; i < actions.size(); i++) {
                    actions.get(i).process(btxn, node, direction, headers, values);
                }
            }
        }

        @Override
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

        private boolean usesHeaders = false;
        private boolean usesContent = false;

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
                    String text = action.getPredicate().predicateText();

                    compiledPredicate = MVEL.compileExpression(text, ctx);

                    if (compiledPredicate == null) {
                        log.severe("Failed to compile action predicate '" + text + "'");
                    } else if (log.isLoggable(Level.FINE)) {
                        log.fine("Initialised processor action predicate '" + text
                                + "' = " + compiledPredicate);
                    }

                    // Check if headers referenced
                    usesHeaders = text.indexOf("headers.") != -1;
                    usesContent = text.indexOf("values[") != -1;
                }
                if (action.getExpression() != null) {
                    String text = action.getExpression().evaluateText();

                    compiledAction = MVEL.compileExpression(text, ctx);

                    if (compiledAction == null) {
                        log.severe("Failed to compile action '" + text + "'");
                    } else if (log.isLoggable(Level.FINE)) {
                        log.fine("Initialised processor action '" + text
                                + "' = " + compiledAction);
                    }

                    // Check if headers referenced
                    if (!usesHeaders) {
                        usesHeaders = text.indexOf("headers.") != -1;
                    }
                    if (!usesContent) {
                        usesContent = text.indexOf("values[") != -1;
                    }
                } else {
                    log.severe("No action expression defined for processor action=" + action);
                }
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Failed to compile processor (action) predicate '"
                        + action.getPredicate() + "' or action '" + action.getExpression() + "'", t);
            }
        }

        /**
         * This method indicates whether the process action uses headers.
         *
         * @return Whether headers are used
         */
        public boolean usesHeaders() {
            return usesHeaders;
        }

        /**
         * This method indicates whether the process action uses content values.
         *
         * @return Whether content is used
         */
        public boolean usesContent() {
            return usesContent;
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
                log.finest("ProcessManager/Processor/Action[" + action + "]: process btxn=" + btxn + " node=" + node
                        + " direction=" + direction + " headers=" + headers + " values=" + values);

                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        log.finest("        [value " + i + "] = " + values[i]);
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
                        log.finest("ProcessManager/Processor/Action[" + action + "]: uses headers but not supplied");
                    }
                    return;
                }

                // Check if content values supplied if expressions requires them
                if (usesContent && (values == null || values.length == 0)) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("ProcessManager/Processor/Action[" + action
                                + "]: uses content values but not supplied");
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
                                    + "' to a String, was: " + result.getClass());
                        }
                        value = result.toString();
                    } else {
                        value = (String) result;
                    }

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("ProcessManager/Processor/Action: value=" + value);
                    }

                    switch (action.getActionType()) {
                        case SetDetail:
                            node.getDetails().put(action.getName(), value);
                            break;
                        case SetFault:
                            node.setFault(value);
                            break;
                        case SetFaultDescription:
                            node.setFaultDescription(value);
                            break;
                        case AddContent:
                            if (node.interactionNode()) {
                                if (direction == Direction.In) {
                                    ((InteractionNode) node).getIn().addContent(action.getName(),
                                            action.getType(), value);
                                } else {
                                    ((InteractionNode) node).getOut().addContent(action.getName(),
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
                            log.warning("Unhandled action type '" + action.getActionType() + "'");
                            break;
                    }
                }
            }
        }
    }
}
