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

import org.hawkular.apm.api.model.trace.Trace;

/**
 * This interface represents the service used to store and retrieve traces.
 *
 * @author gbrown
 */
public interface TraceService {

    /**
     * This method returns the trace fragment associated with the
     * supplied id.
     *
     * @param tenantId The tenant
     * @param id The id
      * @return The trace fragment, or null if not found
     */
    Trace getFragment(String tenantId, String id);

    /**
     * This method returns the end to end trace associated with the
     * supplied id.
     *
     * @param tenantId The tenant
     * @param id The id
      * @return The end to end trace, or null if not found
     */
    Trace getTrace(String tenantId, String id);

    /**
     * This method returns a set of trace fragments that meet the
     * supplied query criteria.
     *
     * @param tenantId The tenant
     * @param criteria The query criteria
     * @return The list of trace fragments that meet the criteria
     */
    List<Trace> searchFragments(String tenantId, Criteria criteria);

    /**
     * This method stores the supplied list of trace fragments.
     *
     * @param tenantId The tenant id
     * @param traces The traces
     * @throws StoreException Failed to store
     */
    void storeFragments(String tenantId, List<Trace> traces) throws StoreException;

    /**
     * This method clears the trace data for the supplied tenant.
     *
     * @param tenantId The tenant id
     */
    void clear(String tenantId);

}
