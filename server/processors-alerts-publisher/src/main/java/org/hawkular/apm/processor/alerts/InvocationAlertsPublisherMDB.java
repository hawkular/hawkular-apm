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
package org.hawkular.apm.processor.alerts;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.apm.api.model.events.NodeDetails;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Juraci Paixão Kröhling
 */
@MessageDriven(name = "NodeDetails_Alerts", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "NodeDetails"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = InvocationAlertsPublisherMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = InvocationAlertsPublisherMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+ InvocationAlertsPublisherMDB.SUBSCRIBER+"'")
})
public class InvocationAlertsPublisherMDB implements MessageListener {
    static final String SUBSCRIBER = "InvocationAlertsPublisher";
    private static final MsgLogger logger = MsgLogger.LOGGER;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    AlertsPublisher publisher;

    @Override
    public void onMessage(Message message) {
        logger.invocationDetailsReceived();
        try {
            String data = ((TextMessage) message).getText();
            List<NodeDetails> items = mapper.readValue(data, new TypeReference<List<NodeDetails>>() {});

            // The list of NodeDetails is filtered to extract the ones with the
            // 'initial' flag set. These NodeDetails represent the handling of
            // service requests (from which service response times can be determined)
            // or the top most component invoked for an application.
            List<Event> events = items.stream().filter(nd -> nd.isInitial())
                    .map(InvocationAlertsPublisherMDB::toEvent)
                    .collect(Collectors.toList());
            if (!events.isEmpty()) {
                publisher.publish(events);
            }
        } catch (IOException | JMSException e) {
            logger.errorPublishingToAlerts(e);
        }
    }

    public static Event toEvent(NodeDetails nodeDetails) {
        Event event = new Event();
        event.getContext().put("id", nodeDetails.getId());

        if (null != nodeDetails.getUri()) {
            event.getTags().put("uri", nodeDetails.getUri());
        }
        if (null != nodeDetails.getOperation()) {
            event.getTags().put("operation", nodeDetails.getOperation());
        }

        event.initTagsFromProperties(nodeDetails.getProperties());

        event.setDataId("Invocation");
        event.setCategory("APM");
        event.setDataSource(nodeDetails.getHostName());
        event.setId(UUID.randomUUID().toString());
        event.setCtime(nodeDetails.getTimestamp());
        event.setText(Long.toString(nodeDetails.getElapsed()));

        return event;
    }
}
