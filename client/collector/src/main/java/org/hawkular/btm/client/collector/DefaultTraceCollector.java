/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.ReportingLevel;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.trace.Component;
import org.hawkular.btm.api.model.trace.Consumer;
import org.hawkular.btm.api.model.trace.ContainerNode;
import org.hawkular.btm.api.model.trace.CorrelationIdentifier;
import org.hawkular.btm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.trace.InteractionNode;
import org.hawkular.btm.api.model.trace.Message;
import org.hawkular.btm.api.model.trace.Node;
import org.hawkular.btm.api.model.trace.NodeType;
import org.hawkular.btm.api.model.trace.Producer;
import org.hawkular.btm.api.model.trace.Trace;
import org.hawkular.btm.api.services.ConfigurationService;
import org.hawkular.btm.api.services.ServiceResolver;
import org.hawkular.btm.api.services.TracePublisher;
import org.hawkular.btm.api.utils.EndpointUtil;
import org.hawkular.btm.api.utils.PropertyUtil;
import org.hawkular.btm.client.api.SessionManager;
import org.hawkular.btm.client.api.TraceCollector;
import org.hawkular.btm.client.collector.internal.FilterManager;
import org.hawkular.btm.client.collector.internal.FilterProcessor;
import org.hawkular.btm.client.collector.internal.FragmentBuilder;
import org.hawkular.btm.client.collector.internal.FragmentManager;
import org.hawkular.btm.client.collector.internal.ProcessorManager;
import org.hawkular.btm.client.collector.internal.TraceReporter;

/**
 * @author gbrown
 */
public class DefaultTraceCollector implements TraceCollector, SessionManager {

    private static final Logger log = Logger.getLogger(DefaultTraceCollector.class.getName());

    private FragmentManager fragmentManager = new FragmentManager();

    private FilterManager filterManager;

    private ProcessorManager processorManager;

    private TraceReporter reporter = new TraceReporter();

    private Map<String, FragmentBuilder> correlations = new ConcurrentHashMap<String, FragmentBuilder>();

    private static final Level warningLogLevel = Level.WARNING;

    private long configLastUpdated = 0;

    private static boolean testMode;

    static {
        testMode = Boolean.getBoolean("hawkular-btm.test.mode");
    }

    {
        // Obtain the configuration service
        setConfigurationService(ServiceResolver.getSingletonService(ConfigurationService.class));
    }

