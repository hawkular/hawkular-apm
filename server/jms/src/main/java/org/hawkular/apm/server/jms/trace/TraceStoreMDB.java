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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.jms.RetryCapableMDB;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "Trace_Store", messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "Traces"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientID", propertyValue = TraceStoreMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = TraceStoreMDB.SUBSCRIBER),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "subscriber IS NULL OR subscriber = '"+TraceStoreMDB.SUBSCRIBER+"'")
})
public class TraceStoreMDB extends RetryCapableMDB<Trace, Void> {

    @Inject
    private TracePublisherJMS tracePublisher;

    @Inject
    private TraceService traceService;

    public static final String SUBSCRIBER = "TraceStore";

    public TraceStoreMDB() {
        super(SUBSCRIBER);
    }

    @PostConstruct
    public void init() {
        setRetryPublisher(tracePublisher);
        setTypeReference(new TypeReference<java.util.List<Trace>>() {
        });

        setProcessor(new AbstractProcessor<Trace, Void>(ProcessorType.ManyToMany) {

            @Override
            public List<Void> processManyToMany(String tenantId, List<Trace> items)
                    throws RetryAttemptException {
                try {
                    traceService.storeFragments(tenantId, items);
                } catch (StoreException se) {
                    throw new RetryAttemptException(se);
                }
                return null;
            }
        });
    }

}
