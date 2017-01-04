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
 * This interface provides the capability for publishing a list of items.
 *
 * @author gbrown
 */
public interface Publisher<T> {

    /**
     * The initial retry count for messages sent by this publisher.
     *
     * @return The initial retry count
     */
    int getInitialRetryCount();

    /**
     * This method publishes the list of items.
     *
     * @param tenantId The tenant
     * @param items The list of items
     * @throws Exception Failed to publish
     */
    void publish(String tenantId, List<T> items) throws Exception;

    /**
     * This method publishes the list of items.
     *
     * @param tenantId The tenant
     * @param items The list of items
     * @param retryCount The retry count
     * @param delay The delay
     * @throws Exception Failed to publish
     */
    void publish(String tenantId, List<T> items, int retryCount, long delay) throws Exception;

    /**
     * This method publishes the list of items.
     *
     * @param tenantId The tenant
     * @param items The list of items
     * @param subscriber The optional name of the subscriber requesting the retry
     * @param retryCount The retry count
     * @param delay The delay
     * @throws Exception Failed to publish
     */
    void retry(String tenantId, List<T> items, String subscriber, int retryCount, long delay) throws Exception;

    /**
     * This method sets the metric handler for the publisher.
     *
     * @param handler The handler
     */
    void setMetricHandler(PublisherMetricHandler<T> handler);

}
