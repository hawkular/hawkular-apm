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
package org.hawkular.apm.client.opentracing.refactor;

import java.util.Collections;
import java.util.Map;

import org.hawkular.apm.api.model.config.ReportingLevel;

import io.opentracing.SpanContext;

/**
 * @author Pavol Loffay
 */
public class HawkularSpanContext implements SpanContext {

    private final Map<String, String> baggage;

    /**
     * APM specific
     */
    private final NodeBuilder nodeBuilder;
    private final TraceFragmentState traceFragmentState;

    HawkularSpanContext(Map<String, String> baggage, NodeBuilder nodeBuilder, TraceFragmentState traceFragmentState) {
        this.baggage = Collections.synchronizedMap(baggage);
        this.nodeBuilder = nodeBuilder;
        this.traceFragmentState = traceFragmentState;
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return baggage.entrySet();
    }

    String getBaggageItem(String key) {
        return baggage.get(key);
    }

    void setBaggageItem(String key, String value) {
        baggage.put(key, value);
    }

    TraceFragmentState traceFragmentState() {
        return traceFragmentState;
    }

    NodeBuilder nodeBuilter() {
        return nodeBuilder;
    }

    String traceId() {
        return traceFragmentState.getTraceId();
    }

    String transaction() {
        return traceFragmentState.getTransaction();
    }

    ReportingLevel reportingLevel() {
        return traceFragmentState.getReportingLevel();
    }
}
