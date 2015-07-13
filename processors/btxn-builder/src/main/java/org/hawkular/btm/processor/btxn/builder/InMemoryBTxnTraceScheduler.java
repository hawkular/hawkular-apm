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
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Provider;

import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;
import org.hawkular.btm.server.api.processors.BusinessTransactionTraceHandler;
import org.jboss.logging.Logger;

/**
 * This class is an in-memory implementation of the trace scheduler.
 *
 * @author gbrown
 */
public class InMemoryBTxnTraceScheduler implements BTxnTraceScheduler {

    private final Logger log = Logger.getLogger(InMemoryBTxnTraceScheduler.class);

    private List<BusinessTransactionTraceHandler> traceHandlers =
            new ArrayList<BusinessTransactionTraceHandler>();

    @Inject
    private Provider<List<BusinessTransactionTraceHandler>> handlers;

    @Resource
    private ManagedExecutorService executorService;

    @Resource
    private ManagedScheduledExecutorService scheduledExecutorService;

    private boolean initialised = false;

    protected void init() {
        // Unfortunate workaround due to circular dependency
        traceHandlers = handlers.get();

        log.debugf("Setup the trace handlers");

        initialised = true;
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
     * @see org.hawkular.btm.processor.btxn.builder.BTxnTraceScheduler#schedule(java.util.List, long)
     */
    @Override
    public void schedule(String tenantId, List<BusinessTransactionTrace> traces, long timeValue) {
        if (!initialised) {
            init();
        }
        scheduledExecutorService.schedule(new BTxnTraceSchedulerTask(tenantId, traces), timeValue,
                TimeUnit.MILLISECONDS);
    }

    /**
     * This task processes a list of business transaction fragments using a provided
     * handler.
     *
     * @author gbrown
     */
    private class BTxnTraceSchedulerTask implements Runnable {

        private String tenantId;
        private List<BusinessTransactionTrace> traces;

        public BTxnTraceSchedulerTask(String tenantId, List<BusinessTransactionTrace> traces) {
            this.tenantId = tenantId;
            this.traces = traces;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            for (int i = 0; i < traceHandlers.size(); i++) {
                executorService.execute(new BTxnTraceHandlerTask(tenantId, traceHandlers.get(i), traces));
            }
        }

    }

    /**
     * This task processes a list of business transaction fragments using a provided
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
            handler.handle(tenantId, traces);
        }

    }
}
