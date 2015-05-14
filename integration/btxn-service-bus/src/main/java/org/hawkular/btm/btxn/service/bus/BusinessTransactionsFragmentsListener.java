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
package org.hawkular.btm.btxn.service.bus;

import java.util.List;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.btm.api.log.MsgLogger;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The listener for receiving business transaction fragments.
 *
 * @author gbrown
 */
@MessageDriven(messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "HawkularBTM.BTxnFragments") })
public class BusinessTransactionsFragmentsListener extends BasicMessageListener<SimpleBasicMessage> {
    private final Logger log = Logger.getLogger(BusinessTransactionsFragmentsListener.class);
    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
    };

    @Inject
    private BusinessTransactionService businessTransactionService;

    @Override
    protected void onBasicMessage(SimpleBasicMessage msg) {

        log.tracef("Business Transaction Service STORE request: %s", msg.getMessage());

        // Deserialize the message
        List<BusinessTransaction> btxns = null;

        try {
            btxns = mapper.readValue(msg.getMessage(), BUSINESS_TXN_LIST);
        } catch (Exception e) {
            msgLog.errorFailedToDeserializeJson(msg.getMessage(), e);
        }

        // Store in business transaction service
        if (btxns != null) {
            try {
                businessTransactionService.store(btxns);
            } catch (Exception e) {
                // TODO Implement retry/recovery - possibly abort txn, but
                // may need to send to a replay queue
                e.printStackTrace();
            }
        }
    }
}