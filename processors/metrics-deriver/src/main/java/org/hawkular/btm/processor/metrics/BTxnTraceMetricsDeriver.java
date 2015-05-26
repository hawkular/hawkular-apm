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
package org.hawkular.btm.processor.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;
import org.hawkular.btm.api.processors.BusinessTransactionTraceHandler;
import org.hawkular.btm.processor.metrics.log.MsgLogger;
import org.jboss.logging.Logger;

/**
 * This class processes business transaction traces to derive metrics of interest.
 *
 * @author gbrown
 */
public class BTxnTraceMetricsDeriver implements BusinessTransactionTraceHandler {

    private final Logger log = Logger.getLogger(BTxnTraceMetricsDeriver.class);

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    @Inject
    private Instance<MetricsService> injectedMetricsService;

    private MetricsService metricsService;

    private BusinessTransactionTraceHandler retryHandler;

    @PostConstruct
    public void init() {
        if (injectedMetricsService.isUnsatisfied()) {
            msgLog.warnNoMetricsService();
        } else {
            metricsService = injectedMetricsService.get();
        }
    }

    /**
     * @return the metricsService
     */
    public MetricsService getMetricsService() {
        return metricsService;
    }

    /**
     * @param metricsService the metricsService to set
     */
    public void setMetricsService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

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

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.processors.BusinessTransactionTraceHandler#handle(java.util.List)
     */
    @Override
    public void handle(List<BusinessTransactionTrace> traces) {
        log.tracef("Metrics Deriver called with: %s", traces);

        List<BusinessTransactionTrace> retry = null;
        Set<BTxnMetric> metrics = null;

        for (int i = 0; i < traces.size(); i++) {
            if (traces.get(i).isComplete()) {
                try {
                    List<BTxnMetric> txnMetrics = derive(traces.get(i));

                    if (txnMetrics != null && !txnMetrics.isEmpty()) {
                        if (metrics == null) {
                            metrics = new HashSet<BTxnMetric>();
                        }
                        metrics.addAll(txnMetrics);
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
            log.tracef("Retry %d traces", retry.size());
            getRetryHandler().handle(retry);
        }

        if (metrics != null && getMetricsService() != null) {
            try {
                getMetricsService().report(metrics);
            } catch (Exception e) {
                // TODO: Error and decide how to handle metrics service failure - do
                // a complete retry, but report error if no retry handler?
                e.printStackTrace();
            }
        }
    }

    /**
     * This method derives the metrics from the supplied trace.
     *
     * @param trace The trace
     * @return The derived metrics
     */
    protected List<BTxnMetric> derive(BusinessTransactionTrace trace) {
        return Collections.emptyList();
    }
}
