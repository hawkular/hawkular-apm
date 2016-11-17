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
package io.opentracing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.client.api.reporter.BatchTraceReporter;
import org.hawkular.apm.client.api.reporter.TraceReporter;
import org.hawkular.apm.client.opentracing.APMTracer;

import io.opentracing.propagation.Format;

/**
 * @author gbrown
 */
public abstract class AbstractAPMTracer extends AbstractTracer {

    private static final Logger log = Logger.getLogger(APMTracer.class.getName());

    private TraceReporter reporter;

    public AbstractAPMTracer() {
        this.reporter = new BatchTraceReporter();
    }

    public AbstractAPMTracer(TraceReporter reporter) {
        this.reporter = reporter;
    }
    
    public void setTraceReporter(TraceReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    APMSpanBuilder createSpanBuilder(String operationName) {
        return new APMSpanBuilder(operationName, reporter);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (spanContext instanceof APMSpan) {
            ((APMSpan) spanContext).setInteractionId(UUID.randomUUID().toString());
            ((APMSpan) spanContext).getNodeBuilder().setNodeType(NodeType.Producer);
        }
        super.inject(spanContext, format, carrier);
    }

    @Override
    Map<String, Object> getTraceState(SpanContext spanContext) {
        Map<String, Object> ret = new HashMap<String, Object>();

        if (spanContext instanceof APMSpan) {
            APMSpan span = (APMSpan) spanContext;
            if (span.getInteractionId() != null) {
                ret.put(Constants.HAWKULAR_APM_ID, span.getInteractionId());
            } else {
                // Not sure if issue - but just logging as warning for now
                log.warning("No id available to include in trace state for context = " + spanContext);
            }

            ret.put(Constants.HAWKULAR_APM_TRACEID, span.getTraceContext().getTraceId());

            // Check if the transaction name has not currently been set, but
            // has been defined in the span tags - if so copy the value to the trace
            // context so that it can be propagated to invoked services
            if (span.getTraceContext().getTransaction() == null
                    && span.getTags().containsKey(Constants.PROP_TRANSACTION_NAME)) {
                span.getTraceContext().setTransaction(span.getTags().get(Constants.PROP_TRANSACTION_NAME).toString());
            }

            // If transaction name defined on trace context, then propagate it
            if (span.getTraceContext().getTransaction() != null) {
                ret.put(Constants.HAWKULAR_APM_TXN, span.getTraceContext().getTransaction());
            }

            if (span.getTraceContext().getReportingLevel() != null) {
                ret.put(Constants.HAWKULAR_APM_LEVEL, span.getTraceContext().getReportingLevel());
            }
        }

        return ret;
    }

}
