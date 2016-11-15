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
package org.hawkular.apm.processor.alerts;

import java.io.IOException;
import java.util.List;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.apm.api.model.events.CompletionTime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Juraci Paixão Kröhling
 */
@MessageDriven(name = "TraceCompletionTimes_Alerts", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "TraceCompletionTimes"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = TraceCompletionTimeAlertsPublisherMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = TraceCompletionTimeAlertsPublisherMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+TraceCompletionTimeAlertsPublisherMDB.SUBSCRIBER+"'")
})
public class TraceCompletionTimeAlertsPublisherMDB implements MessageListener {
    static final String SUBSCRIBER = "TraceCompletionTimeAlertsPublisher";
    private static final MsgLogger logger = MsgLogger.LOGGER;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    AlertsPublisher publisher;

    @Override
    public void onMessage(Message message) {
        logger.traceCompletionTimeReceived();
        try {
            String data = ((TextMessage) message).getText();
            List<CompletionTime> items = mapper.readValue(data, new TypeReference<List<CompletionTime>>() {});
            publisher.publish(items, "TraceCompletion");
        } catch (IOException | JMSException e) {
            logger.errorPublishingToAlerts(e);
        }
    }
}
