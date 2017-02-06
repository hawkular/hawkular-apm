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
package org.hawkular.apm.server.jms;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.apm.api.services.Publisher;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.server.api.task.ProcessingUnit;
import org.hawkular.apm.server.api.task.Processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents a MDB based class to handle processing of events, publication
 * of results, and resubmission of failed events using a retry mechanism.
 *
 * @author gbrown
 *
 * @param <S> Source event type
 * @param <T> Target event type
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

    private String retrySubscriber;

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private int maxRetryCount = PropertyUtil.getPropertyAsInteger(PropertyUtil.HAWKULAR_APM_PROCESSOR_MAX_RETRY_COUNT,
            DEFAULT_MAX_RETRY_COUNT);

    /**
     * This constructor initialises the retry capable MDB with the subscriber name.
     *
     * @param subscriberName The subscriber name
     */
    public RetryCapableMDB(String subscriberName) {
        retrySubscriber = subscriberName;
    }

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
                retryCount = maxRetryCount;
            }

            String data = ((TextMessage) message).getText();

            List<S> items = mapper.readValue(data, getTypeReference());

            process(tenantId, items, retryCount);

        } catch (Exception e) {
            if (processor.isReportRetryExpirationAsWarning()) {
                serverMsgLogger.warnMaxRetryReached(e);
            } else if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Maximum retry reached. Last exception to occur ....", e);
            }
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
        pu.setRetrySubscriber(retrySubscriber);
        pu.setRetryCount(retryCount);

        pu.setResultHandler(
                (tid, events) -> getPublisher().publish(tid, events, getPublisher().getInitialRetryCount(),
                                getProcessor().getDeliveryDelay(events))
        );

        pu.setRetryHandler(
                (tid, events) -> getRetryPublisher().retry(tid, events, pu.getRetrySubscriber(),
                        pu.getRetryCount() - 1, getProcessor().getRetryDelay(events, pu.getRetryCount() - 1))
        );

        pu.handle(tenantId, items);
    }

}
