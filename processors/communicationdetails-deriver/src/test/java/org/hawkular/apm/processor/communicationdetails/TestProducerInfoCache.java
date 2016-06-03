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
package org.hawkular.apm.processor.communicationdetails;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gbrown
 */
public class TestProducerInfoCache implements ProducerInfoCache {

    private Map<String,ProducerInfo> producerInfoCache = new HashMap<String,ProducerInfo>();

    /* (non-Javadoc)
     * @see org.hawkular.apm.processor.communicationdetails.ProducerInfoCache#get(java.lang.String, java.lang.String)
     */
    @Override
    public ProducerInfo get(String tenantId, String id) {
        return producerInfoCache.get(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.processor.communicationdetails.ProducerInfoCache#put(java.lang.String,
     *              java.lang.String, org.hawkular.apm.processor.communicationdetails.ProducerInfo)
     */
    @Override
    public void put(String tenantId, String id, ProducerInfo producerInfo) {
        producerInfoCache.put(id, producerInfo);
    }

}
