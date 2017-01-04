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

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.CommunicationDetailsCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * This MDB is provided to populate the CommunicationDetailsCache when Communication Details.
 * Each clustered APM server node will receive the communication details data, so each cache
 * is expected to only be a local cache.
 *
 * Discussion regarding potential future use of a distributed cache is associated with HWKAPM-479.
 * If this change happens, then this MDB should be converted back to a durable (load balanced) subscriber.
 *
 * @author gbrown
 */
@MessageDriven(name = "CommunicationDetails_CommunicationDetailsCache", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "CommunicationDetails"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = "apm-${jboss.node.name}"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = CommunicationDetailsCacheMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+CommunicationDetailsCacheMDB.SUBSCRIBER+"'")
})
public class CommunicationDetailsCacheMDB extends RetryCapableMDB<CommunicationDetails, Void> {

    @Inject
    private CommunicationDetailsPublisherJMS communicationDetailsPublisher;

    @Inject
    private CommunicationDetailsCache communicationDetailsCache;

    public static final String SUBSCRIBER = "CommunicationDetailsCache";

    public CommunicationDetailsCacheMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setRetryPublisher(communicationDetailsPublisher);
        setTypeReference(new TypeReference<java.util.List<CommunicationDetails>>() {
        });

        setProcessor(new AbstractProcessor<CommunicationDetails, Void>(ProcessorType.ManyToMany) {

            @Override
            public List<Void> processManyToMany(String tenantId, List<CommunicationDetails> items)
                    throws RetryAttemptException {
                try {
                    communicationDetailsCache.store(tenantId, items);
                } catch (CacheException e) {
                    throw new RetryAttemptException(e);
                }
                return null;
            }
        });
    }

}
