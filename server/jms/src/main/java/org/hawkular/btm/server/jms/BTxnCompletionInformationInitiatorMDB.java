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
package org.hawkular.btm.server.jms;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.processor.btxncompletiontime.BTxnCompletionInformation;
import org.hawkular.btm.processor.btxncompletiontime.BTxnCompletionInformationInitiator;
import org.hawkular.btm.processor.btxncompletiontime.BTxnCompletionInformationPublisher;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "BusinessTransaction_BTxnCompletionInformationInitiator",
        messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "BusinessTransactions"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientID",
                            propertyValue = "BTxnCompletionInformationInitiator"),
                @ActivationConfigProperty(propertyName = "subscriptionName",
                            propertyValue = "BTxnCompletionInformationInitiator")
        })
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class BTxnCompletionInformationInitiatorMDB
                extends ProcessorMDB<BusinessTransaction, BTxnCompletionInformation> {

    @Inject
    private BusinessTransactionPublisherJMS businessTransactionPublisher;

    @Inject
    private BTxnCompletionInformationPublisher btxnCompletionInformationPublisher;

    @PostConstruct
    public void init() {
        setProcessor(new BTxnCompletionInformationInitiator());
        setRetryPublisher(businessTransactionPublisher);
        setPublisher(btxnCompletionInformationPublisher);
        setTypeReference(new TypeReference<java.util.List<BusinessTransaction>>() {
        });
    }

}
