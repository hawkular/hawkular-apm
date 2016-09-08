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

package org.hawkular.apm.server.jms.span.comletiontime;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.processor.zipkin.CompletionTimeDeriverInitiator;
import org.hawkular.apm.processor.zipkin.CompletionTimeProcessing;
import org.hawkular.apm.processor.zipkin.CompletionTimeProcessingPublisher;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.jms.RetryCapableMDB;
import org.hawkular.apm.server.jms.span.SpanPublisherJMS;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 * @author Pavol Loffay
 */
@MessageDriven(name = "Span_TraceCompletionTimeDeriverInitiator",
        messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "Spans"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = CompletionTimeProcessingInitiatorMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = CompletionTimeProcessingInitiatorMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"
                + CompletionTimeProcessingInitiatorMDB.SUBSCRIBER + "'")
})
public class CompletionTimeProcessingInitiatorMDB extends RetryCapableMDB<Span, CompletionTimeProcessing> {

    public static final String SUBSCRIBER = "SpanTraceCompletionTimeDeriverInitiator";

    @Inject
    private SpanPublisherJMS spanPublisher;

    @Inject
    private CompletionTimeProcessingPublisher completionTimeProcessingPublisher;

    public CompletionTimeProcessingInitiatorMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setProcessor(new CompletionTimeDeriverInitiator());
        setRetryPublisher(spanPublisher);
        setPublisher(completionTimeProcessingPublisher);
        setTypeReference(new TypeReference<java.util.List<Span>>() {
        });
    }

}
