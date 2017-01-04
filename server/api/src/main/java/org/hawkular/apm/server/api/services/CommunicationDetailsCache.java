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

import java.util.List;

import org.hawkular.apm.api.model.events.CommunicationDetails;

/**
 * This interface represents a cache for communication details.
 *
 * @author gbrown
 */
public interface CommunicationDetailsCache extends Cache<CommunicationDetails> {

    /**
     * Method returns all communication details with given id. Because fragments share the same id
     * it is possible that this method returns multiple objects.
     *
     * @param tenantId tenant
     * @param id id of the communication details
     * @return communication details with the same id
     */
    List<CommunicationDetails> getById(String tenantId, String id);
}
