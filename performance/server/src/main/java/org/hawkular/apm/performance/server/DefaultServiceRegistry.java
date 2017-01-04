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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * @author gbrown
 */
public class DefaultServiceRegistry implements ServiceRegistry {

    /** Default time interval (in milliseconds) that an unused service instance will be discarded */
    private static final int DEFAULT_SERVICE_EXPIRY_INTERVAL = 10000;

    private Map<String, ServiceConfiguration> serviceConfigs = new HashMap<String, ServiceConfiguration>();

    private Map<String, Stack<Service>> services = new HashMap<String, Stack<Service>>();

    private Metrics metrics;

    private long expiryInterval = DEFAULT_SERVICE_EXPIRY_INTERVAL;

    public DefaultServiceRegistry(SystemConfiguration sysConfig, Metrics metrics) {
        this.metrics = metrics;

        for (ServiceConfiguration serviceConfig : sysConfig.getServices()) {
            serviceConfigs.put(serviceConfig.getName(), serviceConfig);
        }

        expiryInterval = PropertyUtil.getPropertyAsInteger("SERVICE_EXPIRY_INTERVAL", DEFAULT_SERVICE_EXPIRY_INTERVAL);
    }

    @Override
    public Service getServiceInstance(String name) {
        Stack<Service> stack = null;

        synchronized (services) {
            stack = services.get(name);
            if (stack == null) {
                stack = new Stack<Service>();
                services.put(name, stack);
            }
        }

        synchronized (stack) {
            if (stack.isEmpty()) {
                // Allocate new instance
                return newServiceInstance(name);
            } else {
                Service service = stack.pop();

                // Check service hasn't expired
                if (System.currentTimeMillis() - service.getLastUsed() > expiryInterval) {

                    for (int i = 0; i < stack.size(); i++) {
                        // Record fact that service instance is being closed
                        metrics.closeService(name);
                    }

                    stack.clear();

                    return newServiceInstance(name);
                }

                return service;
            }
        }
    }

    protected Service newServiceInstance(String name) {
        metrics.createService(name);
        ServiceConfiguration serviceConfig = serviceConfigs.get(name);
        return new Service(name, serviceConfig.getUri(), UUID.randomUUID().toString(), this,
                serviceConfig.getCalledServices());
    }

    @Override
    public void returnServiceInstance(Service service) {
        Stack<Service> stack = null;

        synchronized (services) {
            stack = services.get(service.getName());
            if (stack == null) {
                stack = new Stack<Service>();
                services.put(service.getName(), stack);
            }
        }

        synchronized (stack) {
            stack.push(service);
        }
    }
}
