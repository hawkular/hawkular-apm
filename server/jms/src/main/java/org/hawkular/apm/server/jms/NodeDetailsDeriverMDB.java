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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.processor.nodedetails.NodeDetailsDeriver;
import org.hawkular.apm.server.api.services.NodeDetailsPublisher;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "Trace_NodeDetailsDeriver", messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "Traces"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientID", propertyValue = "NodeDetailsDeriver"),
                @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "NodeDetailsDeriver")
        })
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class NodeDetailsDeriverMDB extends ProcessorMDB<Trace, NodeDetails> {

    @Inject
    private TracePublisherJMS tracePublisher;

    @Inject
    private NodeDetailsPublisher nodeDetailsPublisher;

    @PostConstruct
    public void init() {
        setProcessor(new NodeDetailsDeriver());
        setRetryPublisher(tracePublisher);
        setPublisher(nodeDetailsPublisher);
        setTypeReference(new TypeReference<java.util.List<Trace>>() {
        });
    }

}
