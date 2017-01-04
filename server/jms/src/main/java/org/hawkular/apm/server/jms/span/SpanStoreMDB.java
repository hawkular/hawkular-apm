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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanService;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.hawkular.apm.server.jms.RetryCapableMDB;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author Pavol Loffay
 */
@MessageDriven(name = "Span_Store", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "Spans"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = SpanStoreMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = SpanStoreMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber" +
                " = '" + SpanStoreMDB.SUBSCRIBER + "'")
})
public class SpanStoreMDB extends RetryCapableMDB<Span, Void> {

    public static final String SUBSCRIBER = "SpanStore";

    @Inject
    private SpanPublisherJMS spanPublisher;

    @Inject
    private SpanService spanService;


    public SpanStoreMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setRetryPublisher(spanPublisher);
        setTypeReference(new TypeReference<List<Span>>() {});

        setProcessor(new AbstractProcessor<Span, Void>(Processor.ProcessorType.ManyToMany) {
            @Override
            public List<Void> processManyToMany(String tenantId, List<Span> spans) throws RetryAttemptException {
                try {
                    spanService.storeSpan(tenantId, spans, SpanUniqueIdGenerator::toUnique);
                } catch (StoreException ex) {
                    throw new RetryAttemptException(ex);
                }
                return null;
            }
        });
    }
}
