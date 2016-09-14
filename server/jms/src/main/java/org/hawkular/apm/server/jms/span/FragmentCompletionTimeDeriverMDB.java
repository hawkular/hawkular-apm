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
package org.hawkular.apm.server.jms.span;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.jms.FragmentCompletionTimePublisherJMS;
import org.hawkular.apm.server.jms.RetryCapableMDB;
import org.hawkular.apm.server.processor.zipkin.FragmentCompletionTimeDeriver;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "Span_FragmentCompletionTimeDeriver",
        messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "Spans"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientID", propertyValue = FragmentCompletionTimeDeriverMDB.SUBSCRIBER),
                @ActivationConfigProperty(propertyName = "subscriptionName",
                            propertyValue = FragmentCompletionTimeDeriverMDB.SUBSCRIBER),
                @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+FragmentCompletionTimeDeriverMDB.SUBSCRIBER+"'")
        })
public class FragmentCompletionTimeDeriverMDB extends RetryCapableMDB<Span, CompletionTime> {

    @Inject
    private SpanPublisherJMS spanPublisher;

    @Inject
    private FragmentCompletionTimePublisherJMS fragmentCompletionTimePublisher;

    /**  */
    public static final String SUBSCRIBER = "SpanFragmentCompletionTimeDeriver";

    public FragmentCompletionTimeDeriverMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setProcessor(new FragmentCompletionTimeDeriver());
        setRetryPublisher(spanPublisher);
        setPublisher(fragmentCompletionTimePublisher);
        setTypeReference(new TypeReference<java.util.List<Span>>() {
        });
    }

}
