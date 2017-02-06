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
package org.hawkular.apm.client.collector;

import java.util.Map;

/**
 * This interface represents the collector used to register activity
 * related to a trace execution.
 *
 * @author gbrown
 */
public interface TraceCollector {

    /**
     * This method sets the id of the trace.
     *
     * @param location The instrumentation location
     * @param value The trace id
     */
    void setTraceId(String location, String value);

    /**
     * This method returns the trace id.
     *
     * @return The trace id
     */
    String getTraceId();

    /**
     * This method sets the name of the trace.
     *
     * @param location The instrumentation location
     * @param name The transaction name
     */
    void setTransaction(String location, String name);

    /**
     * This method returns the name of the transaction.
     *
     * @return The transaction name, or empty string if not defined
     */
    String getTransaction();

    /**
     * This method sets the reporting level.
     *
     * @param location The instrumentation location
     * @param level The reporting level
     */
    void setLevel(String location, String level);

    /**
     * This method returns the reporting level.
     *
     * @return The reporting level
     */
    String getLevel();

    /**
     * This method indicates the start of a message being consumed.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     * @param operation The operation
     * @param id The unique interaction id
     */
    void consumerStart(String location, String uri, String type, String operation, String id);

    /**
     * This method indicates the end of a message being consumed.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     * @param operation The operation
     */
    void consumerEnd(String location, String uri, String type, String operation);

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
     * @param operation The operation
     * @param id The unique interaction id
     */
    void producerStart(String location, String uri, String type, String operation, String id);

    /**
     * This method indicates the end of a message being produced.
     *
     * @param location The instrumentation location
     * @param uri The uri
     * @param type The endpoint type
     * @param operation The operation
     */
    void producerEnd(String location, String uri, String type, String operation);

    /**
     * This method identifies whether the in data (content and headers) for the current
     * trace and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the in message is processed
     */
    boolean isInProcessed(String location);

    /**
     * This method identifies whether the in content for the current
     * trace and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the in content is processed
     */
    boolean isInContentProcessed(String location);

    /**
     * This method identifies whether the out data (content and headers) for the current
     * trace and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the out message is processed
     */
    boolean isOutProcessed(String location);

    /**
     * This method identifies whether the out content for the current
     * trace and node will be processed to extract information.
     *
     * @param location The instrumentation location
     * @return Whether the out content is processed
     */
    boolean isOutContentProcessed(String location);

    /**
     * This method processes the supplied in headers and content.
     *
     * @param location The instrumentation location
     * @param headers The header values
     * @param values The values
     */
    void processIn(String location, Map<String, ?> headers, Object... values);

    /**
     * This method processes the supplied out headers and content.
     *
     * @param location The instrumentation location
     * @param headers The header values
     * @param values The values
     */
    void processOut(String location, Map<String, ?> headers, Object... values);

    /**
     * This method sets a property on the trace.
     *
     * @param location The instrumentation location
     * @param name The property name
     * @param value The property value
     */
    void setProperty(String location, String name, String value);

    /**
     * This method initialises a data buffer associated with the supplied object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void initInBuffer(String location, Object obj);

    /**
     * This method determines if there is an active in data buffer for
     * the supplied object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    boolean isInBufferActive(String location, Object obj);

    /**
     * This method appends data to the buffer associated with the supplied object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     */
    void appendInBuffer(String location, Object obj, byte[] data, int offset, int len);

    /**
     * This method records the data within a buffer associated with the supplied in
     * object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void recordInBuffer(String location, Object obj);

    /**
     * This method initialises a data buffer associated with the supplied out object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void initOutBuffer(String location, Object obj);

    /**
     * This method determines if there is an active out data buffer for
     * the supplied object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    boolean isOutBufferActive(String location, Object obj);

    /**
     * This method appends data to the buffer associated with the supplied out object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     */
    void appendOutBuffer(String location, Object obj, byte[] data, int offset, int len);

    /**
     * This method records the data within a buffer associated with the supplied
     * object.
     *
     * @param location The instrumentation location
     * @param obj The object associated with the buffer
     */
    void recordOutBuffer(String location, Object obj);

    /**
     * This method returns the session manager associated with the
     * current thread of execution.
     *
     * @return The session manager
     */
    SessionManager session();

}
