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
import java.util.function.Function;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.model.zipkin.Span;

/**
 * This interface represents the service used to store and retrieve spans.
 *
 * @author Pavol Loffay
 */
public interface SpanService {

    /**
     * This method returns a span of a given id.
     *
     * @param tenantId The tenant
     * @param id The span id
     * @return Span or null when there is no such span.
     * @throws IllegalStateException when failed to deserialize json from a database.
     */
    Span getSpan(String tenantId, String id);

    /**
     * This method returns all children of a given span.
     *
     * @param tenantId The Tenant
     * @param id The span id
     * @return List of spans with parent id equals to given id or an empty list.
     * @throws IllegalStateException when failed to deserialize json from a database.
     */
    List<Span> getChildren(String tenantId, String id);

    /**
     * This method stores the supplied list of spans.
     *
     * @param tenantId The tenant id
     * @param spans The spans
     * @throws StoreException Is thrown when spans could not be serialized to json or stored into database.
     */
    void storeSpan(String tenantId, List<Span> spans) throws StoreException;

    /**
     * This method stores the supplied list of spans.
     *
     * @param tenantId The tenant id
     * @param spans The spans
     * @param spanIdSupplier Function which is used to generate span id for indexing. Therefore this new id should
     *                       be used for following spans querying.
     * @throws StoreException Is thrown when spans could not be serialized to json or stored into database.
     */
    void storeSpan(String tenantId, List<Span> spans, Function<Span, String> spanIdSupplier) throws StoreException;

    /**
     * This method clears the span data for the supplied tenant.
     *
     * @param tenantId The tenant id
     */
    void clear(String tenantId);

    /**
     * This method returns the trace fragment associated with the supplied id.
     *
     * @param tenantId the tenant
     * @param id the span id
     * @return the trace fragment, null if not found
     */
    Trace getTraceFragment(String tenantId, String id);

    /**
     * This method returns the end to end trace associated with the supplied id.
     *
     * @param tenantId the tenant
     * @param id the id
     * @return the end to end trace, or null if not found
     */
    Trace getTrace(String tenantId, String id);
}
