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
package org.hawkular.apm.trace.service.rest.client;

import java.util.List;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.rest.AbstractRESTClient;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * This class provides the REST client implementation for the Trace Service
 * API.
 *
 * @author gbrown
 */
public class TraceServiceRESTClient extends AbstractRESTClient implements TraceService {
    private static final TypeReference<List<Trace>> TRACE_LIST =
            new TypeReference<List<Trace>>() {
    };
    private static final TypeReference<Trace> TRACE =
            new TypeReference<Trace>() {
    };

    public TraceServiceRESTClient() {
        super(PropertyUtil.HAWKULAR_APM_URI_SERVICES);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#get(java.lang.String, java.lang.String)
     */
    @Override
    public Trace getFragment(String tenantId, String id) {
        return getResultsForUrl(tenantId, TRACE, "traces/fragments/%s", id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#getTrace(java.lang.String, java.lang.String)
     */
    @Override
    public Trace getTrace(String tenantId, String id) {
        return getResultsForUrl(tenantId, TRACE, "traces/complete/%s", id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#searchFragments(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<Trace> searchFragments(String tenantId, Criteria criteria) {
        String path = "traces/fragments/search?criteria=%s";
        return getResultsForUrl(tenantId, TRACE_LIST, path, criteria);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#storeTraces(java.lang.String,
     *                              java.util.List)
     */
    @Override
    public void storeFragments(String tenantId, List<Trace> traces) throws StoreException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        clear(tenantId, "traces");
    }

}
