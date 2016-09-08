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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.processor.zipkin.CompletionTimeProcessing;
import org.hawkular.apm.processor.zipkin.CompletionTimeProcessingDeriver;
import org.hawkular.apm.processor.zipkin.CompletionTimeProcessingPublisher;
import org.hawkular.apm.server.jms.RetryCapableMDB;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author Pavol Loffay
 */
@MessageDriven(name = "CompletionTimeProcessing_TraceCompletionTimeProcessingDeriver",
        messageListenerInterface = MessageListener.class,
        activationConfig =  {
            @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
            @ActivationConfigProperty(propertyName = "destination", propertyValue ="SpanTraceCompletionTimeProcessing"),
            @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
            @ActivationConfigProperty(propertyName = "clientID", propertyValue = CompletionTimeProcessingMDB.SUBSCRIBER),
            @ActivationConfigProperty(propertyName = "subscriptionName",
                    propertyValue = CompletionTimeProcessingMDB.SUBSCRIBER),
            @ActivationConfigProperty(propertyName = "messageSelector",
                    propertyValue = "subscriber IS NULL OR subscriber = '"+CompletionTimeProcessingMDB.SUBSCRIBER+"'")
})
public class CompletionTimeProcessingMDB extends RetryCapableMDB<CompletionTimeProcessing, CompletionTimeProcessing> {

    public static final String SUBSCRIBER = "SpanTraceCompletionTimeProcessingDeriver";

    @Inject
    private CompletionTimeProcessingDeriver completionTimeProcessingDeriver;

    @Inject
    private CompletionTimeProcessingPublisher traceCompletionTimePublisher;

    @Inject
    private CompletionTimeProcessingPublisherJMS processingPublisherJMS;

    public CompletionTimeProcessingMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setProcessor(completionTimeProcessingDeriver);
        setRetryPublisher(processingPublisherJMS);
        setPublisher(traceCompletionTimePublisher);
        setTypeReference(new TypeReference<List<CompletionTimeProcessing>>() {
        });
    }
}
