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
package org.hawkular.btm.client.api;

import java.util.Map;

/**
 * This interface represents the collector used to register activity
 * related to a business transaction execution.
 *
 * @author gbrown
 */
public interface BusinessTransactionCollector {

    /**
     * The maximum number of business transactions to batch before sending to the server.
     */
    String BATCH_SIZE = "hawkular-btm.collector.batchsize";

    /**
     * The maximum time (in milliseconds) before sending a batch of business transactions to the server.
     */
    String BATCH_TIME = "hawkular-btm.collector.batchtime";

    /**
     * The thread pool size for reporting a batch of business transactions to the server.
     */
    String BATCH_THREADS = "hawkular-btm.collector.batchthreads";

    /**
     * This method sets the name of the business transaction.
     *
     * @param location The instrumentation location
     * @param name The business transaction name
     */
    void setName(String location, String name);

    /**
     * This method returns the name of the business transaction.
     *
     * @return The business transaction name, or empty string if not defined
     */
    String getName();

    /**
     * This method indicates the start of a message being consumed.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     * @param id The unique interaction id
     */
    void consumerStart(String location, String uri, String type, String id);

    /**
     * This method indicates the end of a message being consumed.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     */
    void consumerEnd(String location, String uri, String type);

    /**
     * This method indicates the start of a component invocation.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The component type
     * @param operation The operation
     */
    void componentStart(String location, String uri, String type, String operation);

    /**
     * This method indicates the end of a component invocation.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The component type
     * @param operation The operation
     */
    void componentEnd(String location, String uri, String type, String operation);

    /**
     * This method indicates the start of a message being produced.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     * @param id The unique interaction id
     */
    void producerStart(String location, String uri, String type, String id);

    /**
     * This method indicates the end of a message being produced.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     */
    void producerEnd(String location, String uri, String type);

    /**
     * This method identifies whether the request data (content and headers) for the current
     * business transaction and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the request is processed
     */
    boolean isRequestProcessed(String location);

    /**
     * This method identifies whether the request content for the current
     * business transaction and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the request content is processed
     */
    boolean isRequestContentProcessed(String location);

    /**
     * This method identifies whether the response data (content and headers) for the current
     * business transaction and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the response is processed
     */
    boolean isResponseProcessed(String location);

    /**
     * This method identifies whether the response content for the current
     * business transaction and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the response content is processed
     */
    boolean isResponseContentProcessed(String location);

    /**
     * This method processes the supplied request headers and content.
     *
     * @param location The instrumentation location
     * @param headers The header values
     * @param values The values
     */
    void processRequest(String location, Map<String, ?> headers, Object... values);

    /**
     * This method processes the supplied response headers and content.
     *
     * @param location The instrumentation location
     * @param headers The header values
     * @param values The values
     */
    void processResponse(String location, Map<String, ?> headers, Object... values);

    /**
     * This method sets a fault on the current node.
     *
     * @param location The instrumentation location
     * @param value The fault value
     * @param description The optional fault description
     */
    void setFault(String location, String value, String description);

    /**
     * This method sets a property on the business transaction.
     *
     * @param location The instrumentation location
     * @param name The property name
     * @param value The property value
     */
    void setProperty(String location, String name, String value);

    /**
     * This method sets a detail on the current node.
     *
     * @param location The instrumentation location
     * @param name The detail name
     * @param value The detail value
     */
    void setDetail(String location, String name, String value);

    /**
     * This method initialises a data buffer associated with the supplied request object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void initRequestBuffer(String location, Object obj);

    /**
     * This method determines if there is an active request data buffer for
     * the supplied object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    boolean isRequestBufferActive(String location, Object obj);

    /**
     * This method appends data to the buffer associated with the supplied request object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     */
    void appendRequestBuffer(String location, Object obj, byte[] data, int offset, int len);

    /**
     * This method records the data within a buffer associated with the supplied request
     * object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void recordRequestBuffer(String location, Object obj);

    /**
     * This method initialises a data buffer associated with the supplied response object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void initResponseBuffer(String location, Object obj);

    /**
     * This method determines if there is an active response data buffer for
     * the supplied object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    boolean isResponseBufferActive(String location, Object obj);

    /**
     * This method appends data to the buffer associated with the supplied response object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     */
    void appendResponseBuffer(String location, Object obj, byte[] data, int offset, int len);

    /**
     * This method records the data within a buffer associated with the supplied response
     * object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void recordResponseBuffer(String location, Object obj);

    /**
     * This method returns the session manager associated with the
     * current thread of execution.
     *
     * @return The session manager
     */
    SessionManager session();

}
