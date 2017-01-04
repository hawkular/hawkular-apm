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

/**
 * The service registry interface.
 *
 * @author gbrown
 */
public interface ServiceRegistry {

    /**
     * This method returns a service instance associated with the supplied name.
     *
     * @param name The service name
     * @return The service instance
     */
    Service getServiceInstance(String name);

    /**
     * This method returns the service instance after use.
     *
     * @param service The service instance
     */
    void returnServiceInstance(Service service);

}
