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

import java.util.Map;

import org.hawkular.apm.api.model.config.ReportingLevel;

import io.opentracing.SpanContext;

/**
 * @author Pavol Loffay
 */
public class ExtractedContext implements SpanContext {

    private String traceId;
    private String correlationId;
    private String transaction;
    private ReportingLevel reportingLevel;
    private Map<String, String> baggage;

    public ExtractedContext(Map<String, String> baggage,
                            String traceId, String correlationId,
                            String transaction, ReportingLevel reportingLevel) {

        this.traceId = traceId;
        this.correlationId = correlationId;
        this.transaction = transaction;
        this.reportingLevel = reportingLevel;
        this.baggage = baggage;
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return baggage.entrySet();
    }

    String getCorrelationId() {
        return correlationId;
    }

    String getTraceId() {
        return traceId;
    }

    String getTransaction() {
        return transaction;
    }

    ReportingLevel getReportingLevel() {
        return reportingLevel;
    }
}
