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

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;

import org.hawkular.btm.api.log.MsgLogger;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.hawkular.bus.mdb.RPCBasicMessageDrivenBean;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The listener for receiving business transaction fragments.
 *
 * @author gbrown
 */
@MessageDriven(messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "HawkularBTM.BTxnService.Get") })
public class BusinessTransactionsGetListener extends RPCBasicMessageDrivenBean<SimpleBasicMessage, SimpleBasicMessage> {
    private final Logger log = Logger.getLogger(BusinessTransactionsGetListener.class);
    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private ObjectMapper mapper = new ObjectMapper();

    @Inject
    private BusinessTransactionService businessTransactionService;

    @Resource(mappedName = "java:/HawkularBusConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    @Override
    protected SimpleBasicMessage onBasicMessage(SimpleBasicMessage msg) {
        log.tracef("Business Transaction Service GET request: id[%s]", msg.getMessage());

        BusinessTransaction btxn = businessTransactionService.get(msg.getMessage());

        String resp = null;

        try {
            resp = mapper.writeValueAsString(btxn);
        } catch (JsonProcessingException e) {
            msgLog.errorFailedToSerializeToJson(e);
        }

        log.tracef("Business Transaction Service GET request: id[%s] btxn=%s", msg.getMessage(), btxn);

        return new SimpleBasicMessage(resp);
    }
}