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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.btm.api.model.analytics.CompletionTime;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;
import org.hawkular.btm.processor.completiontime.CompletionTimeDeriver;
import org.hawkular.btm.server.api.services.CompletionTimePublisher;
import org.hawkular.btm.server.api.task.Handler;
import org.hawkular.btm.server.api.task.ProcessingUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
@MessageDriven(name = "BusinessTransaction_CompletionTimeDeriver", messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "BusinessTransactions")
        })
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class CompletionTimeDeriverMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(CompletionTimeDeriverMDB.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
    };

    private static CompletionTimeDeriver processor = new CompletionTimeDeriver();

    @Inject
    private BusinessTransactionPublisher businessTransactionPublisher;

    @Inject
    private CompletionTimePublisher completionTimePublisher;

    /* (non-Javadoc)
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(Message message) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("CompletionTimeDeriver received=" + message);
        }

        try {
            String tenantId = message.getStringProperty("tenant");

            final int retryCount;

            if (message.propertyExists("retryCount")) {
                retryCount = message.getIntProperty("retryCount");
            } else {
                retryCount = 3; // TODO: Should this be configurable?
            }

            String data = ((TextMessage) message).getText();

            List<BusinessTransaction> btxns = mapper.readValue(data, BUSINESS_TXN_LIST);

            ProcessingUnit<BusinessTransaction, CompletionTime> pu =
                    new ProcessingUnit<BusinessTransaction, CompletionTime>();

            pu.setProcessor(processor);
            pu.setRetryCount(retryCount);

            pu.setResultHandler(new Handler<CompletionTime>() {
                @Override
                public void handle(List<CompletionTime> items) throws Exception {
                    completionTimePublisher.publish(tenantId, items);
                }
            });

            pu.setRetryHandler(new Handler<BusinessTransaction>() {
                @Override
                public void handle(List<BusinessTransaction> items) throws Exception {
                    businessTransactionPublisher.publish(tenantId, items);
                }
            });

            pu.handle(btxns);

        } catch (Exception e) {
            // TODO: Handle nak of JMS message?
            e.printStackTrace();
        }
    }

}
