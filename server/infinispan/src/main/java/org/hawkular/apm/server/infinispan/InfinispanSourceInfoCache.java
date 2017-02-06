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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.server.api.services.SourceInfoCache;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * This class provides the infinispan based implementation of the source info cache.
 *
 * @author gbrown
 */
@Singleton
public class InfinispanSourceInfoCache implements SourceInfoCache, ServiceLifecycle {

    private static final String CACHE_NAME = "sourceinfo";

    private static final Logger log = Logger.getLogger(InfinispanSourceInfoCache.class.getName());

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer cacheContainer;

    private Cache<String, SourceInfo> sourceInfo;

    public InfinispanSourceInfoCache() {}

    public InfinispanSourceInfoCache(CacheContainer cacheContainer) {
        this.cacheContainer = cacheContainer;
        init();
    }

    @PostConstruct
    public void init() {
        // If cache container not already provisions, then must be running outside of a JEE
        // environment, so create a default cache container
        if (cacheContainer == null) {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using default cache");
            }
            sourceInfo = InfinispanCacheManager.getDefaultCache(CACHE_NAME);
        } else {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using container provided cache");
            }
            sourceInfo = cacheContainer.getCache(CACHE_NAME);
        }
    }

    @Override
    public SourceInfo get(String tenantId, String id) {
        SourceInfo ret = sourceInfo.get(id);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get source info [id="+id+"] = "+ret);
        }

        return ret;
    }

    @Override
    public void store(String tenantId, List<SourceInfo> sourceInfoList) {
        if (cacheContainer != null) {
            sourceInfo.startBatch();
        }

        for (int i = 0; i < sourceInfoList.size(); i++) {
            SourceInfo si = sourceInfoList.get(i);

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Store source info [id="+si.getId()+"]: "+si);
            }

            sourceInfo.put(si.getId(), si, 1, TimeUnit.MINUTES);
        }

        if (cacheContainer != null) {
            sourceInfo.endBatch(true);
        }
    }

}