    /**
     * This method sets the configuration service.
     *
     * @param cs The configuration service
     */
    public void setConfigurationService(ConfigurationService cs) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Set configuration service = " + cs);
        }

        CollectorConfiguration config = null;

        if (cs != null) {
            config = cs.getCollector(null, null, null, null);
        }

        if (config != null) {
            configLastUpdated = System.currentTimeMillis();

            filterManager = new FilterManager(config);
            reporter.init(config);
            try {
                processorManager = new ProcessorManager(config);
            } catch (Throwable t) {
                if (t != null) {
                    log.log(Level.SEVERE, "Failed to initialise Process Manager", t);
                }
            }

            // Check if should check for updates
            Integer refresh = PropertyUtil.getPropertyAsInteger(PropertyUtil.HAWKULAR_APM_CONFIG_REFRESH);

            if (log.isLoggable(Level.FINER)) {
                log.finer("Configuration refresh cycle (in seconds) = " + refresh);
            }

            if (refresh != null) {
                Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Map<String, BusinessTxnConfig> changed = cs.getBusinessTransactions(null,
                                    configLastUpdated);

                            for (String btxn : changed.keySet()) {
                                BusinessTxnConfig btc = changed.get(btxn);

                                if (btc.isDeleted()) {
                                    if (log.isLoggable(Level.FINER)) {
                                        log.finer("Removing config for btxn '" + btxn + "' = " + btc);
                                    }

                                    filterManager.remove(btxn);
                                    processorManager.remove(btxn);
                                } else {
                                    if (log.isLoggable(Level.FINER)) {
                                        log.finer("Changed config for btxn '" + btxn + "' = " + btc);
                                    }

                                    filterManager.init(btxn, btc);
                                    processorManager.init(btxn, btc);
                                }

                                if (btc.getLastUpdated() > configLastUpdated) {
                                    configLastUpdated = btc.getLastUpdated();
                                }
                            }
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "Failed to update business transaction configuration", e);
                        }
                    }
                }, refresh.intValue(), refresh.intValue(), TimeUnit.SECONDS);
            }
        }
    }

    /**
     * @return the trace publisher
     */
    protected TracePublisher getTracePublisher() {
        return reporter.getTracePublisher();
    }

    /**
     * @param tracePublisher the trace publisher to set
     */
    protected void setBusinessTransactionPublisher(TracePublisher tracePublisher) {
        reporter.setTracePublisher(tracePublisher);
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
     * @see org.hawkular.btm.client.api.TraceCollector#setName(java.lang.String, java.lang.String)
     */
    @Override
    public void setBusinessTransaction(String location, String name) {
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
                builder.getTrace().setBusinessTransaction(name);
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
     * @see org.hawkular.btm.api.client.TraceCollector#getBusinessTransaction()
     */
    @Override
    public String getBusinessTransaction() {
        String ret = null;

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                ret = builder.getTrace().getBusinessTransaction();
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
     * @see org.hawkular.btm.client.api.TraceCollector#setPrincipal(java.lang.String, java.lang.String)
     */
    @Override
    public void setPrincipal(String location, String principal) {
        if (principal == null || principal.trim().isEmpty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Ignoring attempt to set principal to null");
            }
            return;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set principal location=[" + location + "] principal=" + principal);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.getTrace().setPrincipal(principal);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setPrincipal: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setPrincipal failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#getPrincipal()
     */
    @Override
    public String getPrincipal() {
        String ret = null;

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                ret = builder.getTrace().getPrincipal();
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "getPrincipal: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "getPrincipal failed", t);
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get principal=" + ret);
        }

        if (ret == null) {
            ret = "";
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#setLevel(java.lang.String, java.lang.String)
     */
    @Override
    public void setLevel(String location, String level) {
        if (level == null || level.trim().isEmpty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Ignoring attempt to set level to null");
            }
            return;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set reporting level: location=[" + location + "] level=" + level);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                if (builder != null) {
                    builder.setLevel(ReportingLevel.valueOf(level));
                } else if (log.isLoggable(warningLogLevel)) {
                    log.log(warningLogLevel, "setLevel: No fragment builder for this thread", null);
                }
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "setLevel failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#getLevel()
     */
    @Override
    public String getLevel() {
        String ret = null;

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                ret = builder.getLevel().name();
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "getLevel: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "getLevel failed", t);
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get level=" + ret);
        }

        if (ret == null) {
            ret = "";
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#consumerStart(java.lang.String,
     *                      java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void consumerStart(String location, String uri, String type, String operation, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer start: location=[" + location + "] type=" + type + " operation="
                    + operation + " uri=" + uri + " id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Consumer consumer = new Consumer();
                consumer.setEndpointType(type);
                consumer.setUri(uri);
                consumer.setOperation(operation);

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
     * @see org.hawkular.btm.client.api.TraceCollector#consumerEnd(java.lang.String,
     *                          java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void consumerEnd(String location, String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer end: location=[" + location + "] type=" + type + " operation="
                    + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Node node = pop(location, builder, Consumer.class, uri);

                // Check for completion
                checkForCompletion(builder, node);
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
     * @see org.hawkular.btm.client.api.TraceCollector#componentStart(java.lang.String,
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
     * @see org.hawkular.btm.client.api.TraceCollector#componentEnd(java.lang.String,
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
                Node node = pop(location, builder, Component.class, uri);

                // Check for completion
                checkForCompletion(builder, node);
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
     * @see org.hawkular.btm.client.api.TraceCollector#producerStart(java.lang.String,
     *                      java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void producerStart(String location, String uri, String type, String operation, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer start: location=[" + location + "] type=" + type + " operation="
                    + operation + " uri=" + uri + " id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Producer producer = new Producer();
                producer.setEndpointType(type);
                producer.setUri(uri);
                producer.setOperation(operation);

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
     * @see org.hawkular.btm.client.api.TraceCollector#producerEnd(java.lang.String,
     *                          java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void producerEnd(String location, String uri, String type, String operation) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer end: location=[" + location + "] type=" + type + " operation="
                    + operation + " uri=" + uri);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Producer node = pop(location, builder, Producer.class, uri);

                // Check if current node on stack is also a Producer
                Node current = builder.getCurrentNode();
                if (current != null && current.getType() == NodeType.Producer) {
                    // Merge details into current node
                    mergeProducer(node, (Producer) current);
                }

                // Check for completion
                checkForCompletion(builder, node);
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerEnd: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "producerEnd failed", t);
            }
        }
    }

    /**
     * This method merges an inner Producer node information into its
     * containing Producer node, before removing the inner node.
     *
     * @param inner
     * @param outer
     */
    protected void mergeProducer(Producer inner, Producer outer) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Merging Producer = " + inner + " into Producer = " + outer);
        }

        // NOTE: For now, assumption is that inner Producer is equivalent to the outer
        // and results from instrumentation rules being triggered multiple times
        // for the same message.

        // Merge correlation - just replace for now
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Merging Producers: replacing correlation ids (" + outer.getCorrelationIds()
                    + ") with (" + inner.getCorrelationIds() + ")");
        }
        outer.setCorrelationIds(inner.getCorrelationIds());

        // Remove the inner Producer from the child nodes of the outer
        outer.getNodes().remove(inner);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#isInProcessed(java.lang.String)
     */
    @Override
    public boolean isInProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.headers")
                && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isProcessed(builder.getTrace(),
                            node, Direction.In);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isInProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isInProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#isInContentProcessed(java.lang.String)
     */
    @Override
    public boolean isInContentProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isContentProcessed(builder.getTrace(),
                            node, Direction.In);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isInContentProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isInContentProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#isOutProcessed(java.lang.String)
     */
    @Override
    public boolean isOutProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.headers")
                && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isProcessed(builder.getTrace(),
                            node, Direction.Out);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isOutProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isOutProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#isOutContentProcessed(java.lang.String)
     */
    @Override
    public boolean isOutContentProcessed(String location) {

        if (testMode && !Boolean.getBoolean("hawkular-btm.test.process.content")) {
            return false;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();

                if (node != null && node.interactionNode()) {
                    return processorManager.isContentProcessed(builder.getTrace(),
                            node, Direction.Out);
                }
            } else if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isOutContentProcessed: No fragment builder for this thread", null);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isOutContentProcessed failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#processIn(java.lang.String,
     *                          java.util.Map, java.lang.Object[])
     */
    @Override
    public void processIn(String location, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Process in: location=[" + location + "] headers=" + headers + " values=" + values);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                processValues(builder.getTrace(), builder.getCurrentNode(),
                        Direction.In, headers, values);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("processIn: No fragment builder available to process the in data");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "processIn failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#processOut(java.lang.String,
     *                                      java.util.Map, java.lang.Object[])
     */
    @Override
    public void processOut(String location, Map<String, ?> headers, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Process out: location=[" + location + "] headers=" + headers + " values=" + values);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = builder.getCurrentNode();
                if (node != null) {
                    processValues(builder.getTrace(), node,
                        Direction.Out, headers, values);
                }
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("processOut: No fragment builder available to process the out data");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "processOut failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#setFault(java.lang.String,
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
     * @see org.hawkular.btm.client.api.TraceCollector#setProperty(java.lang.String,
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

                if (value == null) {
                    builder.getTrace().getProperties().remove(name);
                } else {
                    builder.getTrace().getProperties().put(name, value);
                }
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
     * @see org.hawkular.btm.client.api.TraceCollector#setDetail(java.lang.String,
     *                                      java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void setDetail(String location, String name, String value, String nodeType, boolean onStack) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set node detail: location=[" + location + "] name=" + name + " value=" + value
                    + " nodeType=" + nodeType + " onStack=" + onStack);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                Node node = null;

                if (nodeType == null) {
                    node = builder.getCurrentNode();
                } else {
                    node = builder.getLatestNode(nodeType, onStack);
                }

                if (node != null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Set node details: using node=" + node);
                    }
                    if (value == null) {
                        node.getDetails().remove(name);
                    } else {
                        node.getDetails().put(name, value);
                    }
                } else if (log.isLoggable(Level.FINEST)) {
                    log.finest("Set node details: failed to find node to set");
                }

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
     * @see org.hawkular.btm.client.api.TraceCollector#initInBuffer(java.lang.String,
     *                                       java.lang.Object)
     */
    @Override
    public void initInBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("initInBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.initInBuffer(getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("initInBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "initInBuffer failed", t);
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
     * @see org.hawkular.btm.client.api.TraceCollector#isInBufferActive(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public boolean isInBufferActive(String location, Object obj) {
        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                boolean ret = builder.isInBufferActive(getCode(obj));

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("isInBufferActive: location=[" + location + "] obj="
                            + obj + "? " + ret);
                }

                return ret;

            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("isInBufferActive: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isInBufferActive failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#appendInBuffer(java.lang.String,
     *                                  java.lang.Object, byte[], int, int)
     */
    @Override
    public void appendInBuffer(String location, Object obj, byte[] data, int offset, int len) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("appendInBuffer: location=[" + location + "] obj=" + obj + " data=" + data
                    + " offset=" + offset + " len=" + len);
        }

        if (len == -1) {
            return;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.writeInData(getCode(obj), data, offset, len);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("appendInBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "appendInBuffer failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#recordInBuffer(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public void recordInBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("recordInBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                processInContent(location, fragmentManager.getFragmentBuilder(), getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("recordInBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "recordInBuffer failed", t);
            }
        }
    }

    /**
     * This method processes the in content if available.
     *
     * @param location The instrumentation location
     * @param builder The builder
     * @param hashCode The hash code, or -1 to ignore the hash code
     */
    protected void processInContent(String location, FragmentBuilder builder, int hashCode) {
        if (builder.isInBufferActive(hashCode)) {
            processIn(location, null, builder.getInData(hashCode));
        } else if (log.isLoggable(Level.FINEST)) {
            log.finest("processInContent: location=[" + location + "] hashCode=" + hashCode
                    + " in buffer is not active");
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#initOutBuffer(java.lang.String,
     *                              java.lang.Object)
     */
    @Override
    public void initOutBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("initOutBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.initOutBuffer(getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("initOutBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "initOutBuffer failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#isOutBufferActive(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public boolean isOutBufferActive(String location, Object obj) {
        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                boolean ret = builder.isOutBufferActive(getCode(obj));

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("isOutBufferActive: location=[" + location + "] obj="
                            + obj + "? " + ret);
                }

                return ret;

            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("isOutBufferActive: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "isOutBufferActive failed", t);
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#appendOutBuffer(java.lang.String,
     *                          java.lang.Object, byte[], int, int)
     */
    @Override
    public void appendOutBuffer(String location, Object obj, byte[] data, int offset, int len) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("appendOutBuffer: location=[" + location + "] obj=" + obj + " data=" + data
                    + " offset=" + offset + " len=" + len);
        }

        if (len == -1) {
            return;
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                builder.writeOutData(getCode(obj), data, offset, len);
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("appendOutBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "appendOutBuffer failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.TraceCollector#recordOutBuffer(java.lang.String,
     *                                      java.lang.Object)
     */
    @Override
    public void recordOutBuffer(String location, Object obj) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("recordOutBuffer: location=[" + location + "] obj=" + obj);
        }

        try {
            if (fragmentManager.hasFragmentBuilder()) {
                processOutContent(location, fragmentManager.getFragmentBuilder(), getCode(obj));
            } else if (log.isLoggable(Level.FINEST)) {
                log.finest("recordOutBuffer: No fragment builder for this thread");
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "recordOutBuffer failed", t);
            }
        }
    }

    /**
     * This method processes the out content if available.
     *
     * @param location The instrumentation location
     * @param builder The builder
     * @param hashCode The hash code, or -1 to ignore the hash code
     */
    protected void processOutContent(String location, FragmentBuilder builder, int hashCode) {
        if (builder.isOutBufferActive(hashCode)) {
            processOut(location, null, builder.getOutData(hashCode));
        } else if (log.isLoggable(Level.FINEST)) {
            log.finest("processOutContent: location=[" + location + "] hashCode=" + hashCode
                    + " out buffer is not active");
        }
    }

    /**
     * This method initialises the supplied node.
     *
     * @param node The node
     */
    protected void initNode(Node node) {
        node.setBaseTime(System.nanoTime());
    }

    /**
     * This method pushes a new node into the trace fragment.
     *
     * @param location The instrumentation location
     * @param builder The fragment builder
     * @param node The node
     */
    protected void push(String location, FragmentBuilder builder, Node node) {

        // Check if any in content should be processed for the current node
        processInContent(location, builder, -1);

        initNode(node);

        builder.pushNode(node);
    }

    /**
     * This method pops an existing node from the trace fragment.
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
            if (log.isLoggable(Level.FINEST)) {
                log.finest("WARNING: No 'current node' for this thread (" + Thread.currentThread()
                        + ") - trying to pop node of type: " + cls);
            }
            return null;
        }

        // Check if any in or out content should be processed for the current node
        processInContent(location, builder, -1);
        processOutContent(location, builder, -1);

        Node node = builder.popNode(cls, uri);
        if (node != null) {
            node.setDuration(System.nanoTime() - node.getBaseTime());
            return cls.cast(node);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Current node (type=" + builder.getCurrentNode().getClass()
                    + ") does not match required cls=" + cls + " and uri=" + uri
                    + " at location=" + location);
        }

        return null;
    }

    /**
     * This method processes the values associated with the start or end of a scoped
     * activity.
     *
     * @param trace The trace
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     */
    protected void processValues(Trace trace, Node node, Direction direction,
            Map<String, ?> headers, Object[] values) {

        if (node.interactionNode()) {
            Message m = null;

            if (direction == Direction.In) {
                m = ((InteractionNode) node).getIn();
                if (m == null) {
                    m = new Message();
                    ((InteractionNode) node).setIn(m);
                }
            } else {
                m = ((InteractionNode) node).getOut();
                if (m == null) {
                    m = new Message();
                    ((InteractionNode) node).setOut(m);
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
            processorManager.process(trace, node, direction, headers, values);
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
     * @param node The most recently popped node
     * @param builder The fragment builder
     */
    protected void checkForCompletion(FragmentBuilder builder, Node node) {
        // Check if completed
        if (builder.isComplete()) {
            if (node != null) {
                Trace trace = builder.getTrace();

                if (builder.getLevel().ordinal() <= ReportingLevel.None.ordinal()) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Not recording trace (level="
                                + builder.getLevel() + "): " + trace);
                    }
                } else {
                    if (trace != null && !trace.getNodes().isEmpty()) {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("Record trace: " + trace);
                        }

                        // Check if first top level node is an internal consumer
                        // and if so move subsequent top level nodes within its scope
                        if (trace.getNodes().size() > 1
                                && trace.getNodes().get(0).getClass() == Consumer.class
                                && ((Consumer)trace.getNodes().get(0)).getEndpointType() == null) {
                            Consumer consumer=(Consumer)trace.getNodes().get(0);
                            for (int i=1; trace.getNodes().size() > 1; ) {
                                consumer.getNodes().add(trace.getNodes().get(1));
                                trace.getNodes().remove(1);
                            }
                        }

                        reporter.report(trace);
                    }
                }
            }

            fragmentManager.clear();

            // Remove uncompleted correlation ids
            for (String id : builder.getUncompletedCorrelationIds()) {
                correlations.remove(id);
            }

            diagnostics();
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#activate(java.lang.String,java.lang.String)
     */
    @Override
    public boolean activate(String uri, String  operation) {
        return activate(uri, operation, null);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#activate(java.lang.String,java.lang.String,java.lang.String)
     */
    @Override
    public boolean activate(String uri, String operation, String id) {
        if (!reporter.isEnabled()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Reporter is disabled, so cannot activate");
            }
            return false;
        }

        // If id is set, then fragment must be tracked
        if (id != null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("activate: ID not null, so fragment will be traced");
            }
            return true;
        }

        // Check if already active
        boolean active = isActive();
        FragmentBuilder builder = null;

        if (active) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("activate: Already active");
            }

            // Check whether business transaction name should be applied
            builder = fragmentManager.getFragmentBuilder();

            // If business txn name already defined, or top level node has correlation ids,
            // then just return already active
            Trace trace = builder.getTrace();

            if (trace.getBusinessTransaction() != null
                    || (trace.getNodes().size() > 0
                    && !trace.getNodes().get(0).getCorrelationIds().isEmpty())) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("activate: Already active, with btxn name or top level node having correlation ids");
                }
                return true;
            }
        }

        if (uri != null) {
            String endpoint = EndpointUtil.encodeEndpoint(uri, operation);

            if (filterManager == null) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Unable to determine if fragment should be traced due to missing filter manager");
                }
            } else {
                FilterProcessor filterProcessor = filterManager.getFilterProcessor(endpoint);

                if (filterProcessor != null && filterProcessor.getBusinessTransaction() != null) {
                    if (builder == null) {
                        builder = fragmentManager.getFragmentBuilder();
                    }

                    if (builder != null) {
                        builder.getTrace().setBusinessTransaction(filterProcessor.getBusinessTransaction());
                        builder.setLevel(filterProcessor.getConfig().getLevel());
                    }
                }

                if (log.isLoggable(Level.FINEST)) {
                    if (filterProcessor != null) {
                        log.finest("activate: Endpoint[" + endpoint + "] business transaction name="
                                + filterProcessor.getBusinessTransaction() + " config="
                                + filterProcessor.getConfig());
                    } else {
                        log.finest("activate: Endpoint[" + endpoint + "] no business transaction found");
                    }
                }
                return filterProcessor != null;
            }
        }

        // No URI, so for now we will assume should NOT be traced (if not already active)
        if (log.isLoggable(Level.FINEST)) {
            log.finest("activate: No URI, so returning existing active state=" + active);
        }
        return active;
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
     * @see org.hawkular.btm.api.client.TraceCollector#retainNode(java.lang.String)
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
     * @see org.hawkular.btm.api.client.TraceCollector#releaseNode(java.lang.String)
     */
    @Override
    public void releaseNode(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Release node: id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Node node = builder.releaseNode(id);

                // Check for completion
                checkForCompletion(builder, node);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "releaseNode failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.TraceCollector#retrieveNode(java.lang.String)
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
     * @see org.hawkular.btm.api.client.SessionManager#initiateCorrelation(java.lang.String)
     */
    @Override
    public void initiateCorrelation(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Initiate correlation: id=" + id);
        }

        try {
            FragmentBuilder builder = fragmentManager.getFragmentBuilder();

            if (builder != null) {
                Node currentNode = builder.getCurrentNode();
                int pos = -1;

                if (currentNode != null && currentNode.containerNode()) {
                    pos = ((ContainerNode)currentNode).getNodes().size();
                }

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Setup uncompleted correlation between id=" + id
                            + " and current node=" + currentNode + " pos=" + pos);
                }

                // Record the correlation id against the node in which it was initiated
                // TODO: HWKBTM-402 May also need to record current position in node's child list,
                // so that internal Producer link is created in correct place??
                // Would only be an issue if other subsequent (non-internal-link) activities
                // occurred under the same node and therefore may be out of order if a
                // 'spawn' internal link was later to be created
                builder.addUncompletedCorrelationId(id, currentNode, pos);

                correlations.put(id, builder);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "initiateCorrelation failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#isCorrelated(java.lang.String)
     */
    @Override
    public boolean isCorrelated(String id) {
        boolean correlationActive = correlations.containsKey(id);
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Is correlated? id=" + id + " result=" + correlationActive);
        }
        return correlationActive;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#correlate(java.lang.String)
     */
    @Override
    public void correlate(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Correlate: id=" + id);
        }

        try {
            FragmentBuilder builder = correlations.get(id);

            if (builder != null) {
                fragmentManager.setFragmentBuilder(builder);
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "correlate failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.SessionManager#completeCorrelation(java.lang.String, boolean)
     */
    @Override
    public void completeCorrelation(String id, boolean allowSpawn) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Complete correlation: id=" + id + " allowSpawn=" + allowSpawn);
        }

        try {
            FragmentBuilder builder = correlations.get(id);

            if (builder != null) {
                int pos = builder.getUncompletedCorrelationIdPosition(id);
                Node node = builder.removeUncompletedCorrelationId(id);
                correlations.remove(id);

                // Check if thread is already associated with correlated fragment builder
                if (fragmentManager.hasFragmentBuilder()) {
                    FragmentBuilder existing=fragmentManager.getFragmentBuilder();

                    // If the same, then nothing needs to be done
                    if (existing == builder) {
                        return;
                    }
                }

                // If spawning is allowed on completion of a correlation, then automatically
                // create the separate fragment. Previously tried to be clever and only do
                // it if thread count was greater than 1, but this lead to problems with the
                // activities being recorded in the potentially concurrent thread not being
                // anchored to the correct parent node - this is because the work was being
                // done in an async thread that may have been triggered after the spawning
                // thread had completed, resulting in a thread count of 1 (i.e. so not spawn
                // represented), but the stack had been changed due to the parent activities
                // being completed. So for consistency now just spawn a separate btxn fragment
                // where the performed activities may be started asynchronously at some
                // point in the future.
                if (allowSpawn) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Starting separate thread for asynchronous path: count="
                            + builder.getThreadCount() + " parent node=" + node);
                    }

                    // Clear the current association between the thread and fragment
                    fragmentManager.setFragmentBuilder(null);

                    // Cause a new fragment to be created for this thread
                    FragmentBuilder spawnedBuilder = fragmentManager.getFragmentBuilder();

                    spawnFragment(builder, node, pos, spawnedBuilder);
                } else {
                    fragmentManager.setFragmentBuilder(builder);
                }
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "completeCorrelation failed", t);
            }
        }
    }

    /**
     * This method creates a new linked fragment to handle some asynchronous
     * activities.
     */
    protected void spawnFragment(FragmentBuilder parentBuilder, Node node, int position,
                            FragmentBuilder spawnedBuilder) {
        Trace trace = parentBuilder.getTrace();
        String id = UUID.randomUUID().toString();
        String location = null;
        String uri = null;
        String operation = null;
        String type = null; // Set to null to indicate internal

        if (!trace.getNodes().isEmpty()) {
            Node rootNode = trace.getNodes().get(0);
            uri = rootNode.getUri();
            operation = rootNode.getOperation();
        }

        // Create Producer node to represent the internal connection to the spawned fragment
        Producer producer = new Producer();
        producer.setEndpointType(type);
        producer.setUri(uri);
        producer.setOperation(operation);
        producer.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, id));

        if (node != null && node.containerNode()) {
            initNode(producer);
            if (position == -1) {
                ((ContainerNode)node).getNodes().add(producer);
            } else {
                ((ContainerNode)node).getNodes().add(position, producer);
            }
        } else {
            push(location, parentBuilder, producer);
            pop(location, parentBuilder, Producer.class, uri);
        }

        // Transfer relevant details to the spawned trace and builder
        Trace spawnedTrace = spawnedBuilder.getTrace();
        spawnedTrace.setBusinessTransaction(trace.getBusinessTransaction());
        spawnedTrace.setPrincipal(trace.getPrincipal());

        spawnedBuilder.setLevel(parentBuilder.getLevel());

        // Create Consumer node to represent other end of internal spawn link
        Consumer consumer = new Consumer();
        consumer.setEndpointType(type);
        consumer.setUri(uri);
        consumer.setOperation(operation);
        consumer.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, id));

        push(location, spawnedBuilder, consumer);

        // Pop immediately as easier than attempting to determine end of spawned scope and
        // removing it from the stack then.
        // TODO:  Could look at moving subsequent top level nodes under this Consumer
        // at the point when the fragment is recorded
        pop(location, spawnedBuilder, Consumer.class, uri);
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
     * @see org.hawkular.btm.api.client.SessionManager#ignoreNode()
     */
    @Override
    public void ignoreNode() {
        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            builder.ignoreNode();
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
                    if (builder.isCompleteExceptIgnoredNodes() && log.isLoggable(Level.FINEST)) {
                        log.finest("Business transaction fragment only contains 'ignored' nodes: "
                            + fragmentManager.getFragmentBuilder());
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest("ERROR: Business transaction has not completed: "
                            + fragmentManager.getFragmentBuilder());
                    }
                }
            }
        } catch (Throwable t) {
            if (log.isLoggable(warningLogLevel)) {
                log.log(warningLogLevel, "assertComplete failed", t);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#setState(java.lang.Object, java.lang.String, java.lang.Object, boolean)
     */
    @Override
    public void setState(Object context, String name, Object value, boolean session) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set state: context=" + context + " name=" + name + " value="
                    + value + " session=" + session);
        }

        if (session) {
            try {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                if (builder != null) {
                    builder.setState(context, name, value);
                }
            } catch (Throwable t) {
                if (log.isLoggable(warningLogLevel)) {
                    log.log(warningLogLevel, "setState failed", t);
                }
            }
        } else {
            throw new UnsupportedOperationException("Only session state supported currently");
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#getState(java.lang.Object, java.lang.String, boolean)
     */
    @Override
    public Object getState(Object context, String name, boolean session) {
        Object ret = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get state: context=" + context + " name=" + name + " session=" + session);
        }

        if (session) {
            try {
                FragmentBuilder builder = fragmentManager.getFragmentBuilder();

                if (builder != null) {
                    ret = builder.getState(context, name);
                }
            } catch (Throwable t) {
                if (log.isLoggable(warningLogLevel)) {
                    log.log(warningLogLevel, "getState failed", t);
                }
            }
        } else {
            throw new UnsupportedOperationException("Only session state supported currently");
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get state: context=" + context + " name=" + name + " session="
                    + session + " value is: " + ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.TraceCollector#session()
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
            log.finest("Correlation (" + correlations.size() + "): " + correlations);
            log.finest("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        }
    }

}
