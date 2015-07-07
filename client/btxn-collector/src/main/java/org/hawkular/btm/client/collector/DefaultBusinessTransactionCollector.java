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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.hawkular.btm.api.client.BusinessTransactionCollector;
import org.hawkular.btm.api.client.ConfigurationManager;
import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;
import org.hawkular.btm.api.client.SessionManager;
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
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.api.util.ServiceResolver;
import org.hawkular.btm.client.collector.internal.FilterManager;
import org.hawkular.btm.client.collector.internal.FragmentBuilder;
import org.hawkular.btm.client.collector.internal.FragmentManager;

/**
 * @author gbrown
 */
public class DefaultBusinessTransactionCollector implements BusinessTransactionCollector, SessionManager {

    private static final Logger log = Logger.getLogger(DefaultBusinessTransactionCollector.class.getName());

    private FragmentManager fragmentManager = new FragmentManager();

    private String tenantId = System.getProperty("hawkular-btm.tenantId");

    private BusinessTransactionService businessTransactionService;

    private CollectorConfiguration config;

    private FilterManager filterManager;

    private Map<String,FragmentBuilder> links=new ConcurrentHashMap<String,FragmentBuilder>();

    private static final Level warningLogLevel=Level.WARNING;

    {
        CompletableFuture<BusinessTransactionService> bts =
                ServiceResolver.getSingletonService(BusinessTransactionService.class);

        bts.whenCompleteAsync(new BiConsumer<BusinessTransactionService, Throwable>() {
            @Override
            public void accept(BusinessTransactionService arg0, Throwable arg1) {
                if (businessTransactionService == null) {
                    setBusinessTransactionService(arg0);

                    if (arg1 != null) {
                        log.severe("Failed to locate Business Transaction Service: " + arg1);
                    } else {
                        log.info("Initialised Business Transaction Service: " + arg0 + " in this=" + this);
                    }
                }
            }
        });

        // Obtain the configuration
        CompletableFuture<ConfigurationManager> cmFuture =
                ServiceResolver.getSingletonService(ConfigurationManager.class);

        cmFuture.whenComplete(new BiConsumer<ConfigurationManager, Throwable>() {

            @Override
            public void accept(ConfigurationManager cm, Throwable t) {
                config = cm.getConfiguration();

                if (config != null) {
                    filterManager = new FilterManager(config);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#consumerStart(java.lang.String,
     *                  java.lang.String, java.lang.String, java.util.Map, java.lang.Object[])
     */
    @Override
    public void consumerStart(String uri, String type, String id, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer start: type=" + type + " uri=" + uri + " id=" + id
                    + " headers=" + headers + " values=" + values);
        }

        try {
            Consumer consumer = new Consumer();
            consumer.setEndpointType(type);
            consumer.setUri(uri);

            processValues(consumer, true, id, headers, values);

            push(consumer);
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "consumerStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#consumerEnd(
     *              java.lang.String, java.lang.String, java.util.Map, java.lang.Object[])
     */
    @Override
    public void consumerEnd(String uri, String type, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer end: type=" + type + " uri=" + uri
                    + " headers=" + headers
                    + " values=" + values);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Consumer consumer = pop(builder, Consumer.class);

                processValues(consumer, false, null, headers, values);

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
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#serviceStart(java.lang.String,
     *                  java.lang.String, java.util.Map, java.lang.Object[])
     */
    @Override
    public void serviceStart(String uri, String operation, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service start: uri=" + uri + " operation=" + operation + " values=" + values);
        }

        try {
            Service service = new Service();
            service.setUri(uri);
            service.setOperation(operation);

            processValues(service, true, null, headers, values);

            push(service);
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "serviceStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#serviceEnd(java.lang.String,
     *                  java.lang.String, java.util.Map, java.lang.Object[])
     */
    @Override
    public void serviceEnd(String uri, String operation, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service end: uri=" + uri + " operation=" + operation + " values=" + values);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Service service = pop(builder, Service.class);

                processValues(service, false, null, headers, values);

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
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#componentStart(
     *                      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void componentStart(String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component start: type=" + type + " operation=" + operation + " uri=" + uri);
        }

        try {
            Component component = new Component();
            component.setComponentType(type);
            component.setUri(uri);
            component.setOperation(operation);

            push(component);
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "componentStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#componentEnd(
     *                      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void componentEnd(String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component end: type=" + type + " operation=" + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(builder, Component.class);

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
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#producerStart(java.lang.String,
     *                     java.lang.String, java.lang.String, java.util.Map, java.lang.Object[])
     */
    @Override
    public void producerStart(String uri, String type, String id, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer start: type=" + type + " uri=" + uri + " id=" + id
                    + " headers=" + headers + " values=" + values);
        }

        try {
            Producer producer = new Producer();
            producer.setEndpointType(type);
            producer.setUri(uri);

            processValues(producer, true, id, headers, values);

            push(producer);
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#producerEnd(java.lang.String,
     *                      java.lang.String, java.util.Map, java.lang.Object[])
     */
    @Override
    public void producerEnd(String uri, String type, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer end: type=" + type + " uri=" + uri
                    + " headers=" + headers
                    + " values=" + values);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Producer producer = pop(builder, Producer.class);

                processValues(producer, false, null, headers, values);

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
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#setProperty(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public void setProperty(String name, String value) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Add property: name=" + name + " value=" + value);
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
            log.finest("Add property: name=" + name + " value=" + value);
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
     * @return the businessTransactionService
     */
    public BusinessTransactionService getBusinessTransactionService() {
        return businessTransactionService;
    }

    /**
     * @param businessTransactionService the businessTransactionService to set
     */
    public void setBusinessTransactionService(BusinessTransactionService businessTransactionService) {
        this.businessTransactionService = businessTransactionService;
    }

    /**
     * @return the tenantId
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId the tenantId to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * This method pushes a new node into the business transaction fragment.
     *
     * @param node The node
     */
    protected void push(Node node) {
        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            node.setStartTime(System.currentTimeMillis());
            builder.pushNode(node);
        }
    }

    /**
     * This method pops an existing node from the business transaction fragment.
     *
     * @return The node
     */
    protected <T extends Node> T pop(FragmentBuilder builder, Class<T> cls) {
        if (builder == null) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("No fragment builder for this thread ("+Thread.currentThread()
                        +") - trying to pop node of type: "+cls);
            }
            return null;
        }

        if (builder.isSuppressed()) {
            builder.popNode();
            return null;
        }

        if (builder.getCurrentNode() == null) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("No 'current node' for this thread ("+Thread.currentThread()
                        +") - trying to pop node of type: "+cls);
            }
            return null;
        }

        // Check node is of appropriate type
        if (builder.getCurrentNode().getClass() == cls) {
            Node node = builder.popNode();
            node.setDuration(System.currentTimeMillis() - node.getStartTime());
            return cls.cast(node);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Current node (type=" + builder.getCurrentNode().getClass()
                    + ") does not match required cls=" + cls);
        }

        return null;
    }

    /**
     * This method processes the values associated with the start or end of a scoped
     * activity.
     *
     * @param node The node
     * @param req Whether processing a request
     * @param id The unique interaction id
     * @param headers The optional headers
     * @param values The values
     */
    protected void processValues(InteractionNode node, boolean req, String id,
            Map<String, ?> headers, Object[] values) {
        Message m = new Message();
        m.setId(id);

        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    // TODO: Type conversion based on provided config
                    m.getParameters().add(values[i].toString());
                }
            }
        }

        // Only use the request based interaction id for correlation
        if (id != null && req) {
            node.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, id));
        }

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
            node.setRequest(m);
        } else {
            node.setResponse(m);
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
            return (String)value;
        } else if (value instanceof List) {
            List<?> list=(List<?>)value;
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

                if (businessTransactionService != null) {
                    Executors.newSingleThreadExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
                            btxns.add(btxn);
                            try {
                                businessTransactionService.store(tenantId, btxns);
                            } catch (Exception e) {
                                log.log(Level.SEVERE, "Failed to store business transactions", e);
                            }
                        }
                    });
                } else {
                    log.warning("Business transaction service is not available!");
                }
            }

            fragmentManager.clear();
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
                boolean valid=filterManager.isValid(uri);
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("activate: URI["+uri+"] isValid="+valid);
                }
                return valid;
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
        Node ret=null;

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
        boolean linkActive=links.containsKey(id);
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
                FragmentBuilder builder=fragmentManager.getFragmentBuilder();

                if (!builder.isComplete()) {
                    log.severe("Business transaction has not completed: "
                            +fragmentManager.getFragmentBuilder());
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
}
