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
package org.hawkular.btm.client.collector;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.InteractionNode;
import org.hawkular.btm.api.model.btxn.Message;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.btxn.Service;
import org.hawkular.btm.api.services.AdminService;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.api.services.ServiceResolver;
import org.hawkular.btm.client.api.BusinessTransactionCollector;
import org.hawkular.btm.client.api.SessionManager;
import org.hawkular.btm.client.collector.internal.BusinessTransactionReporter;
import org.hawkular.btm.client.collector.internal.FilterManager;
import org.hawkular.btm.client.collector.internal.FragmentBuilder;
import org.hawkular.btm.client.collector.internal.FragmentManager;
import org.hawkular.btm.client.collector.internal.ProcessorManager;

/**
 * @author gbrown
 */
public class DefaultBusinessTransactionCollector implements BusinessTransactionCollector, SessionManager {

    private static final Logger log = Logger.getLogger(DefaultBusinessTransactionCollector.class.getName());

    private FragmentManager fragmentManager = new FragmentManager();

    private FilterManager filterManager;

    private ProcessorManager processorManager;

    private BusinessTransactionReporter reporter = new BusinessTransactionReporter();

    private Map<String, FragmentBuilder> links = new ConcurrentHashMap<String, FragmentBuilder>();

    private static final Level warningLogLevel = Level.WARNING;

    {
        // Obtain the admin service
        CompletableFuture<AdminService> asFuture =
                ServiceResolver.getSingletonService(AdminService.class);

        asFuture.whenComplete(new BiConsumer<AdminService, Throwable>() {

            @Override
            public void accept(AdminService as, Throwable t) {
                setAdminService(as);
            }
        });
    }

    /**
     * This method sets the admin service.
     *
     * @param as The admin service
     */
    public void setAdminService(AdminService as) {
        CollectorConfiguration config = as.getConfiguration(null, null, null);

        if (config != null) {
            filterManager = new FilterManager(config);
            reporter.init(config);
            try {
                processorManager = new ProcessorManager(config);
            } catch (Throwable t) {
                // TODO: log
                t.printStackTrace();
            }
        }
    }

    /**
     * @return the businessTransactionService
     */
    public BusinessTransactionService getBusinessTransactionService() {
        return reporter.getBusinessTransactionService();
    }

    /**
     * @param businessTransactionService the businessTransactionService to set
     */
    public void setBusinessTransactionService(BusinessTransactionService businessTransactionService) {
        reporter.setBusinessTransactionService(businessTransactionService);
    }

    /**
     * @return the tenantId
     */
    public String getTenantId() {
        return reporter.getTenantId();
    }

