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
package org.hawkular.apm.server.jms.trace;

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

import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SourceInfoCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.SourceInfoUtil;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This MDB is provided to populate the SourceInfoCache with Source Information derived from the
 * received trace fragments. Each clustered APM server node will receive the trace data, so each cache
 * is expected to only be a local cache.
 *
 * Discussion regarding potential future use of a distributed cache, which would make this MDB redundant
 * is associated with HWKAPM-479.
 *
 * @author gbrown
 */
@MessageDriven(name = "Trace_SourceInfoCache", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "Traces"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = "apm-${jboss.node.name}"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "SourceInfoCache")
})
public class SourceInfoCacheMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(SourceInfoCacheMDB.class.getName());

    @Inject
    private SourceInfoCache sourceInfoCache;

    private static final ObjectMapper mapper = new ObjectMapper();

    private TypeReference<java.util.List<Trace>> typeRef = new TypeReference<java.util.List<Trace>>() {
    };

    @Override
    public void onMessage(Message message) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Message received=" + message);
        }

        try {
            String tenantId = message.getStringProperty("tenant");

            String data = ((TextMessage) message).getText();

            List<Trace> items = mapper.readValue(data, typeRef);

            List<SourceInfo> sourceInfoList = SourceInfoUtil.getSourceInfo(tenantId, items);

            sourceInfoCache.store(tenantId, sourceInfoList);

        } catch (JMSException | IOException | CacheException | RetryAttemptException e) {
            log.log(Level.SEVERE, "Failed to handle message", e);
        }
    }

}
