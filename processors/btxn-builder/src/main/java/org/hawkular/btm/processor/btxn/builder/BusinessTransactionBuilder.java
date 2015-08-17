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

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;
import org.hawkular.btm.server.api.processors.BusinessTransactionFragmentHandler;
import org.hawkular.btm.server.api.processors.BusinessTransactionTraceHandler;
import org.hawkular.btm.server.api.processors.RetryHandler;
import org.jboss.logging.Logger;

/**
 * This class is responsible for initiating the build of an end to end trace,
 * from individual business transaction fragments, to ultimately derive relevant
 * metrics.
 *
 * @author gbrown
 */
public class BusinessTransactionBuilder implements BusinessTransactionFragmentHandler {

    private static final Logger log = Logger.getLogger(BusinessTransactionBuilder.class);

    @Inject
    private BTxnTraceProcessor traceProcessor;

    @Inject
    private List<BusinessTransactionTraceHandler> traceHandlers =
            new ArrayList<BusinessTransactionTraceHandler>();

    /**
     * @return the traceHandlers
     */
    public List<BusinessTransactionTraceHandler> getTraceHandlers() {
        return traceHandlers;
    }

    /**
     * @param traceHandlers the traceHandlers to set
     */
    public void setTraceHandlers(List<BusinessTransactionTraceHandler> traceHandlers) {
        this.traceHandlers = traceHandlers;
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
     * @see org.hawkular.btm.api.processors.BusinessTransactionProcessor#handle(java.lang.String,
     *                  org.hawkular.btm.api.model.btxn.BusinessTransaction)
     */
    @Override
    public void handle(String tenantId, List<BusinessTransaction> btxns,
            RetryHandler<BusinessTransaction> retryHandler) {
        log.tracef("Business Transaction Builder called with: %s", btxns);

        List<BusinessTransaction> retry = null;
        List<BusinessTransactionTrace> traces = null;

        for (int i = 0; i < btxns.size(); i++) {
            try {
                BusinessTransactionTrace trace = traceProcessor.init(btxns.get(i));

                if (trace != null) {
                    if (traces == null) {
                        traces = new ArrayList<BusinessTransactionTrace>();
                    }
                    traces.add(trace);
                }
            } catch (Exception e) {
                log.debug("Failed to process trace", e);

                if (retry == null) {
                    retry = new ArrayList<BusinessTransaction>();
                }
                retry.add(btxns.get(i));
            }
        }

        if (retry != null && retryHandler != null) {
            log.tracef("Retrying %d traces", retry.size());
            retryHandler.retry(retry);
        }

        if (traces != null && getTraceHandlers() != null) {
            for (int i = 0; i < getTraceHandlers().size(); i++) {
                getTraceHandlers().get(i).handle(tenantId, traces, null);
            }
        }
    }

}
