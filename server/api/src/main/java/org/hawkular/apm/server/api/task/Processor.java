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
package org.hawkular.apm.server.api.task;

import java.util.List;

/**
 * This interface represents a processor.
 *
 * @author gbrown
 */
public interface Processor<T, R> {

    /**
     * This method identifies the type of the processor.
     *
     * @return The processor type
     */
    ProcessorType getType();

    /**
     * This method enables the processor to perform some initialisation
     * tasks before processing the items individually to generate new
     * information.
     *
     * @param tenantId The optional tenant id
     * @param items
     * @throws RetryAttemptException Failed to process the item
     */
    void initialise(String tenantId, List<T> items) throws RetryAttemptException;

    /**
     * This method processes the supplied item to optionally
     * generated a new resulting value.
     *
     * @param tenantId The optional tenant id
     * @param item The item
     * @return The optional value
     * @throws RetryAttemptException Failed to process the item
     */
    R processOneToOne(String tenantId, T item) throws RetryAttemptException;

    /**
     * This method processes the supplied item to
     * generate zero or more resulting values.
     *
     * @param tenantId The optional tenant id
     * @param item The item
     * @return The list of values
     * @throws RetryAttemptException Failed to process the item
     */
    List<R> processOneToMany(String tenantId, T item) throws RetryAttemptException;

    /**
     * This method processes the supplied items to
     * generate zero or more resulting values.
     *
     * @param tenantId The optional tenant id
     * @param items The items
     * @return The list of values
     * @throws RetryAttemptException Failed to process the item
     */
    List<R> processManyToMany(String tenantId, List<T> items) throws RetryAttemptException;

    /**
     * This method determines the delivery delay (in milliseconds)
     * associated with the supplied list of results.
     *
     * @param results The results
     * @return The delivery delay, or 0 if no delay
     */
    long getDeliveryDelay(List<R> results);

    /**
     * This method determines the retry delay (in milliseconds)
     * associated with the supplied list of items.
     *
     * @param items The items
     * @param retryCount The retry count
     * @return The retry delay, or 0 if no delay
     */
    long getRetryDelay(List<T> items, int retryCount);

    /**
     * This method determines whether a retry expiration should be reported as
     * a warning. If not, then it will just be logged as detailed logging.
     *
     * @return Whether to report retry expiration as a warning message
     */
    boolean isReportRetryExpirationAsWarning();

    /**
     * This method is called once all of the items in the list of been
     * processed to generate new information. It can be used to
     * clean up any information managed by the processor related to
     * those items. Generally the processor should not store
     * any state outside the processing of the individual items.
     *
     * @param tenantId The optional tenant id
     * @param items
     */
    void cleanup(String tenantId, List<T> items);

    /**
     * This method determines what type of processing should be performed on the
     * inbound information.
     */
    public enum ProcessorType {

        /** Each inbound event will be processed individually to return a single result **/
        OneToOne,

        /** Each inbound event will be processed individually to return a zero or more results each **/
        OneToMany,

        /** The inbound events will be processed in bulk and may return zero or more results **/
        ManyToMany

    }
}
