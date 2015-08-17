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
package org.hawkular.btm.bus.inmemory;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;
import org.hawkular.btm.server.api.processors.BusinessTransactionBus;
import org.hawkular.btm.server.api.processors.BusinessTransactionFragmentHandler;
import org.hawkular.btm.server.api.processors.BusinessTransactionTraceHandler;
import org.jboss.logging.Logger;

/**
 * This class provides an in-memory implementation of the business transaction bus.
 *
 * @author gbrown
 */
public class BusinessTransactionBusInMemory implements BusinessTransactionBus {

    private final Logger log = Logger.getLogger(BusinessTransactionBusInMemory.class);

    private List<BusinessTransactionFragmentHandler> fragmentHandlers =
            new ArrayList<BusinessTransactionFragmentHandler>();

    private List<BusinessTransactionTraceHandler> traceHandlers =
            new ArrayList<BusinessTransactionTraceHandler>();

    @Resource
    private ManagedExecutorService executorService;

    /**
     * @return the fragmentHandlers
     */
    public List<BusinessTransactionFragmentHandler> getBusinessTransactionFragmentHandlers() {
        return fragmentHandlers;
    }

    /**
     * @param fragmentHandlers the fragmentHandlers to set
     */
    public void setBusinessTransactionFragmentHandlers(List<BusinessTransactionFragmentHandler> fragmentHandlers) {
        this.fragmentHandlers = fragmentHandlers;
    }

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

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.processors.BusinessTransactionBus#process(java.lang.String, java.util.List)
     */
    @Override
    public void processFragments(String tenantId, List<BusinessTransaction> btxns) {

        if (fragmentHandlers.size() > 0) {
            log.tracef("Distribute business transaction fragments to " + fragmentHandlers.size() +
                    " handlers: " + fragmentHandlers + " (with executor=" + executorService + ")");

            // Process business transaction fragments
            for (int i = 0; i < fragmentHandlers.size(); i++) {
                if (executorService == null) {
                    fragmentHandlers.get(i).handle(tenantId, btxns, null);
                } else {
                    executorService.execute(new BTxnFragmentHandlerTask(tenantId, fragmentHandlers.get(i), btxns));
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.processors.BusinessTransactionBus#processTraces(java.lang.String,
     *                  java.util.List)
     */
    @Override
    public void processTraces(String tenantId, List<BusinessTransactionTrace> traces) {

        if (traceHandlers.size() > 0) {
            log.tracef("Distribute business transaction traces to " + traceHandlers.size() +
                    " handlers: " + traceHandlers + " (with executor=" + executorService + ")");

            // Process business transaction traces
            for (int i = 0; i < traceHandlers.size(); i++) {
                if (executorService == null) {
                    traceHandlers.get(i).handle(tenantId, traces, null);
                } else {
                    executorService.execute(new BTxnTraceHandlerTask(tenantId, traceHandlers.get(i), traces));
                }
            }
        }
    }

    /**
     * This task processes a list of business transaction fragments using a provided
     * handler.
     *
     * @author gbrown
     */
    private static class BTxnFragmentHandlerTask implements Runnable {

        private String tenantId;
        private BusinessTransactionFragmentHandler handler;
        private List<BusinessTransaction> businessTransactions;

        public BTxnFragmentHandlerTask(String tenantId, BusinessTransactionFragmentHandler handler,
                List<BusinessTransaction> btxns) {
            this.tenantId = tenantId;
            this.handler = handler;
            this.businessTransactions = btxns;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            handler.handle(tenantId, businessTransactions, null);
        }

    }

    /**
     * This task processes a list of business transaction traces using a provided
     * handler.
     *
     * @author gbrown
     */
    private static class BTxnTraceHandlerTask implements Runnable {

        private String tenantId;
        private BusinessTransactionTraceHandler handler;
        private List<BusinessTransactionTrace> traces;

        public BTxnTraceHandlerTask(String tenantId, BusinessTransactionTraceHandler handler,
                List<BusinessTransactionTrace> traces) {
            this.tenantId = tenantId;
            this.handler = handler;
            this.traces = traces;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            handler.handle(tenantId, traces, null);
        }

    }

}
