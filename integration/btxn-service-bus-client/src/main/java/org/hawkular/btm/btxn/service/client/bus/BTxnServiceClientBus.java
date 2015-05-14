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
package org.hawkular.btm.btxn.service.client.bus;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;

import org.hawkular.btm.api.log.MsgLogger;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * This class represents the Bus client implementation of the Business Transaction
 * Service.
 *
 * @author gbrown
 */
public class BTxnServiceClientBus implements BusinessTransactionService {

    private final Logger log = Logger.getLogger(BTxnServiceClientBus.class);
    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private final String CONNECTION_FACTORY = "java:/HawkularBusConnectionFactory";
    private final String BTXN_FRAGMENTS_QUEUE = "HawkularBTM.BTxnFragments";
    private final String BTXN_SERVICE_GET_QUEUE = "HawkularBTM.BTxnService.Get";
    private final String BTXN_SERVICE_QUERY_QUEUE = "HawkularBTM.BTxnService.Query";

    private static final String HAWKULAR_BTM_BUS_TIMEOUT = "hawkular.btm.bus.timeout";

    private final long DEFAULT_BUS_TIMEOUT = 10000;

    private ConnectionFactory connectionFactory;
    private ConnectionContextFactory conContextFactory;
    private ProducerConnectionContext btxnFragmentsPCC;
    private ProducerConnectionContext btxnGetPCC;
    private ProducerConnectionContext btxnQueryPCC;
    private InitialContext initialContext;
    private MessageProcessor messageProcessor;

    private long busTimeout = DEFAULT_BUS_TIMEOUT;

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
            };

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * This method initializes the business transaction service bus client.
     *
     * @throws Exception Failed to initialize the bus client
     */
    @PostConstruct
    public void init() throws Exception {
        initialContext = new InitialContext();

        connectionFactory = (ConnectionFactory) initialContext.lookup(CONNECTION_FACTORY);
        conContextFactory = new ConnectionContextFactory(connectionFactory);
        btxnFragmentsPCC = conContextFactory.createProducerConnectionContext(
                new Endpoint(Endpoint.Type.QUEUE, BTXN_FRAGMENTS_QUEUE));
        btxnGetPCC = conContextFactory.createProducerConnectionContext(
                new Endpoint(Endpoint.Type.QUEUE, BTXN_SERVICE_GET_QUEUE));
        btxnQueryPCC = conContextFactory.createProducerConnectionContext(
                new Endpoint(Endpoint.Type.QUEUE, BTXN_SERVICE_QUERY_QUEUE));

        messageProcessor = new MessageProcessor();

        // Check if bus timeout defined
        if (System.getProperties().containsKey(HAWKULAR_BTM_BUS_TIMEOUT)) {
            try {
                String str = System.getProperty(HAWKULAR_BTM_BUS_TIMEOUT);
                busTimeout = Long.getLong(str);
            } catch (Exception e) {
                msgLog.errorConvertingPropertyToType(HAWKULAR_BTM_BUS_TIMEOUT, "long", e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#store(java.util.List)
     */
    @Override
    public void store(List<BusinessTransaction> btxns) throws Exception {

        // Serialize the business transaction list
        String json = mapper.writeValueAsString(btxns);

        SimpleBasicMessage message = new SimpleBasicMessage(json);

        log.tracef("Store business transactions: %s", message);

        messageProcessor.send(btxnFragmentsPCC, message);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String)
     */
    @Override
    public BusinessTransaction get(String id) {
        SimpleBasicMessage message = new SimpleBasicMessage(id);
        SimpleBasicMessage resp = null;

        try {
            log.tracef("Get business transaction with id[%s]", id);

            ListenableFuture<SimpleBasicMessage> future =
                    messageProcessor.sendRPC(btxnGetPCC, message, SimpleBasicMessage.class);

            resp = future.get(busTimeout, TimeUnit.MILLISECONDS);

            log.tracef("Got business transaction with id[%s] = %s", id, resp);

        } catch (Exception e) {
            msgLog.errorSendingMessage(e);
        }

        if (resp != null) {
            try {
                return mapper.readValue(resp.getMessage().getBytes(), BusinessTransaction.class);
            } catch (Exception e) {
                msgLog.errorFailedToDeserializeJson(resp.getMessage(), e);
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(
     *              org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public List<BusinessTransaction> query(BusinessTransactionCriteria criteria) {
        SimpleBasicMessage message = null;

        try {
            message = new SimpleBasicMessage(mapper.writeValueAsString(criteria));
        } catch (JsonProcessingException e1) {
            msgLog.errorFailedToSerializeToJson(e1);
        }

        SimpleBasicMessage resp = null;

        try {
            log.tracef("Get business transactions with criteria[%s]", criteria);

            ListenableFuture<SimpleBasicMessage> future =
                    messageProcessor.sendRPC(btxnQueryPCC, message, SimpleBasicMessage.class);

            resp = future.get(busTimeout, TimeUnit.MILLISECONDS);

            log.tracef("Got business transactions with criteria[%s] = %s", criteria, resp);

        } catch (Exception e) {
            msgLog.errorSendingMessage(e);
        }

        if (resp != null) {
            try {
                return mapper.readValue(resp.getMessage().getBytes(), BUSINESS_TXN_LIST);
            } catch (Exception e) {
                msgLog.errorFailedToDeserializeJson(resp.getMessage(), e);
            }
        }

        return null;
    }

}
