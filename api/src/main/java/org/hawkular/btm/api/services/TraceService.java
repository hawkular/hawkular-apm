/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.services;

import java.util.List;

import org.hawkular.btm.api.model.trace.Trace;

/**
 * This interface represents the service used to store and retrieve traces.
 *
 * @author gbrown
 */
public interface TraceService {

    /**
     * This method returns the trace associated with the
     * supplied id.
     *
     * @param tenantId The tenant
     * @param id The id
     * @return The trace, or null if not found
     */
    Trace get(String tenantId, String id);

    /**
     * This method returns a set of traces that meet the
     * supplied query criteria.
     *
     * @param tenantId The tenant
     * @param criteria The query criteria
     * @return The list of traces that meet the criteria
     */
    List<Trace> query(String tenantId, Criteria criteria);

    /**
     * This method stores the supplied list of trace fragments.
     *
     * @param tenantId The tenant id
     * @param traces The traces
     * @throws Exception Failed to store
     */
    void storeTraces(String tenantId, List<Trace> traces) throws Exception;

    /**
     * This method clears the trace data for the supplied tenant.
     *
     * @param tenantId The tenant id
     */
    void clear(String tenantId);

}
