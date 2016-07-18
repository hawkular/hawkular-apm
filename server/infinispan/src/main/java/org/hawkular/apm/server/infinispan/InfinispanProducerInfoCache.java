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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.server.api.services.ProducerInfoCache;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * This class provides the infinispan based implementation of the producer info cache.
 *
 * @author gbrown
 */
@Singleton
public class InfinispanProducerInfoCache implements ProducerInfoCache, ServiceLifecycle {

    /**  */
    private static final String CACHE_NAME = "producerinfo";

    private static final Logger log = Logger.getLogger(InfinispanProducerInfoCache.class.getName());

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer container;

    private Cache<String, ProducerInfo> producerInfo;

    @PostConstruct
    public void init() {
        // If cache container not already provisions, then must be running outside of a JEE
        // environment, so create a default cache container
        if (container == null) {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using default cache");
            }
            setProducerInfo(InfinispanCacheManager.getDefaultCache(CACHE_NAME));
        } else {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using container provided cache");
            }
            setProducerInfo(container.getCache(CACHE_NAME));
        }
    }

    /**
     * @return the producerInfo
     */
    public Cache<String, ProducerInfo> getProducerInfo() {
        return producerInfo;
    }

    /**
     * @param producerInfo the producerInfo to set
     */
    public void setProducerInfo(Cache<String, ProducerInfo> producerInfo) {
        this.producerInfo = producerInfo;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.processor.communicationdetails.ProducerInfoCache#get(java.lang.String, java.lang.String)
     */
    @Override
    public ProducerInfo get(String tenantId, String id) {
        ProducerInfo ret = producerInfo.get(id);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get producer info [id="+id+"] = "+ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.processor.communicationdetails.ProducerInfoCache#store(java.lang.String, java.util.List)
     */
    @Override
    public void store(String tenantId, List<ProducerInfo> producerInfoList) {
        if (container != null) {
            producerInfo.startBatch();
        }

        for (int i = 0; i < producerInfoList.size(); i++) {
            ProducerInfo pi = producerInfoList.get(i);

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Store producer info [id="+pi.getId()+"]: "+pi);
            }

            producerInfo.put(pi.getId(), pi, 1, TimeUnit.MINUTES);
        }

        if (container != null) {
            producerInfo.endBatch(true);
        }
    }

}
