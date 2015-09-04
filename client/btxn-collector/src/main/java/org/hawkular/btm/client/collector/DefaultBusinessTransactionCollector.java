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
import org.hawkular.btm.api.model.admin.Direction;
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

    private static boolean testMode;

    static {
        testMode = Boolean.getBoolean("hawkular-btm.test.mode");
    }

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
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#setName(java.lang.String, java.lang.String)
     */
    @Override
    public void setName(String location, String name) {
        if (name == null || name.trim().isEmpty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Ignoring attempt to set business transaction name to null");
            }
            return;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set business transaction location=[" + location + "] name=" + name);
        }

        try {
            // Getting the builder will cause it to be created if does not exist.
            // As this method is setting the name of the business transaction, this
            // should be permitted, as other activity is likely to follow.
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                builder.getBusinessTransaction().setName(name);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setName: No fragment builder for this thread", null);
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
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                ret = builder.getBusinessTransaction().getName();
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "getName: No fragment builder for this thread", null);
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
     *                      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void consumerStart(String location, String uri, String type, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer start: location=[" + location + "] type=" + type + " uri=" + uri + " id=" + id);
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

                push(location, builder, consumer);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "consumerStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#consumerEnd(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public void consumerEnd(String location, String uri, String type) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer end: location=[" + location + "] type=" + type + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(location, builder, Consumer.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "consumerEnd: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "consumerEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#serviceStart(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public void serviceStart(String location, String uri, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service start: location=[" + location + "] uri=" + uri + " operation=" + operation);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Service service = new Service();
                service.setUri(uri);
                service.setOperation(operation);

                push(location, builder, service);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "serviceStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#serviceEnd(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public void serviceEnd(String location, String uri, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service end: location=[" + location + "] uri=" + uri + " operation=" + operation);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(location, builder, Service.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "serviceEnd: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "serviceEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#componentStart(java.lang.String,
     *                      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void componentStart(String location, String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component start: location=[" + location + "] type=" + type + " operation="
                    + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Component component = new Component();
                component.setComponentType(type);
                component.setUri(uri);
                component.setOperation(operation);

                push(location, builder, component);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "componentStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#componentEnd(java.lang.String,
     *                      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void componentEnd(String location, String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component end: location=[" + location + "] type=" + type + " operation="
                    + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(location, builder, Component.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "componentEnd: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "componentEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#producerStart(java.lang.String,
     *                      java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void producerStart(String location, String uri, String type, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer start: location=[" + location + "] type=" + type
                    + " uri=" + uri + " id=" + id);
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

                push(location, builder, producer);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerStart failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#producerEnd(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public void producerEnd(String location, String uri, String type) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer end: location=[" + location + "] type=" + type + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                pop(location, builder, Producer.class, uri);

                // Check for completion
                checkForCompletion(builder);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerEnd: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerEnd failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isRequestProcessed(java.lang.String)
     */
    @Override
    public boolean isRequestProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.headers")
                && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isProcessed(builder.getBusinessTransaction(),
                            node, Direction.Request);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isRequestContentProcessed(java.lang.String)
     */
    @Override
    public boolean isRequestContentProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isContentProcessed(builder.getBusinessTransaction(),
                            node, Direction.Request);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestContentProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestContentProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isResponseProcessed(java.lang.String)
     */
    @Override
    public boolean isResponseProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.headers")
                && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isProcessed(builder.getBusinessTransaction(),
                            node, Direction.Response);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isResponseContentProcessed(java.lang.String)
     */
    @Override
    public boolean isResponseContentProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isContentProcessed(builder.getBusinessTransaction(),
                            node, Direction.Response);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseContentProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseContentProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#processRequest(java.lang.String,
     *                          java.util.Map, java.lang.Object[])
     */
    @Override
    public void processRequest(String location, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Process request: location=[" + location + "] headers=" + headers + " values=" + values);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                processValues(builder.getBusinessTransaction(), builder.getCurrentNode(),
                        Direction.Request, headers, values);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("processRequest: No fragment builder available to process the request data");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "processRequest failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#processResponse(java.lang.String,
     *                                      java.util.Map, java.lang.Object[])
     */
    @Override
    public void processResponse(String location, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Process response: location=[" + location + "] headers=" + headers + " values=" + values);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                processValues(builder.getBusinessTransaction(), builder.getCurrentNode(),
                        Direction.Response, headers, values);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("processResponse: No fragment builder available to process the response data");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "processResponse failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#setFault(java.lang.String,
     *                  java.lang.String, java.lang.String)
     */
    @Override
    public void setFault(String location, String value, String description) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set fault: location=[" + location + "] value=" + value + " description=" + description);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.getCurrentNode().setFault(value).setFaultDescription(description);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setFault: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setFault failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#setProperty(java.lang.String,
     *                                      java.lang.String, java.lang.String)
     */
    @Override
    public void setProperty(String location, String name, String value) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set business transaction property: location=" + location +
                    " name=" + name + " value=" + value);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.getBusinessTransaction().getProperties().put(name, value);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setProperty: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setProperty failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#setDetail(java.lang.String,
     *                                      java.lang.String, java.lang.String)
     */
    @Override
    public void setDetail(String location, String name, String value) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set node detail: location=[" + location + "] name=" + name + " value=" + value);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.getCurrentNode().getDetails().put(name, value);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setDetail: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setDetail failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#initRequestBuffer(java.lang.String,
     *                                       java.lang.Object)
     */
    @Override
    public void initRequestBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("initRequestBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.initRequestBuffer(getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("initRequestBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "initRequestBuffer failed", t);
            }
        }
    }

    /**
     * This method returns a code associated with the supplied object.
     *
     * @param obj The optional object
     * @return The code, or 0 if no object supplied
     */
    protected int getCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isRequestBufferActive(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public boolean isRequestBufferActive(String location, Object obj) {
        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                boolean ret = builder.isRequestBufferActive(getCode(obj));

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("isRequestBufferActive: location=[" + location + "] obj="
                            + obj + "? " + ret);
                }

                return ret;

            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("isRequestBufferActive: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isRequestBufferActive failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#appendRequestBuffer(java.lang.String,
     *                                  java.lang.Object, byte[], int, int)
     */
    @Override
    public void appendRequestBuffer(String location, Object obj, byte[] data, int offset, int len) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("appendRequestBuffer: location=[" + location + "] obj=" + obj + " data=" + data
                    + " offset=" + offset + " len=" + len);
        }

        if (len == -1) {
            return;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.writeRequestData(getCode(obj), data, offset, len);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("appendRequestBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "appendRequestBuffer failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#recordRequestBuffer(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public void recordRequestBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("recordRequestBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                processRequestContent(location, fragmentManager.getFragmentBuilder(), getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("recordRequestBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "recordRequestBuffer failed", t);
            }
        }
    }

    /**
     * This method processes the request content if available.
     *
     * @param location The instrumentation location
     * @param builder The builder
     * @param hashCode The hash code, or -1 to ignore the hash code
     */
    protected void processRequestContent(String location, FragmentBuilder builder, int hashCode) {
        if (builder.isRequestBufferActive(hashCode)) {
            processRequest(location, null, builder.getRequestData(hashCode));
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#initResponseBuffer(java.lang.String,
     *                              java.lang.Object)
     */
    @Override
    public void initResponseBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("initResponseBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.initResponseBuffer(getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("initResponseBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "initResponseBuffer failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#isResponseBufferActive(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public boolean isResponseBufferActive(String location, Object obj) {
        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                boolean ret = builder.isResponseBufferActive(getCode(obj));

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("isResponseBufferActive: location=[" + location + "] obj="
                            + obj + "? " + ret);
                }

                return ret;

            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("isResponseBufferActive: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isResponseBufferActive failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#appendResponseBuffer(java.lang.String,
     *                          java.lang.Object, byte[], int, int)
     */
    @Override
    public void appendResponseBuffer(String location, Object obj, byte[] data, int offset, int len) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("appendResponseBuffer: location=[" + location + "] obj=" + obj + " data=" + data
                    + " offset=" + offset + " len=" + len);
        }

        if (len == -1) {
            return;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.writeResponseData(getCode(obj), data, offset, len);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("appendResponseBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "appendResponseBuffer failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.BusinessTransactionCollector#recordResponseBuffer(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public void recordResponseBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("recordResponseBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                processResponseContent(location, fragmentManager.getFragmentBuilder(), getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("recordResponseBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "recordResponseBuffer failed", t);
            }
        }
    }

    /**
     * This method processes the response content if available.
     *
     * @param location The instrumentation location
     * @param builder The builder
     * @param hashCode The hash code, or -1 to ignore the hash code
     */
    protected void processResponseContent(String location, FragmentBuilder builder, int hashCode) {
        if (builder.isResponseBufferActive(hashCode)) {
            processResponse(location, null, builder.getResponseData(hashCode));
        }
    }

    /**
     * This method pushes a new node into the business transaction fragment.
     *
     * @param location The instrumentation location
     * @param builder The fragment builder
     * @param node The node
     */
    protected void push(String location, FragmentBuilder builder, Node node) {

        // Check if any request content should be processed for the current node
        processRequestContent(location, builder, -1);

        node.setBaseTime(System.nanoTime());
        builder.pushNode(node);
    }

    /**
     * This method pops an existing node from the business transaction fragment.
     *
     * @param location The instrumentation location
     * @param The class to pop
     * @param The optional URI to match
     * @return The node
     */
    protected <T extends Node> T pop(String location, FragmentBuilder builder, Class<T> cls, String uri) {
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

        // Check if any request or response content should be processed for the current node
        processRequestContent(location, builder, -1);
        processResponseContent(location, builder, -1);

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
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     */
    protected void processValues(BusinessTransaction btxn, Node node, Direction direction,
            Map<String, ?> headers, Object[] values) {

        if (node.interactionNode()) {
            Message m = null;

            if (direction == Direction.Request) {
                m = ((InteractionNode) node).getRequest();
                if (m == null) {
                    m = new Message();
                    ((InteractionNode) node).setRequest(m);
                }
            } else {
                m = ((InteractionNode) node).getResponse();
                if (m == null) {
                    m = new Message();
                    ((InteractionNode) node).setResponse(m);
                }
            }

            if (headers != null && m.getHeaders().isEmpty()) {
                // TODO: Need to have config to determine whether headers should be logged
                for (String key : headers.keySet()) {
                    String value = getHeaderValueText(headers.get(key));

                    if (value != null) {
                        m.getHeaders().put(key, value);
                    }
                }
            }
        }

        if (processorManager != null) {
            processorManager.process(btxn, node, direction, headers, values);
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
        if (value == null) {
            return null;
        }

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
