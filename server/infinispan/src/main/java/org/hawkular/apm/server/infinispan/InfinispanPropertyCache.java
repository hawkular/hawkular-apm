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

package org.hawkular.apm.server.infinispan;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.ObservableProperty;
import org.hawkular.apm.server.api.services.PropertyService;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * @author Pavol Loffay
 */
public class InfinispanPropertyCache implements PropertyService, ServiceLifecycle {

    private static final Logger log = Logger.getLogger(InfinispanSpanCache.class.getName());

    protected static final String CACHE_NAME = "property";

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer cacheContainer;

    private Cache<String, ObservableProperty<?>> propertyCache;


    public InfinispanPropertyCache() {}

    public InfinispanPropertyCache(Cache<String, ObservableProperty<?>> cache) {
        this.propertyCache = cache;
    }

    @Override
    @PostConstruct
    public void init() {
        // If cache container not already provisions, then must be running outside of a JEE
        // environment, so create a default cache container
        if (cacheContainer == null) {
            log.fine("Using default cache");
            propertyCache = InfinispanCacheManager.getDefaultCache(CACHE_NAME);
        } else {
            log.fine("Using container provided cache");
            propertyCache = cacheContainer.getCache(CACHE_NAME);
        }
    }

    @Override
    public ObservableProperty get(String tenantId, String id) {
        ObservableProperty<?> observableProperty = propertyCache.get(id);

        if (observableProperty == null) {
            observableProperty = getFromSystemOrEnvVariables(id);
            if (observableProperty == null) {
                return null;
            }

            try {
                store(tenantId, Collections.singletonList(observableProperty));
            } catch (CacheException ex) {
                log.severe(String.format("Could not store system or env property: %s, to cache: ", id));
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get property [id=" + id + "] = " + observableProperty);
        }

        return observableProperty;
    }

    @Override
    public void store(String tenantId, List<ObservableProperty> observableProperties) throws CacheException {
        if (cacheContainer != null) {
            propertyCache.startBatch();
        }

        for (ObservableProperty observableProperty : observableProperties) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Store property [id=" + observableProperty.getId() + "]: " + observableProperty);
            }

            ObservableProperty propFromCache = propertyCache.get(observableProperty.getId());
            if (propFromCache != null) {
                propFromCache.addAllObservers(observableProperty.getObservers());
                propFromCache.updateValue(observableProperty.getValue());
            } else {
                propertyCache.put(observableProperty.getId(), observableProperty);
            }
        }

        if (cacheContainer != null) {
            propertyCache.endBatch(true);
        }
    }

    private ObservableProperty<String> getFromSystemOrEnvVariables(String name) {
        String strProperty = PropertyUtil.getProperty(name);
        if (strProperty != null) {
            return new ObservableProperty<>(name, strProperty);
        }

        return null;
    }
}
