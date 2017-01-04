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
package org.hawkular.apm.server.api.services;

import java.util.List;

/**
 * This interface represents a cache.
 *
 * @author gbrown
 *
 * @param <T> The type of cached information
 */
public interface Cache<T> {

    /**
     * This method retrieves the information based on the supplied
     * id.
     *
     * @param tenantId The tenant id
     * @param id The id
     * @return The information, or null if not found
     */
    T get(String tenantId, String id);

    /**
     * This methods stores the information in the cache. The
     * id for each piece of information is obtained from the information
     * itself.
     *
     * @param tenantId The tenant id
     * @param information The list of information
     * @throws Failed to store information in the cache
     */
    void store(String tenantId, List<T> information) throws CacheException;

}
