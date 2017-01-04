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

package org.hawkular.apm.server.jms.span;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This MDB receives all published Spans and stores them in a local cache for use by other processors.
 *
 * @author gbrown
 */
@MessageDriven(name = "Span_Cache", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "Spans"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = "apm-${jboss.node.name}"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = SpanCacheMDB.SUBSCRIBER)
})
public class SpanCacheMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(SpanCacheMDB.class.getName());

    public static final String SUBSCRIBER = "SpanCache";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private SpanCache spanCache;

    private TypeReference<java.util.List<Span>> typeRef = new TypeReference<java.util.List<Span>>() {
    };

    @Override
    public void onMessage(Message message) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Message received=" + message);
        }

        try {
            String tenantId = message.getStringProperty("tenant");

            String data = ((TextMessage) message).getText();

            List<Span> items = mapper.readValue(data, typeRef);

            spanCache.store(tenantId, items, SpanUniqueIdGenerator::toUnique);

        } catch (JMSException | IOException | CacheException e) {
            log.log(Level.SEVERE, "Failed to process message", e);
        }
    }

}
