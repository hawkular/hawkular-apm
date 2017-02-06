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

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.jms.RetryCapableMDB;
import org.hawkular.apm.server.processor.tracecompletiontime.TraceCompletionInformation;
import org.hawkular.apm.server.processor.tracecompletiontime.TraceCompletionInformationInitiator;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "Trace_TraceCompletionInformationInitiator",
        messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "Traces"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientID",
                            propertyValue = TraceCompletionInformationInitiatorMDB.SUBSCRIBER),
                @ActivationConfigProperty(propertyName = "subscriptionName",
                            propertyValue = TraceCompletionInformationInitiatorMDB.SUBSCRIBER),
                @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+TraceCompletionInformationInitiatorMDB.SUBSCRIBER+"'")
        })
public class TraceCompletionInformationInitiatorMDB
                extends RetryCapableMDB<Trace, TraceCompletionInformation> {

    @Inject
    private TracePublisherJMS tracePublisher;

    @Inject
    private TraceCompletionInformationPublisherJMS traceCompletionInformationPublisher;

    public static final String SUBSCRIBER = "TraceCompletionInformationInitiator";

    public TraceCompletionInformationInitiatorMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setProcessor(new TraceCompletionInformationInitiator());
        setRetryPublisher(tracePublisher);
        setPublisher(traceCompletionInformationPublisher);
        setTypeReference(new TypeReference<java.util.List<Trace>>() {
        });
    }

}