    /**
     * @param tenantId the tenantId to set
     */
    public void setTenantId(String tenantId) {
        reporter.setTenantId(tenantId);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Ignoring attempt to set business transaction name to null");
            }
            return;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set business transaction name=" + name);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.getBusinessTransaction().setName(name);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setName failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#getName()
     */
    @Override
    public String getName() {
        String ret = null;

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                ret = builder.getBusinessTransaction().getName();
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "getName failed", t);
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction name=" + ret);
        }

        if (ret == null) {
            ret = "";
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#consumerStart(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public void consumerStart(String uri, String type, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer start: type=" + type + " uri=" + uri + " id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Consumer consumer = new Consumer();
                consumer.setEndpointType(type);
                consumer.setUri(uri);

                if (id != null) {
                    consumer.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, id));
                }

                push(builder, consumer);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "consumerStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#consumerEnd(java.lang.String,
     *                              java.lang.String)
     */
    @Override
    public void consumerEnd(String uri, String type) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer end: type=" + type + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(builder, Consumer.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "consumerEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#serviceStart(java.lang.String,
     *                                  java.lang.String)
     */
    @Override
    public void serviceStart(String uri, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service start: uri=" + uri + " operation=" + operation);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Service service = new Service();
                service.setUri(uri);
                service.setOperation(operation);

                push(builder, service);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "serviceStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#serviceEnd(java.lang.String,
     *                                          java.lang.String)
     */
    @Override
    public void serviceEnd(String uri, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service end: uri=" + uri + " operation=" + operation);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(builder, Service.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "serviceEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#componentStart(java.lang.String,
     *                                    java.lang.String, java.lang.String)
     */
    @Override
    public void componentStart(String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component start: type=" + type + " operation=" + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Component component = new Component();
                component.setComponentType(type);
                component.setUri(uri);
                component.setOperation(operation);

                push(builder, component);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "componentStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#componentEnd(java.lang.String,
     *                                  java.lang.String, java.lang.String)
     */
    @Override
    public void componentEnd(String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component end: type=" + type + " operation=" + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(builder, Component.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "componentEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#producerStart(java.lang.String,
     *                                       java.lang.String, java.lang.String)
     */
    @Override
    public void producerStart(String uri, String type, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer start: type=" + type + " uri=" + uri + " id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Producer producer = new Producer();
                producer.setEndpointType(type);
                producer.setUri(uri);

                if (id != null) {
                    producer.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, id));
                }

                push(builder, producer);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#producerEnd(java.lang.String,
     *                                          java.lang.String)
     */
    @Override
    public void producerEnd(String uri, String type) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer end: type=" + type + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(builder, Producer.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isRequestProcessed()
     */
    @Override
    public boolean isRequestProcessed() {
        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                InteractionNode node=(InteractionNode)builder.getCurrentNode();

                return processorManager.isProcessed(builder.getBusinessTransaction(),
                        node, true);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isRequestContentProcessed()
     */
    @Override
    public boolean isRequestContentProcessed() {
        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                InteractionNode node=(InteractionNode)builder.getCurrentNode();

                return processorManager.isContentProcessed(builder.getBusinessTransaction(),
                        node, true);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestContentProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isResponseProcessed()
     */
    @Override
    public boolean isResponseProcessed() {
        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                InteractionNode node=(InteractionNode)builder.getCurrentNode();

                return processorManager.isProcessed(builder.getBusinessTransaction(),
                        node, false);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isResponseContentProcessed()
     */
    @Override
    public boolean isResponseContentProcessed() {
        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                InteractionNode node=(InteractionNode)builder.getCurrentNode();

                return processorManager.isContentProcessed(builder.getBusinessTransaction(),
                        node, false);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseContentProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#processRequest(java.util.Map, java.lang.Object[])
     */
    @Override
    public void processRequest(Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Process request: headers=" + headers + " values=" + values);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                processValues(builder.getBusinessTransaction(), builder.getCurrentNode(),
                        true, headers, values);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setFault failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#processResponse(java.util.Map, java.lang.Object[])
     */
    @Override
    public void processResponse(Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Process response: headers=" + headers + " values=" + values);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                processValues(builder.getBusinessTransaction(), builder.getCurrentNode(),
                        false, headers, values);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setFault failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#setFault(java.lang.String, java.lang.String)
     */
    @Override
    public void setFault(String value, String description) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set fault: value=" + value + " description=" + description);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.getCurrentNode().setFault(value).setFaultDescription(description);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setFault failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#setProperty(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public void setProperty(String name, String value) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set business transaction property: name=" + name + " value=" + value);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.getBusinessTransaction().getProperties().put(name, value);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setProperty failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#setDetail(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public void setDetail(String name, String value) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set node detail: name=" + name + " value=" + value);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.getCurrentNode().getDetails().put(name, value);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setDetail failed", t);
            }
        }
    }

    /**
     * This method pushes a new node into the business transaction fragment.
     *
     * @param builder The fragment builder
     * @param node The node
     */
    protected void push(FragmentBuilder builder, Node node) {
        node.setBaseTime(System.nanoTime());
        builder.pushNode(node);
    }

    /**
     * This method pops an existing node from the business transaction fragment.
     *
     * @param The class to pop
     * @param The optional URI to match
     * @return The node
     */
    protected <T extends Node> T pop(FragmentBuilder builder, Class<T> cls, String uri) {
        if (builder == null) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("No fragment builder for this thread (" + Thread.currentThread()
                        + ") - trying to pop node of type: " + cls);
            }
            return null;
        }

        if (builder.getCurrentNode() == null) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("No 'current node' for this thread (" + Thread.currentThread()
                        + ") - trying to pop node of type: " + cls);
            }
            return null;
        }

        Node node = builder.popNode(cls, uri);
        if (node != null) {
            node.setDuration(System.nanoTime() - node.getBaseTime());
            return cls.cast(node);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Current node (type=" + builder.getCurrentNode().getClass()
                    + ") does not match required cls=" + cls + "and uri=" + uri);
        }

        return null;
    }

    /**
     * This method processes the values associated with the start or end of a scoped
     * activity.
     *
     * @param btxn The business transaction
     * @param node The node
     * @param req Whether processing a request
     * @param headers The optional headers
     * @param values The values
     */
    protected void processValues(BusinessTransaction btxn, Node node, boolean req,
            Map<String, ?> headers, Object[] values) {

        if (node.interactionNode()) {
            Message m = new Message();

            if (headers != null) {
                // TODO: Need to have config to determine whether headers should be logged
                for (String key : headers.keySet()) {
                    String value = getHeaderValueText(headers.get(key));

                    if (value != null) {
                        m.getHeaders().put(key, value);
                    }
                }
            }

            if (req) {
                ((InteractionNode) node).setRequest(m);
            } else {
                ((InteractionNode) node).setResponse(m);
            }
        }

        if (processorManager != null) {
            processorManager.process(btxn, node, req, headers, values);
        }
    }

    /**
     * This method returns a textual representation of the supplied
     * header value.
     *
     * @param value The original value
     * @return The text representation, or null if no suitable found
     */
    protected String getHeaderValueText(Object value) {
        // TODO: Type conversion based on provided config
        if (value.getClass() == String.class) {
            return (String) value;
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() == 1) {
                return getHeaderValueText(list.get(0));
            } else {
                return list.toString();
            }
        }

        return null;
    }

    /**
     * This method checks whether the supplied fragment has been completed
     * and therefore should be processed.
     *
     * @param builder The fragment builder
     */
    protected void checkForCompletion(FragmentBuilder builder) {
        // Check if completed
        if (builder.isComplete()) {
            BusinessTransaction btxn = builder.getBusinessTransaction();

            if (btxn != null && !btxn.getNodes().isEmpty()) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Record business transaction: " + btxn);
                }

                reporter.report(btxn);
            }

            fragmentManager.clear();

            diagnostics();
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#activate(java.lang.String)
     */
    @Override
    public boolean activate(String uri) {
        return activate(uri, null);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#activate(java.lang.String,java.lang.String)
     */
    @Override
    public boolean activate(String uri, String id) {
        // If already active, then just return
        if (isActive()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("activate: Already active");
            }
            return true;
        }

        // If id is set, then fragment must be tracked
        if (id != null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("activate: ID not null, so fragment will be traced");
            }
            return true;
        }

        if (uri != null) {
            if (filterManager == null) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Unable to determine if fragment should be traced due to missing filter manager");
                }
            } else {
                String btxnName = filterManager.getBusinessTransactionName(uri);

                if (btxnName != null && !btxnName.trim().isEmpty()) {
                    FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                    if (builder != null) {
                        builder.getBusinessTransaction().setName(btxnName);
                    }
                }

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("activate: URI[" + uri + "] business transaction name=" + btxnName);
                }
                return btxnName != null;
            }
        }

        // No URI, so for now we will assume should NOT be traced
        if (log.isLoggable(Level.FINEST)) {
            log.finest("activate: No URI, so returning false");
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#isActive()
     */
    @Override
    public boolean isActive() {
        try {
            return fragmentManager.hasFragmentBuilder();
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isActive failed", t);
            }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#retainNode(java.lang.String)
     */
    @Override
    public void retainNode(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Retain node: id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.retainNode(id);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "retainNode failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#releaseNode(java.lang.String)
     */
    @Override
    public void releaseNode(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Release node: id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.releaseNode(id);

                // Check for completion
                checkForCompletion(builder);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "releaseNode failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#retrieveNode(java.lang.String)
     */
    @Override
    public Node retrieveNode(String id) {
        Node ret = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Retrieve node: id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                ret = builder.retrieveNode(id);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "retrieveNode failed", t);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#initiateLink(java.lang.String)
     */
    @Override
    public void initiateLink(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Initiate link: id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.getUnlinkedIds().add(id);
                links.put(id, builder);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "initiateLink failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#isLinkActive(java.lang.String)
     */
    @Override
    public boolean isLinkActive(String id) {
        boolean linkActive = links.containsKey(id);
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Is link active? id=" + id + " result=" + linkActive);
        }
        return linkActive;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#completeLink(java.lang.String)
     */
    @Override
    public void completeLink(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Complete link: id=" + id);
        }

        try {
            FragmentBuilder builder = links.get(id);

            if (builder != null) {
                builder.getUnlinkedIds().remove(id);
                links.remove(id);
                fragmentManager.setFragmentBuilder(builder);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "completeLink failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#unlink()
     */
    @Override
    public void unlink() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Unlink");
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                fragmentManager.clear();
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "unlink failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#suppress()
     */
    @Override
    public void suppress() {
        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            builder.suppress();
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#assertComplete()
     */
    @Override
    public void assertComplete() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Assert complete");
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                if (!builder.isComplete()) {
                    log.severe("Business transaction has not completed: "
                            + fragmentManager.getFragmentBuilder());
                }
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "assertComplete failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#session()
     */
    @Override
    public SessionManager session() {
        return this;
    }

    /**
     * This method provides access to the fragment manager.
     *
     * @return The fragment manager
     */
    protected FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    /**
     * This method reports diagnostic information to the log.
     */
    protected void diagnostics() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            log.finest("BTM COLLECTOR DIAGNOSTICS:");
            fragmentManager.diagnostics();
            log.finest("Links (" + links.size() + "): " + links);
            log.finest("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        }
    }

}
