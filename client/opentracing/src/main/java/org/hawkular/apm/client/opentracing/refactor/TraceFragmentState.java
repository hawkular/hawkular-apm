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

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.recorder.TraceRecorder;

/**
 * @author Pavol Loffay
 */
public class TraceFragmentState {

    private TraceRecorder traceRecorder;

    public final NodeBuilder rootBuilder;
    private HawkularSpan rootSpan;

    private String traceId;
    private String fragmentId;
    private String namedTransaction;
    private ReportingLevel reportingLevel;

    AtomicInteger nodeCounter = new AtomicInteger(0);

    static TraceFragmentState create(TraceRecorder traceRecorder) {
        String id = UUID.randomUUID().toString();
        return new TraceFragmentState(traceRecorder, id, id, null, ReportingLevel.All, null, false);
    }

    static TraceFragmentState createFromExtracted(TraceRecorder traceRecorder,
                                                         ExtractedContext extractedContext) {
        return new TraceFragmentState(traceRecorder, extractedContext.getTraceId(), UUID.randomUUID().toString(),
                extractedContext.getTransaction(), extractedContext.getReportingLevel(), null, false);
    }

    static TraceFragmentState createFollowsFrom(TraceRecorder traceRecorder,
                                                       TraceFragmentState traceFragmentState) {
        return new TraceFragmentState(traceRecorder, traceFragmentState.traceId, UUID.randomUUID().toString(),
                traceFragmentState.namedTransaction, traceFragmentState.reportingLevel, traceFragmentState.rootSpan,
                true);
    }

    TraceFragmentState(TraceRecorder traceRecorder, String traceId, String fragmentId, String transaction,
                              ReportingLevel reportingLevel,
                              HawkularSpan rootSpan, boolean dummyInitialNode) {
        this.traceRecorder = traceRecorder;
        this.traceId = traceId;
        this.fragmentId = fragmentId;
        this.namedTransaction = transaction;
        this.reportingLevel = reportingLevel;
        this.rootSpan = rootSpan;

        this.rootBuilder = new NodeBuilder(this);
        if (!dummyInitialNode) {
            this.nodeCounter.incrementAndGet();
        }
    }


    public void finish() {
        if (nodeCounter.decrementAndGet() == 0) {
            Trace trace = new Trace();

            trace.setTraceId(traceId);
            trace.setFragmentId(fragmentId);
            trace.setNodes(Arrays.asList(rootBuilder.build()));
            trace.setTransaction(namedTransaction);
            trace.setTimestamp(rootSpan.startMicros());
            trace.setHostName(PropertyUtil.getHostName());
            trace.setHostAddress(PropertyUtil.getHostAddress());
            traceRecorder.record(trace);
        }
    }

    HawkularSpan rootSpan() {
        return rootSpan;
    }

    void setRootSpan(HawkularSpan rootSpan) {
        if (this.rootSpan == null) {
            this.rootSpan = rootSpan;
        }
    }

    NodeBuilder getRootBuilder() {
        return rootBuilder;
    }

    String getTraceId() {
        return traceId;
    }

    String getFragmentId() {
        return fragmentId;
    }

    void setNamedTransaction(String namedTransaction) {
        this.namedTransaction = namedTransaction;
    }

    String getTransaction() {
        return namedTransaction;
    }

    ReportingLevel getReportingLevel() {
        return reportingLevel;
    }
}
