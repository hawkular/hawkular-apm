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

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.services.AnalyticsService;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "NodeDetails_Store", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "NodeDetails"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = NodeDetailsStoreMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = NodeDetailsStoreMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+NodeDetailsStoreMDB.SUBSCRIBER+"'")
})
public class NodeDetailsStoreMDB extends RetryCapableMDB<NodeDetails, Void> {

    @Inject
    private NodeDetailsPublisherJMS nodeDetailsPublisher;

    @Inject
    private AnalyticsService analyticsService;

    public static final String SUBSCRIBER = "NodeDetailsStore";

    public NodeDetailsStoreMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setRetryPublisher(nodeDetailsPublisher);
        setTypeReference(new TypeReference<java.util.List<NodeDetails>>() {
        });

        setProcessor(new AbstractProcessor<NodeDetails, Void>(ProcessorType.ManyToMany) {

            @Override
            public List<Void> processManyToMany(String tenantId, List<NodeDetails> items)
                    throws RetryAttemptException {
                try {
                    analyticsService.storeNodeDetails(tenantId, items);
                } catch (StoreException se) {
                    throw new RetryAttemptException(se);
                }
                return null;
            }
        });
    }

}
