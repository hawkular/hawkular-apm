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
package org.hawkular.apm.api.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * This utility class provides service lookup and management of singletons
 * based on the java ServiceLoader mechanism. This mechanism will be used
 * in situations where other higher level component dependency injection
 * mechanisms are not appropriate (e.g. instrumenting the client JVM).
 *
 * @author gbrown
 */
public class ServiceResolver {

    private static Map<Class<?>, Object> singletons = new HashMap<Class<?>, Object>();

    /**
     * This method identifies a service implementation that implements
     * the supplied interface, and returns it as a singleton, so that subsequent
     * calls for the same service will get the same instance.
     *
     * @param intf The interface
     * @return The service, or null if not found
     *
     * @param <T> The service interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSingletonService(Class<T> intf) {
        T ret = null;

        synchronized (singletons) {
            if (singletons.containsKey(intf)) {
                ret = (T) singletons.get(intf);
            } else {
                List<T> services = getServices(intf);

                if (!services.isEmpty()) {
                    ret = services.get(0);
                }

                singletons.put(intf, ret);
            }
        }

        return ret;
    }

    /**
     * This method returns the list of service implementations that implement
     * the supplied interface.
     *
     * @param intf The interface
     * @return The list
     *
     * @param <T> The service interface
     */
    public static <T> List<T> getServices(Class<T> intf) {
        List<T> ret = new ArrayList<T>();

        for (T service : ServiceLoader.load(intf)) {
            if (!(service instanceof ServiceStatus) || ((ServiceStatus)service).isAvailable()) {
                if (service instanceof ServiceLifecycle) {
                    ((ServiceLifecycle)service).init();
                }

                ret.add(service);
            }
        }

        return ret;
    }
}
