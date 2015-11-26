/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

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
import org.hawkular.btm.api.services.BusinessTransactionService;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "BusinessTransactions_Store", messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "BusinessTransactions")
        })
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class BusinessTransactionStoreMDB extends RetryCapableMDB<BusinessTransaction> {

    @Inject
    private BusinessTransactionPublisherJMS businessTransactionPublisher;

    @Inject
    private BusinessTransactionService businessTransactionService;

    @PostConstruct
    public void init() {
        setRetryPublisher(businessTransactionPublisher);
        setTypeReference(new TypeReference<java.util.List<BusinessTransaction>>() {
        });
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.jms.AbstractRetryMDB#process(java.lang.String, java.util.List, int)
     */
    @Override
    protected void process(String tenantId, List<BusinessTransaction> items, int retryCount) throws Exception {
        businessTransactionService.storeBusinessTransactions(tenantId, items);
    }

}
