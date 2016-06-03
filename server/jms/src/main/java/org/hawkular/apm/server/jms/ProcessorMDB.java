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
package org.hawkular.apm.server.jms;

import java.util.List;

import org.hawkular.apm.api.services.Publisher;
import org.hawkular.apm.server.api.task.Handler;
import org.hawkular.apm.server.api.task.ProcessingUnit;
import org.hawkular.apm.server.api.task.Processor;

/**
 * @author gbrown
 */
public abstract class ProcessorMDB<S, T> extends RetryCapableMDB<S> {

    private Processor<S, T> processor;

    private Publisher<T> publisher;

    /**
     * @return the processor
     */
    public Processor<S, T> getProcessor() {
        return processor;
    }

    /**
     * @param processor the processor to set
     */
    public void setProcessor(Processor<S, T> processor) {
        this.processor = processor;
    }

    /**
     * @return the publisher
     */
    public Publisher<T> getPublisher() {
        return publisher;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process(String tenantId, List<S> items, int retryCount) throws Exception {
        ProcessingUnit<S, T> pu =
                new ProcessingUnit<S, T>();

        pu.setProcessor(getProcessor());
        pu.setRetryCount(retryCount);

        pu.setResultHandler(new Handler<T>() {
            @Override
            public void handle(String tenantId, List<T> items) throws Exception {
                getPublisher().publish(tenantId, items, getPublisher().getInitialRetryCount(),
                        getProcessor().getDeliveryDelay(items));
            }
        });

        pu.setRetryHandler(new Handler<S>() {
            @Override
            public void handle(String tenantId, List<S> items) throws Exception {
                getRetryPublisher().publish(tenantId, items, pu.getRetryCount() - 1,
                        getProcessor().getRetryDelay(items));
            }
        });

        pu.handle(tenantId, items);
    }

}
