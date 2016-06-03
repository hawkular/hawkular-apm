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
package org.hawkular.apm.processor.btxncompletiontime;

import java.util.List;

import org.hawkular.apm.api.model.events.CommunicationDetails;

/**
 * This interface represents a cache for communication details.
 *
 * @author gbrown
 */
public interface CommunicationDetailsCache {

    /**
     * This method retrieves communication details based on the supplied
     * interaction id.
     *
     * @param tenentId The tenant id
     * @param id The interaction id
     * @return The communication details, or null if not currently available
     *                 or the interaction id is associated with multiple consumers
     */
    CommunicationDetails getSingleConsumer(String tenantId, String id);

    /**
     * This method retrieves zero or more communication details based on the supplied
     * interaction id.
     *
     * @param tenentId The tenant id
     * @param id The interaction id
     * @return The list of communication details, or an empty list if no details currently available
     */
    List<CommunicationDetails> getMultipleConsumers(String tenantId, String id);

    /**
     * This methods the communication details in the cache.
     *
     * @param tenentId The tenant id
     * @param details The list of communication details
     */
    void store(String tenantId, List<CommunicationDetails> details);

}
