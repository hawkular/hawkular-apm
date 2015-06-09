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
package org.hawkular.btm.api.client;

import java.util.Map;

/**
 * This interface represents the collector used to register activity
 * related to a business transaction execution.
 *
 * @author gbrown
 */
public interface BusinessTransactionCollector {

    /**
     * This method indicates the start of a message being consumed.
     *
     * @param type The endpoint type
     * @param uri The uri
     * @param id The unique interaction id
     * @param headers The header values
     * @param values The request values
     */
    void consumerStart(String type, String uri, String id, Map<String,?> headers, Object... values);

    /**
     * This method indicates the end of a message being consumed.
     *
     * @param type The endpoint type
     * @param uri The uri
     * @param headers The header values
     * @param values The response values
     */
    void consumerEnd(String type, String uri, Map<String,?> headers, Object... values);

    /**
     * This method indicates the start of a service invocation.
     *
     * @param type The service type
     * @param operation The operation
     * @param headers The header values
     * @param values The request values
     */
    void serviceStart(String type, String operation, Map<String,?> headers, Object... values);

    /**
     * This method indicates the end of a service invocation.
     *
     * @param type The service type
     * @param operation The operation
     * @param headers The header values
     * @param values The response values
     */
    void serviceEnd(String type, String operation, Map<String,?> headers, Object... values);

    /**
     * This method indicates the start of a component invocation.
     *
     * @param type The component type
     * @param operation The operation
     * @param uri The uri
     * @param headers The header values
     * @param values The request values
     */
    void componentStart(String type, String operation, String uri, Map<String,?> headers, Object... values);

    /**
     * This method indicates the end of a component invocation.
     *
     * @param type The component type
     * @param operation The operation
     * @param uri The uri
     * @param headers The header values
     * @param values The response values
     */
    void componentEnd(String type, String operation, String uri, Map<String,?> headers, Object... values);

    /**
     * This method indicates the start of a message being produced.
     *
     * @param type The endpoint type
     * @param uri The uri
     * @param id The unique interaction id
     * @param headers The header values
     * @param values The request values
     */
    void producerStart(String type, String uri, String id, Map<String,?> headers, Object... values);

    /**
     * This method indicates the end of a message being produced.
     *
     * @param type The endpoint type
     * @param uri The uri
     * @param headers The header values
     * @param values The response values
     */
    void producerEnd(String type, String uri, Map<String,?> headers, Object... values);

}
