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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "BusinessTransactions"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientId", propertyValue = "BusinessTransactionStore"),
                @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "BusinessTransactionStore")
        })
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class BusinessTransactionStoreMDB extends RetryCapableMDB<BusinessTransaction> {

    private static final Logger perfLog=Logger.getLogger("org.hawkular.btm.performance.btxn");

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
        long startTime=0;
        if (perfLog.isLoggable(Level.FINEST)) {
            startTime = System.currentTimeMillis();
            perfLog.finest("Performance: about to store btxn (first id="+items.get(0).getId()+")");
        }
        businessTransactionService.storeBusinessTransactions(tenantId, items);
        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: store btxn (first id="+items.get(0).getId()+") duration="+
                        (System.currentTimeMillis()-startTime)+"ms");
        }
    }

}
