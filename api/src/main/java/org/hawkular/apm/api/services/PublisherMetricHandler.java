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
package org.hawkular.apm.api.services;

import java.util.List;

/**
 * This interface enables an application to register a handler for metric
 * information from the publisher.
 *
 * @author gbrown
 */
public interface PublisherMetricHandler<T> {

    /**
     * This method is invoked with the time taken to publish the
     * supplied items.
     *
     * @param tenantId The tenantId
     * @param items The items
     * @param metric The time taken to publish
     */
    void published(String tenantId, List<T> items, long metric);

}
