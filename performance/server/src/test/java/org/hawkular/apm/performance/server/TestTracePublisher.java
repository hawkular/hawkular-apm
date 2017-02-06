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
package org.hawkular.apm.performance.server;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.PublisherMetricHandler;
import org.hawkular.apm.api.services.TracePublisher;

/**
 * @author gbrown
 */
public class TestTracePublisher implements TracePublisher {

    private List<Trace> traces = new ArrayList<Trace>();

    public List<Trace> getTraces() {
        return traces;
    }

    @Override
    public int getInitialRetryCount() {
        return 0;
    }

    @Override
    public void publish(String tenantId, List<Trace> items) throws Exception {
        traces.addAll(items);
    }

    @Override
    public void publish(String tenantId, List<Trace> items, int retryCount, long delay) throws Exception {
        traces.addAll(items);
    }

    @Override
    public void retry(String tenantId, List<Trace> items, String subscriber, int retryCount, long delay)
            throws Exception {
    }

    @Override
    public void setMetricHandler(PublisherMetricHandler<Trace> handler) {
    }

}
