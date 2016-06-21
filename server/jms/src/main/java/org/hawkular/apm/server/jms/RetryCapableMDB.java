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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.apm.api.services.Publisher;
import org.hawkular.apm.server.api.task.Handler;
import org.hawkular.apm.server.api.task.ProcessingUnit;
import org.hawkular.apm.server.api.task.Processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
public abstract class RetryCapableMDB<S,T> implements MessageListener {

    private static final Logger log = Logger.getLogger(RetryCapableMDB.class.getName());

    private static final org.hawkular.apm.server.api.log.MsgLogger serverMsgLogger =
            org.hawkular.apm.server.api.log.MsgLogger.LOGGER;

    private static final ObjectMapper mapper = new ObjectMapper();

    private TypeReference<java.util.List<S>> typeReference;

    private AbstractPublisherJMS<S> retryPublisher;

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
     * @return the retryPublisher
     */
    public AbstractPublisherJMS<S> getRetryPublisher() {
        return retryPublisher;
    }

    /**
     * @param retryPublisher the retryPublisher to set
     */
    public void setRetryPublisher(AbstractPublisherJMS<S> retryPublisher) {
        this.retryPublisher = retryPublisher;
    }

    /**
     * @return the typeReference
     */
    public TypeReference<java.util.List<S>> getTypeReference() {
        return typeReference;
    }

    /**
     * @param typeReference the typeReference to set
     */
    public void setTypeReference(TypeReference<java.util.List<S>> typeReference) {
        this.typeReference = typeReference;
    }

    /* (non-Javadoc)
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(Message message) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Message received=" + message);
        }

        try {
            String tenantId = message.getStringProperty("tenant");

            int retryCount;

            if (message.propertyExists("retryCount")) {
                retryCount = message.getIntProperty("retryCount");
            } else {
                retryCount = 3; // TODO: Should this be configurable?
            }

            String data = ((TextMessage) message).getText();

            List<S> items = mapper.readValue(data, getTypeReference());

            process(tenantId, items, retryCount);

        } catch (Exception e) {
            serverMsgLogger.warnMaxRetryReached(e);
        }
    }

    /**
     * This method processes the received list of items.
     *
     * @param tenantId The optional tenant id
     * @param items The items
     * @param retryCount The remaining retry count
     * @throws Failed to process items
     */
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
