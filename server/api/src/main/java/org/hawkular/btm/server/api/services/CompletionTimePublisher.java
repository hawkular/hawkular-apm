/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.server.api.services;

import java.util.List;

import org.hawkular.btm.api.model.analytics.CompletionTime;

/**
 * This interface provides the capability for publishing completion time
 * information.
 *
 * @author gbrown
 */
public interface CompletionTimePublisher {

    /**
     * This method publishes the list of completion times.
     *
     * @param tenantId The tenant
     * @param cts The list of completion times
     * @throws Exception Failed to publish completion times
     */
    void publish(String tenantId, List<CompletionTime> cts) throws Exception;

}
