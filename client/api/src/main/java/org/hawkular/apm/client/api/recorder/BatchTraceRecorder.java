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

package org.hawkular.apm.client.api.recorder;

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
public class BatchTraceRecorder implements TraceRecorder {
    private static final Logger log = Logger.getLogger(BatchTraceRecorder.class.getName());

    private static final int DEFAULT_BATCH_THREAD_POOL_SIZE = 5;
    private static final int DEFAULT_BATCH_TIME = 500;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String HAWKULAR_APM_TENANT_ID = "HAWKULAR_APM_TENANTID";

    private TracePublisher tracePublisher;

    private List<Trace> traces;
    private int batchSize;
    private String tenantId;

    private ExecutorService executor;
    private final ReentrantLock lock = new ReentrantLock();

    public BatchTraceRecorder() {
        this(BatchTraceRecorderBuilder.fromEnvProperties());
    }

    public BatchTraceRecorder(BatchTraceRecorderBuilder recorderBuilder) {
        init(recorderBuilder);
    }

    protected void init(BatchTraceRecorderBuilder builder) {
        this.tracePublisher = builder.tracePublisher;
        this.tenantId = builder.tenantId;
        this.batchSize = builder.batchSize;
        this.traces = new ArrayList<>(batchSize + 1);

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
                // Initial check, to avoid doing too much work if no
                // traces reported
                if (!traces.isEmpty()) {
                    try {
                        lock.lock();
                        submitTraces();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }, builder.batchTime, builder.batchTime, TimeUnit.MILLISECONDS);

        executor = Executors.newFixedThreadPool(builder.threadPoolSize,
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = Executors.defaultThreadFactory().newThread(r);
                        t.setDaemon(true);
                        return t;
                    }
                });
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

    @Override
    public void record(Trace trace) {
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
            log.warning("Trace publisher is not available!");
        }
    }

    /**
     * This method submits the current list of traces
     */
    protected void submitTraces() {
        if (!traces.isEmpty()) {
            // Locally store list and create new list for subsequent traces
            List<Trace> toSend = traces;
            traces = new ArrayList<>(batchSize + 1);

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

    public static class BatchTraceRecorderBuilder {
        private int batchSize = DEFAULT_BATCH_SIZE;
        private int batchTime = DEFAULT_BATCH_TIME;
        private int threadPoolSize = DEFAULT_BATCH_THREAD_POOL_SIZE;
        private String tenantId;

        private TracePublisher tracePublisher;

        public BatchTraceRecorderBuilder() {
            tracePublisher = ServiceResolver.getSingletonService(TracePublisher.class);
        }

        public BatchTraceRecorderBuilder withTracePublisher(TracePublisher tracePublisher) {
            this.tracePublisher = tracePublisher;
            return this;
        }

        public BatchTraceRecorderBuilder withBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public BatchTraceRecorderBuilder withBatchTime(int batchTimeMillis) {
            this.batchTime = batchTimeMillis;
            return this;
        }

        public BatchTraceRecorderBuilder withBatchPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public BatchTraceRecorderBuilder withTenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public BatchTraceRecorder build() {
            return new BatchTraceRecorder(this);
        }

        private static BatchTraceRecorderBuilder fromEnvProperties() {
            BatchTraceRecorderBuilder builder = new BatchTraceRecorderBuilder();

            if (PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHSIZE, null) != null) {
                String batchSize = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHSIZE, null);
                builder.withBatchSize(Integer.parseInt(batchSize));
            }
            if (PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTIME, null) != null) {
                String batchTime = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTIME, null);
                builder.withBatchTime(Integer.parseInt(batchTime));
            }

            if (PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTHREADS, null) != null) {
                String threadPoolSize = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_COLLECTOR_BATCHTHREADS, null);
                builder.withBatchPoolSize(Integer.parseInt(threadPoolSize));
            }

            builder.withTenantId(PropertyUtil.getProperty(HAWKULAR_APM_TENANT_ID, null));

            return builder;
        }
    }
}
