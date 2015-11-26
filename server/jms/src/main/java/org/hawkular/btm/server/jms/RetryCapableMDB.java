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
package org.hawkular.btm.server.jms;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
public abstract class RetryCapableMDB<S> implements MessageListener {

    private static final Logger log = Logger.getLogger(RetryCapableMDB.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private TypeReference<java.util.List<S>> typeReference;

    private AbstractPublisherJMS<S> retryPublisher;

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
            // TODO: Handle nak of JMS message?
            e.printStackTrace();
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
    protected abstract void process(String tenantId, List<S> items, int retryCount) throws Exception;

}
