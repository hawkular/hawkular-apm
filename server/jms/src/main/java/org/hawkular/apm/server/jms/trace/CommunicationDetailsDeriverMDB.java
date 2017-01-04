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

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.jms.CommunicationDetailsPublisherJMS;
import org.hawkular.apm.server.jms.RetryCapableMDB;
import org.hawkular.apm.server.processor.communicationdetails.CommunicationDetailsDeriver;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "Trace_CommunicationDetailsDeriver",
        messageListenerInterface = MessageListener.class,
activationConfig =
{
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "Traces"),
    @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
    @ActivationConfigProperty(propertyName = "clientID", propertyValue = CommunicationDetailsDeriverMDB.SUBSCRIBER),
    @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = CommunicationDetailsDeriverMDB.SUBSCRIBER),
    @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+CommunicationDetailsDeriverMDB.SUBSCRIBER+"'")
})
public class CommunicationDetailsDeriverMDB extends RetryCapableMDB<Trace, CommunicationDetails> {

    @Inject
    private TracePublisherJMS tracePublisher;

    @Inject
    private CommunicationDetailsPublisherJMS communicationDetailsPublisher;

    @Inject
    private CommunicationDetailsDeriver communicationDetailsDeriver;

    public static final String SUBSCRIBER = "CommunicationDetailsDeriver";

    public CommunicationDetailsDeriverMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setProcessor(communicationDetailsDeriver);
        setRetryPublisher(tracePublisher);
        setPublisher(communicationDetailsPublisher);
        setTypeReference(new TypeReference<java.util.List<Trace>>() {
        });
    }

}
