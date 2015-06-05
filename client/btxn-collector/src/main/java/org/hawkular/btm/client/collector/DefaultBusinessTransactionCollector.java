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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.hawkular.btm.api.client.BusinessTransactionCollector;
import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.InvocationNode;
import org.hawkular.btm.api.model.btxn.Message;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.btxn.Service;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.api.util.ServiceResolver;
import org.hawkular.btm.client.collector.internal.FragmentBuilder;
import org.hawkular.btm.client.collector.internal.FragmentManager;

/**
 * @author gbrown
 */
public class DefaultBusinessTransactionCollector implements BusinessTransactionCollector {

    private static final Logger log = Logger.getLogger(DefaultBusinessTransactionCollector.class.getName());

    private FragmentManager fragmentManager = new FragmentManager();

    private String tenantId = System.getProperty("hawkular-btm.tenantId");

    private BusinessTransactionService businessTransactionService;

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
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#consumerStart(
     *                      java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void consumerStart(String type, String uri, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer start: type=" + type + " uri=" + uri + " values=" + values);
        }

        Consumer consumer = new Consumer();
        consumer.setEndpointType(type);
        consumer.setUri(uri);

        processValues(consumer, true, values);

        push(consumer);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#consumerEnd(
     *                      java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void consumerEnd(String type, String uri, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Consumer end: type=" + type + " uri=" + uri + " values=" + values);
        }

        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            Consumer consumer = pop(Consumer.class);

            processValues(consumer, false, values);

            // Check for completion
            checkForCompletion(builder);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#serviceStart(
     *                      java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void serviceStart(String type, String operation, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service start: type=" + type + " operation=" + operation + " values=" + values);
        }

        Service service = new Service();
        service.setServiceType(type);
        service.setOperation(operation);

        processValues(service, true, values);

        push(service);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#serviceEnd(
     *              java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void serviceEnd(String type, String operation, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Service end: type=" + type + " operation=" + operation + " values=" + values);
        }

        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            Service service = pop(Service.class);

            processValues(service, false, values);

            // Check for completion
            checkForCompletion(builder);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#componentStart(
     *                      java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void componentStart(String type, String operation, String uri, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component start: type=" + type + " operation=" + operation + " uri=" + uri + " values="
                    + values);
        }

        Component component = new Component();
        component.setComponentType(type);
        component.setUri(uri);
        component.setOperation(operation);

        processValues(component, true, values);

        push(component);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#componentEnd(
     *                      java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void componentEnd(String type, String operation, String uri, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Component end: type=" + type + " operation=" + operation + " uri=" + uri + " values=" + values);
        }

        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            Component component = pop(Component.class);

            processValues(component, false, values);

            // Check for completion
            checkForCompletion(builder);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#producerStart(
     *                      java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void producerStart(String type, String uri, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer start: type=" + type + " uri=" + uri + " values=" + values);
        }

        Producer producer = new Producer();
        producer.setEndpointType(type);
        producer.setUri(uri);

        processValues(producer, true, values);

        push(producer);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.BusinessTransactionCollector#producerEnd(
     *                      java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void producerEnd(String type, String uri, Object... values) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Producer end: type=" + type + " uri=" + uri + " values=" + values);
        }

        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            Producer producer = pop(Producer.class);

            processValues(producer, false, values);

            // Check for completion
            checkForCompletion(builder);
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
    protected <T extends Node> T pop(Class<T> cls) {
        FragmentBuilder builder = fragmentManager.getFragmentBuilder();

        if (builder != null) {
            // Check node is of appropriate type
            if (builder.getCurrentNode().getClass() == cls) {
                Node node = builder.popNode();
                node.setDuration(System.currentTimeMillis() - node.getStartTime());
                return cls.cast(node);
            }
        }

        return null;
    }

    /**
     * This method processes the values associated with the start or end of a scoped
     * activity.
     *
     * @param node The node
     * @param req Whether processing a request
     * @param values The values
     */
    protected void processValues(InvocationNode node, boolean req, Object... values) {
        if (values != null) {
            Message m = new Message();
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    // TODO: Type conversion based on provided config
                    m.getParameters().add(values[i].toString());
                }
            }
            if (req) {
                node.setRequest(m);
            } else {
                node.setResponse(m);
            }
        }
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

            log.info("Record business transaction: " + btxn);

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

            fragmentManager.clear();
        }
    }

}
