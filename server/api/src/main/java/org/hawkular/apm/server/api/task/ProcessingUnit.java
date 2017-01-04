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
package org.hawkular.apm.server.api.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.server.api.task.Processor.ProcessorType;

/**
 * This class provides a processing unit for processing a batch of
 * items against a defined processor, and managing the results and/or
 * retries.
 *
 * @author gbrown
 */
public class ProcessingUnit<T, R> implements Handler<T> {

    private static final Logger perfLog = Logger.getLogger("org.hawkular.apm.performance");

    private Processor<T, R> processor;

    private int retryCount;
    private String retrySubscriber;

    private Handler<R> resultHandler;
    private Handler<T> retryHandler;

    /**
     * @return the processor
     */
    public Processor<T, R> getProcessor() {
        return processor;
    }

    /**
     * @param processor the processor to set
     */
    public void setProcessor(Processor<T, R> processor) {
        this.processor = processor;
    }

    /**
     * @return the retryCount
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * @param retryCount the retryCount to set
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * @return the retrySubscriber
     */
    public String getRetrySubscriber() {
        return retrySubscriber;
    }

    /**
     * @param retrySubscriber the retrySubscriber to set
     */
    public void setRetrySubscriber(String retrySubscriber) {
        this.retrySubscriber = retrySubscriber;
    }

    /**
     * @return the resultHandler
     */
    public Handler<R> getResultHandler() {
        return resultHandler;
    }

    /**
     * @param resultHandler the resultHandler to set
     */
    public void setResultHandler(Handler<R> resultHandler) {
        this.resultHandler = resultHandler;
    }

    /**
     * @return the retryHandler
     */
    public Handler<T> getRetryHandler() {
        return retryHandler;
    }

    /**
     * @param retryHandler the retryHandler to set
     */
    public void setRetryHandler(Handler<T> retryHandler) {
        this.retryHandler = retryHandler;
    }

    @Override
    public void handle(String tenantId, List<T> items) throws Exception {
        List<R> results = null;
        List<T> retries = null;
        RetryAttemptException lastException = null;

        try {
            processor.initialise(tenantId, items);

            // If performance logging enabled, save the current time
            long startTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                startTime = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
            }

            if (processor.getType() == ProcessorType.ManyToMany) {
                results = processor.processManyToMany(tenantId, items);

            } else {
                for (int i = 0; i < items.size(); i++) {
                    try {
                        if (processor.getType() == ProcessorType.OneToMany) {
                            List<R> result = processor.processOneToMany(tenantId, items.get(i));
                            if (resultHandler != null && result != null && !result.isEmpty()) {
                                if (results == null) {
                                    results = new ArrayList<R>();
                                }
                                results.addAll(result);
                            }
                        } else {
                            R result = processor.processOneToOne(tenantId, items.get(i));
                            if (resultHandler != null && result != null) {
                                if (results == null) {
                                    results = new ArrayList<R>();
                                }
                                results.add(result);
                            }
                        }
                    } catch (RetryAttemptException e) {
                        if (retryHandler != null) {
                            if (retries == null) {
                                retries = new ArrayList<T>();
                            }
                            retries.add(items.get(i));
                            lastException = e;
                        }
                    }
                }
            }

            // If performance logging enabled, log the duration associated with the event processing
            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: invoked processor ["+processor.getClass().getSimpleName()+"] duration=" +
                        (TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) - startTime) + "microseconds");
            }

        } catch (RetryAttemptException e) {
            retries = items;
            lastException = e;
        }

        processor.cleanup(tenantId, items);

        if (results != null && !results.isEmpty()) {
            resultHandler.handle(tenantId, results);
        }

        if (retries != null && !retries.isEmpty()) {
            if (getRetryCount() > 0) {
                retryHandler.handle(tenantId, retries);
            } else {
                throw lastException;
            }
        }
    }

}
