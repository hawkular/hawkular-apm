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

package org.hawkular.apm.server.infinispan;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.Property;
import org.hawkular.apm.server.api.services.PropertyService;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.impl.EventImpl;

/**
 * @author Pavol Loffay
 */
@Singleton
public class InfinispanPropertyCache implements PropertyService, ServiceLifecycle {

    private static final Logger log = Logger.getLogger(InfinispanPropertyCache.class.getName());

    protected static final String CACHE_NAME = "property";

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer cacheContainer;

    private Cache<String, Property<?>> propertyCache;
    private CacheEventListener eventListener;


    public InfinispanPropertyCache() {
        this.eventListener = new CacheEventListener();
    }

    public InfinispanPropertyCache(CacheContainer cacheContainer) {
        this();
        this.cacheContainer = cacheContainer;
        init();
    }

    @Override
    @PostConstruct
    public void init() {
        if (propertyCache != null) {
            return;
        }

        // If cache container not already provisions, then must be running outside of a JEE
        // environment, so create a default cache container
        if (cacheContainer == null) {
            log.fine("Using default cache");
            propertyCache = InfinispanCacheManager.getDefaultCache(CACHE_NAME);
        } else {
            log.fine("Using container provided cache");
            propertyCache = cacheContainer.getCache(CACHE_NAME);
        }

        propertyCache.addListener(eventListener);
    }

    @Override
    public Property get(String tenantId, String id) {
        Property<?> property = propertyCache.get(id);

        if (property == null) {
            property = getFromSystemOrEnvVariables(id);
            if (property == null) {
                return null;
            }

            try {
                store(tenantId, property);
            } catch (CacheException e) {
                log.severe(String.format("Could not store environmental property: %s to cache", property));
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get property [id=" + id + "] = " + property);
        }

        return property;
    }

    @Override
    public void store(String tenantId, List<Property<?>> properties) throws CacheException {
        if (cacheContainer != null) {
            propertyCache.startBatch();
        }

        for (Property<?> property: properties) {
            store(tenantId, property);
        }

        if (cacheContainer != null) {
            propertyCache.endBatch(true);
        }
    }

    @Override
    public void store(String tenantId, Property<?> property) throws CacheException {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Store property [id=" + property.getId() + "]: " + property);
        }

        propertyCache.put(property.getId(), property);
    }

    @Override
    public void remove(String tenantId, String id) {
        propertyCache.remove(id);
    }

    @Override
    public void addObserver(String tenantId, String id, Observer observer) {
        List<Observer> observers = eventListener.observersMap.get(id);
        if (observers == null) {
            observers = new CopyOnWriteArrayList<>();
            eventListener.observersMap.put(id, observers);
        }

        observers.add(observer);
    }

    @Override
    public void removeObserver(String tenantId, String id, Observer observer) {
        List<Observer> observers = eventListener.observersMap.get(id);
        if (observers != null) {
            observers.remove(observer);

            if (observers.isEmpty()) {
                eventListener.observersMap.remove(id);
            }
        }
    }

    @Listener
    private static class CacheEventListener {

        private Map<String, List<Observer>> observersMap = new ConcurrentHashMap<>();

        @CacheEntryCreated
        public void created(CacheEntryCreatedEvent<String, Property<?>> event) {
            if (event.isPre()) {
                return;
            }

            List<Observer> observers = this.observersMap.get(event.getKey());
            if (observers != null) {
                observers.forEach(observer -> observer.created(event.getValue()));
            }
        }

        @CacheEntryModified
        public void updated(CacheEntryModifiedEvent<String, Property<?>> event) {
            if (event.isPre()) {
                return;
            }

            Property<?> oldValue = (Property<?>) ((EventImpl) event).getOldValue();
            Property<?> newValue = event.getValue();

            if (!newValue.equals(oldValue)) {
                List<Observer> observers = this.observersMap.get(event.getKey());
                if (observers != null) {
                    observers.forEach(observer -> observer.updated(newValue));
                }
            }
        }

        @CacheEntryRemoved
        public void removed(CacheEntryRemovedEvent<String, Property<?>> event) {
            if (event.isPre()) {
                return;
            }

            List<Observer> observers = this.observersMap.get(event.getKey());
            if (observers != null && event.getOldValue() != null) {
                observers.forEach(observer -> observer.removed(event.getValue()));
            }
        }
    }

    private Property<String> getFromSystemOrEnvVariables(String name) {
        String strProperty = PropertyUtil.getProperty(name);
        if (strProperty != null) {
            return new Property<>(name, strProperty);
        }

        return null;
    }
}
