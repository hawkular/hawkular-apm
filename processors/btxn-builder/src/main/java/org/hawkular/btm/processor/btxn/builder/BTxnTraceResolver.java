/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.processor.btxn.builder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;
import org.hawkular.btm.server.api.processors.BusinessTransactionTraceHandler;
import org.jboss.logging.Logger;

/**
 * This class is responsible for initiating the build of an end to end trace,
 * from individual business transaction fragments, to ultimately derive relevant
 * metrics.
 *
 * @author gbrown
 */
public class BTxnTraceResolver implements BusinessTransactionTraceHandler {

    private static final Logger log = Logger.getLogger(BTxnTraceResolver.class);

    private BTxnTraceProcessor traceProcessor = new BTxnTraceProcessor();

    @Inject
    private BTxnTraceScheduler scheduler;

    private BusinessTransactionTraceHandler retryHandler;

    /**
     * @return the retryHandler
     */
    public BusinessTransactionTraceHandler getRetryHandler() {
        return retryHandler;
    }

    /**
     * @param retryHandler the retryHandler to set
     */
    public void setRetryHandler(BusinessTransactionTraceHandler retryHandler) {
        this.retryHandler = retryHandler;
    }

    /**
     * @return the scheduler
     */
    public BTxnTraceScheduler getScheduler() {
        return scheduler;
    }

    /**
     * @param scheduler the scheduler to set
     */
    public void setScheduler(BTxnTraceScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * @return the traceProcessor
     */
    public BTxnTraceProcessor getTraceProcessor() {
        return traceProcessor;
    }

    /**
     * @param traceProcessor the traceProcessor to set
     */
    public void setTraceProcessor(BTxnTraceProcessor traceProcessor) {
        this.traceProcessor = traceProcessor;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.processors.BusinessTransactionTraceHandler#handle(java.lang.String,java.util.List)
     */
    @Override
    public void handle(String tenantId, List<BusinessTransactionTrace> traces) {
        log.tracef("Trace Resolver called with: %s", traces);

        List<BusinessTransactionTrace> retry = null;
        List<BusinessTransactionTrace> resolveTraces = null;

        for (int i = 0; i < traces.size(); i++) {
            if (!traces.get(i).isComplete()) {
                try {
                    BusinessTransactionTrace trace = traceProcessor.process(traces.get(i));

                    if (trace != null) {
                        if (resolveTraces == null) {
                            resolveTraces = new ArrayList<BusinessTransactionTrace>();
                        }
                        resolveTraces.add(trace);
                    }
                } catch (Exception e) {
                    log.debug("Failed to process trace", e);

                    if (retry == null) {
                        retry = new ArrayList<BusinessTransactionTrace>();
                    }
                    retry.add(traces.get(i));
                }
            }
        }

        if (retry != null && getRetryHandler() != null) {
            log.tracef("Calling retry handler with: %d traces", retry.size());
            getRetryHandler().handle(tenantId, retry);
        }

        if (resolveTraces != null && !resolveTraces.isEmpty() && getScheduler() != null) {
            // TODO: Need to segment traces into time slots and call the
            // scheduler for each slot
            long interval = traceProcessor.getScheduleInterval(resolveTraces.get(0));
            getScheduler().schedule(tenantId, resolveTraces, interval);
        }
    }

}
