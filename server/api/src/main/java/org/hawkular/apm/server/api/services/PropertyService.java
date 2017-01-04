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

/**
 * Provides cache for a property domain class. This class also supports access to the system and
 * environmental properties through {@link Cache#get(String, String)} method. If it finds the property it
 * stores it in the cache.
 *
 * @author Pavol Loffay
 */
public interface PropertyService extends Cache<Property<?>> {

    /**
     * Store single property in the cache.
     *
     * @param tenantId tenant
     * @param property property
     * @throws CacheException
     */
    void store(String tenantId, Property<?> property) throws CacheException;

    /**
     * Remove property from the cache.
     *
     * @param tenantId tenant id
     * @param id property id
     */
    void remove(String tenantId, String id);

    /**
     * Add an observer for given property id. Callback {@link Observer#update(Property, Property)} is called when a
     * client invokes This{@link #store(String, Property)}. Observers can be added before storing a
     * Property with given id.
     *
     * @param tenantId tenant
     * @param id id of the property
     * @param observer observer
     */
    void addObserver(String tenantId, String id, Observer observer);

    /**
     * Remove observer for given property id.
     *
     * @param tenantId tenant
     * @param id id of the property
     * @param observer
     */
    void removeObserver(String tenantId, String id, Observer observer);

    /**
     * Observer which is used to be notified for specific events.
     *
     * @param <Type>
     */
    interface Observer<Type> {
        /**
         * Called when new property is inserted into cache.
         *
         * @param property updated property
         */
        void created(Property<Type> property);

        /**
         * Called when property value is updated (stored property with the same id but different value)
         *
         * @param property new property
         */
        void updated(Property<Type> property);

        /**
         * Called when property is removed from the cache.
         *
         * @param property removed property
         */
        void removed(Property<Type> property);
    }
}
