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
package org.hawkular.apm.performance.server;

import java.util.Map;
import java.util.UUID;

import org.hawkular.apm.client.collector.TraceCollector;
import org.hawkular.apm.client.collector.internal.DefaultTraceCollector;

/**
 * @author gbrown
 */
public class Service {

    private TraceCollector collector = new DefaultTraceCollector();

    private String uri;

    private String id;

    private String name;

    private Map<String, String> calledServices;

    private long lastUsed = System.currentTimeMillis();

    private ServiceRegistry registry;

    public Service(String name, String uri, String id, ServiceRegistry reg, Map<String, String> calledServices) {
        this.uri = uri;
        this.name = name;
        this.id = id;
        this.setRegistry(reg);
        this.calledServices = calledServices;
    }

    /**
     * @return the collector
     */
    public TraceCollector getCollector() {
        return collector;
    }

    /**
     * @param collector the collector to set
     */
    public void setCollector(TraceCollector collector) {
        this.collector = collector;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the calledServices
     */
    public Map<String, String> getCalledServices() {
        return calledServices;
    }

    /**
     * @param calledServices the calledServices to set
     */
    public void setCalledServices(Map<String, String> calledServices) {
        this.calledServices = calledServices;
    }

    /**
     * @return the registry
     */
    public ServiceRegistry getRegistry() {
        return registry;
    }

    /**
     * @param registry the registry to set
     */
    public void setRegistry(ServiceRegistry registry) {
        this.registry = registry;
    }

    /**
     * This method simulates calling the service.
     *
     * @param mesg The message to be exchanged
     * @param interactionId The interaction id, or null if initial call
     * @param btxnName The optional business txn name
     */
    public void call(Message mesg, String interactionId, String btxnName) {
        boolean activated = collector.session().activate(uri, null, interactionId);

        if (activated) {
            collector.consumerStart(null, uri, "Test", null, interactionId);
            collector.setTransaction(null, btxnName);
        }

        if (calledServices != null) {
            String calledServiceName = calledServices.get(mesg.getType());

            // Introduce a delay related to the business logic
            synchronized (this) {
                try {
                    wait(((int) Math.random() % 300) + 50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (calledServiceName != null) {
                Service calledService = registry.getServiceInstance(calledServiceName);

                String nextInteractionId = UUID.randomUUID().toString();

                if (activated) {
                    collector.producerStart(null, calledService.getUri(), "Test", null, nextInteractionId);
                }

                // Introduce a delay related to the latency
                synchronized (this) {
                    try {
                        wait(((int) Math.random() % 100) + 10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                calledService.call(mesg, nextInteractionId, collector.getTransaction());

                if (activated) {
                    collector.producerEnd(null, calledService.getUri(), "Test", null);
                }
            }
        }

        if (activated) {
            collector.consumerEnd(null, uri, "Test", null);
        }

        setLastUsed(System.currentTimeMillis());

        // Return instance to stack
        registry.returnServiceInstance(this);
    }

    /**
     * @return the lastUsed
     */
    public long getLastUsed() {
        return lastUsed;
    }

    /**
     * @param lastUsed the lastUsed to set
     */
    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }
}
