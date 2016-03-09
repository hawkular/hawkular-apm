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
package org.hawkular.btm.server.api.task;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.server.api.log.MsgLogger;

/**
 * This class provides a processing unit for processing a batch of
 * items against a defined processor, and managing the results and/or
 * retries.
 *
 * @author gbrown
 */
public class ProcessingUnit<T, R> implements Handler<T> {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private Processor<T, R> processor;

    private int retryCount;

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

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Handler#handle(java.lang.String,java.util.List)
     */
    @Override
    public void handle(String tenantId, List<T> items) throws Exception {
        List<R> results = null;
        List<T> retries = null;
        Exception lastException = null;

        processor.initialise(tenantId, items);

        for (int i = 0; i < items.size(); i++) {
            try {
                if (processor.isMultiple()) {
                    List<R> result = processor.processMultiple(tenantId, items.get(i));
                    if (resultHandler != null && result != null && result.size() > 0) {
                        if (results == null) {
                            results = new ArrayList<R>();
                        }
                        results.addAll(result);
                    }
                } else {
                    R result = processor.processSingle(tenantId, items.get(i));
                    if (resultHandler != null && result != null) {
                        if (results == null) {
                            results = new ArrayList<R>();
                        }
                        results.add(result);
                    }
                }
            } catch (Exception e) {
                if (retryHandler != null) {
                    if (retries == null) {
                        retries = new ArrayList<T>();
                    }
                    retries.add(items.get(i));
                    lastException = e;
                }
            }
        }

        processor.cleanup(tenantId, items);

        if (results != null && results.size() > 0) {
            resultHandler.handle(tenantId, results);
        }

        if (retries != null && retries.size() > 0) {
            if (getRetryCount() > 0) {
                retryHandler.handle(tenantId, retries);
            } else {
                msgLog.warnMaxRetryReached(lastException);
            }
        }
    }

}
