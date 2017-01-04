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
import java.util.Set;
import java.util.function.Function;

import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;

/**
 * @author Pavol Loffay
 */
public interface SpanCache extends Cache<Span> {

    /**
     * Get all children of a given span.
     *
     * @param tenant the tenant
     * @param id Id of the span.
     * @return Children spans of a given span.
     */
    Set<Span> getChildren(String tenant, String id);

    /**
     * Get all spans which belongs to the trace.
     *
     * @param tenant the tenant
     * @param id id of the trace
     * @return spans which belongs to a given trace, null if trace does not exists.
     */
    Set<Span> getTrace(String tenant, String id);

    /**
     * Stores spans into cache
     *
     * @param tenantId The tenant
     * @param spans The spans
     * @param cacheKeyEntrySupplier Function to generate unique id of the span
     *          @see {@link SpanUniqueIdGenerator}. This is used as key
     *          entry in the cache.
     * @throws CacheException
     */
    void store(String tenantId, List<Span> spans, Function<Span, String> cacheKeyEntrySupplier) throws CacheException;
}
