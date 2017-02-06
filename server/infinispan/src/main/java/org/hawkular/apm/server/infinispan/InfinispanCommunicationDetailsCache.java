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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.CommunicationDetailsCache;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * This class provides the infinispan based implementation of the communication details cache.
 *
 * @author gbrown
 */
@Singleton
public class InfinispanCommunicationDetailsCache implements CommunicationDetailsCache, ServiceLifecycle {

    private static final Logger log = Logger.getLogger(InfinispanCommunicationDetailsCache.class.getName());

    protected static final String CACHE_NAME = "communicationdetails";

    protected static final String MULTI_CONSUMER_CACHE_NAME = "communicationdetailsMulticonsumer";

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer cacheContainer;

    private Cache<String, CommunicationDetails> communicationDetails;

    private Cache<String, List<CommunicationDetails>> communicationDetailsMultiConsumers;

    public InfinispanCommunicationDetailsCache() {}

    public InfinispanCommunicationDetailsCache(CacheContainer cacheContainer) {
        this.cacheContainer = cacheContainer;
        init();
    }

    @PostConstruct
    public void init() {
        if (communicationDetails != null && communicationDetailsMultiConsumers != null) {
            return;
        }

        // If cache container not already provisions, then must be running outside of a JEE
        // environment, so create a default cache container
        if (cacheContainer == null) {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using default cache");
            }
            communicationDetails = InfinispanCacheManager.getDefaultCache(CACHE_NAME);
            communicationDetailsMultiConsumers = InfinispanCacheManager.getDefaultCache(MULTI_CONSUMER_CACHE_NAME);
        } else {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using container provided cache");
            }
            communicationDetails = cacheContainer.getCache(CACHE_NAME);
            communicationDetailsMultiConsumers = cacheContainer.getCache(MULTI_CONSUMER_CACHE_NAME);
        }
    }

    @Override
    public CommunicationDetails get(String tenantId, String id) {
        CommunicationDetails ret = communicationDetails.get(id);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get communication details [id="+id+"] = "+ret);
        }

        return ret;
    }

    @Override
    public void store(String tenantId, List<CommunicationDetails> details) throws CacheException {
        if (cacheContainer != null) {
            communicationDetails.startBatch();
            communicationDetailsMultiConsumers.startBatch();
        }

        for (int i = 0; i < details.size(); i++) {
            CommunicationDetails cd = details.get(i);

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Store communication details [link id="+cd.getLinkId()+"]: "+cd);
            }

            // TODO: HWKBTM-348 How long should details be cached if related to long running
            // processes with wait intervals? Issue is that all communication details
            // after a particular point would need to be retained - but how will
            // we know? Might need a mechanism for processing the short running parts
            // of the process into a more concise representation which is retained?
            // Or possibly it is not a problem as those short term bits are only
            // created after the long wait anyway.

            if (cd.isMultiConsumer()) {
                synchronized (communicationDetailsMultiConsumers) {
                    List<CommunicationDetails> list = communicationDetailsMultiConsumers.get(cd.getLinkId());
                    if (list == null) {
                        list = new CopyOnWriteArrayList<>();
                    }
                    list.add(cd);

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Store multiconsumer communication details list [link id="
                                +cd.getLinkId()+"]: "+list);
                    }

                    communicationDetailsMultiConsumers.put(cd.getLinkId(), list, 1, TimeUnit.MINUTES);
                }
            } else {
                communicationDetails.put(cd.getLinkId(), cd, 1, TimeUnit.MINUTES);
            }
        }

        if (cacheContainer != null) {
            communicationDetails.endBatch(true);
            communicationDetailsMultiConsumers.endBatch(true);
        }
    }

    @Override
    public List<CommunicationDetails> getById(String tenantId, String id) {
        if (id == null) {
            throw new NullPointerException("Id should not be null!");
        }

        return communicationDetailsMultiConsumers.containsKey(id) ? communicationDetailsMultiConsumers.get(id)
                : Collections.emptyList();
    }
}
