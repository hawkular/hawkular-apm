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

import java.util.List;

/**
 * This interface represents a cache for producer info.
 *
 * @author gbrown
 */
public interface ProducerInfoCache {

    /**
     * This method retrieves the producer information based on the supplied
     * id.
     *
     * @param tenentId The tenant id
     * @param id The id
     * @return The producer info, or null if not found
     */
    ProducerInfo get(String tenantId, String id);

    /**
     * This methods stores the producer information in the cache.
     *
     * @param tenentId The tenant id
     * @param producerInfoList The producer information list
     */
    void store(String tenantId, List<ProducerInfo> producerInfoList);

}
