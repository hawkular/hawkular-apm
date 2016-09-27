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
package org.hawkular.apm.client.api.reporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * This class is responsible for managing a set of traces and
 * reporting them to the server.
 *
 * @author gbrown
 */
public class BatchTraceReporter implements TraceReporter {

    /**  */
    private static final int DEFAULT_BATCH_THREAD_POOL_SIZE = 5;

    /**  */
    private static final String HAWKULAR_APM_TENANT_ID = "HAWKULAR_APM_TENANTID";

    private static final Logger log = Logger.getLogger(BatchTraceReporter.class.getName());

    /**  */
    private static final int DEFAULT_BATCH_TIME = 500;

    /**  */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private TracePublisher tracePublisher;

    private int batchSize = DEFAULT_BATCH_SIZE;
    private int batchTime = DEFAULT_BATCH_TIME;

    private String tenantId = PropertyUtil.getProperty(HAWKULAR_APM_TENANT_ID);

    private ExecutorService executor;
    private final ReentrantLock lock=new ReentrantLock();
    private List<Trace> traces = new ArrayList<Trace>();

    {
        executor = Executors.newFixedThreadPool(DEFAULT_BATCH_THREAD_POOL_SIZE,
                                new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });

        setTracePublisher(ServiceResolver.getSingletonService(TracePublisher.class));
    }

    /**
     * The default constructor.
     */
    public BatchTraceReporter() {
        init();
    }

    /**
     * This method sets the trace publisher.
     *
     * @param tp The trace publisher
     */
    public void setTracePublisher(TracePublisher tp) {
        this.tracePublisher = tp;
    }

    /**
     * @return the trace publisher
     */
    public TracePublisher getTracePublisher() {
        return tracePublisher;
    }

    /**
     * This method initialises the reporter.
     */
    protected void init() {
        // Get properties
        String size = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHSIZE, null);
        if (size != null) {
            batchSize = Integer.parseInt(size);
        }

        String time = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTIME, null);
        if (time != null) {
            batchTime = Integer.parseInt(time);
        }

        tenantId = PropertyUtil.getProperty(HAWKULAR_APM_TENANT_ID, null);

        String pool = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTHREADS, null);
        if (pool != null) {
            executor = Executors.newFixedThreadPool(Integer.parseInt(pool));
        }

        // Create scheduled task
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        }).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // Initial check, to avoid doing too much work if no business
                // transactions reported
                if (!traces.isEmpty()) {
                    try {
                        lock.lock();
                        submitTraces();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }, batchTime, batchTime, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return tracePublisher != null;
    }

    /**
     * @return the tenantId
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId the tenantId to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.client.api.TraceReporter#report(org.hawkular.apm.api.model.trace.Trace)
     */
    @Override
    public void report(Trace trace) {
        if (tracePublisher != null) {
            try {
                lock.lock();
                traces.add(trace);

                if (traces.size() >= batchSize) {
                    submitTraces();
                }
            } finally {
                lock.unlock();
            }
        } else {
            log.warning("Trace service is not available!");
        }
    }

    /**
     * This method submits the current list of traces
     */
    protected void submitTraces() {
        if (!traces.isEmpty()) {
            // Locally store list and create new list for subsequent traces
            List<Trace> toSend=traces;
            traces = new ArrayList<Trace>();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracePublisher.publish(tenantId, toSend);
                    } catch (Exception e) {
                        // TODO: Retain for retry
                        log.log(Level.SEVERE, "Failed to publish traces", e);
                    }
                }
            });
        }
    }

}
