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
import java.util.logging.Logger;

import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.client.api.reporter.TraceReporter;

import io.opentracing.propagation.Format;

/**
 * The opentracing compatible Tracer implementation for Hawkular APM.
 *
 * @author gbrown
 */
public class APMTracer extends AbstractTracer {

    private static final Logger log = Logger.getLogger(APMTracer.class.getName());

    /** This constant represents the prefix used by all Hawkular APM state. */
    public static final String HAWKULAR_APM_PREFIX = "Hawkular-APM";

    /** This constant represents the interaction id exchanges between a sender and receiver. */
    public static final String HAWKULAR_APM_ID = HAWKULAR_APM_PREFIX + "-Id";

    /** This constant represents the transaction name. */
    public static final String HAWKULAR_BT_NAME = HAWKULAR_APM_PREFIX + "-BTxn";

    /** This constant represents the reporting level. */
    public static final String HAWKULAR_APM_LEVEL = HAWKULAR_APM_PREFIX + "-Level";

    /** Tag name used to represent the business transaction name */
    public static final String TRANSACTION_NAME = "transaction.name";

    private TraceReporter reporter;

    public APMTracer(TraceReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    APMSpanBuilder createSpanBuilder(String operationName) {
        return new APMSpanBuilder(operationName, reporter);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (spanContext instanceof APMSpan) {
            ((APMSpan) spanContext).setInteractionId(UUID.randomUUID().toString(), NodeType.Producer);
        }
        super.inject(spanContext, format, carrier);
    }

    @Override
    Map<String, Object> getTraceState(SpanContext spanContext) {
        Map<String, Object> ret = new HashMap<String, Object>();

        if (spanContext instanceof APMSpan) {
            APMSpan span = (APMSpan) spanContext;
            if (span.getInteractionId() != null) {
                ret.put(APMTracer.HAWKULAR_APM_ID, span.getInteractionId());
            } else {
                // Not sure if issue - but just logging as warning for now
                log.warning("No id available to include in trace state for context = " + spanContext);
            }

            // Check if the transaction name has not currently been set, but
            // has been defined in the span tags - if so copy the value to the trace
            // context so that it can be propagated to invoked services
            if (span.getTraceContext().getBusinessTransaction() == null
                    && span.getTags().containsKey(TRANSACTION_NAME)) {
                span.getTraceContext().setBusinessTransaction(span.getTags().get(TRANSACTION_NAME).toString());
            }

            // If transaction name defined on trace context, then propagate it
            if (span.getTraceContext().getBusinessTransaction() != null) {
                ret.put(APMTracer.HAWKULAR_BT_NAME, span.getTraceContext().getBusinessTransaction());
            }

            if (span.getTraceContext().getReportingLevel() != null) {
                ret.put(APMTracer.HAWKULAR_APM_LEVEL, span.getTraceContext().getReportingLevel());
            }
        }

        return ret;
    }
}
