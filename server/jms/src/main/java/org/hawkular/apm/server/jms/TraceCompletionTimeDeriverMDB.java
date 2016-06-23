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
package org.hawkular.apm.server.jms;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionInformation;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionTimeDeriver;
import org.hawkular.apm.server.api.services.TraceCompletionTimePublisher;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "TraceCompletionInformation_TraceCompletionTimeDeriver",
        messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "TraceCompletionInformation"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientID", propertyValue = TraceCompletionTimeDeriverMDB.SUBSCRIBER),
                @ActivationConfigProperty(propertyName = "subscriptionName",
                            propertyValue = TraceCompletionTimeDeriverMDB.SUBSCRIBER),
                @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+TraceCompletionTimeDeriverMDB.SUBSCRIBER+"'")
        })
public class TraceCompletionTimeDeriverMDB extends RetryCapableMDB<TraceCompletionInformation, CompletionTime> {

    @Inject
    private TraceCompletionInformationPublisherJMS traceCompletionInformationPublisher;

    @Inject
    private TraceCompletionTimePublisher traceCompletionTimePublisher;

    /**  */
    public static final String SUBSCRIBER = "TraceCompletionTimeDeriver";

    public TraceCompletionTimeDeriverMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setProcessor(new TraceCompletionTimeDeriver());
        setRetryPublisher(traceCompletionInformationPublisher);
        setPublisher(traceCompletionTimePublisher);
        setTypeReference(new TypeReference<java.util.List<TraceCompletionInformation>>() {
        });
    }

}
