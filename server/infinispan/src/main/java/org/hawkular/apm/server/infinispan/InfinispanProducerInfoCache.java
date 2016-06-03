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

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.hawkular.apm.processor.communicationdetails.ProducerInfo;
import org.hawkular.apm.processor.communicationdetails.ProducerInfoCache;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * This class provides the infinispan based implementation of the producer info cache.
 *
 * @author gbrown
 */
@Singleton
public class InfinispanProducerInfoCache implements ProducerInfoCache {

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer container;

    private Cache<String, ProducerInfo> producerInfo;

    @PostConstruct
    public void init() {
        setProducerInfo(container.getCache("producerinfo"));
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
        return producerInfo.get(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.processor.communicationdetails.ProducerInfoCache#put(java.lang.String,
     *                  org.hawkular.apm.processor.communicationdetails.ProducerInfo)
     */
    @Override
    public void put(String tenantId, String id, ProducerInfo pi) {
        producerInfo.put(id, pi, 1, TimeUnit.MINUTES);
    }

}
