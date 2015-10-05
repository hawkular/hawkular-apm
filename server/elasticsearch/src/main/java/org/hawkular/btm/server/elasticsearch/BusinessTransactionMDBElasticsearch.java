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
package org.hawkular.btm.server.elasticsearch;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
@MessageDriven(name = "BusinessTransactions_Elasticsearch", messageListenerInterface = MessageListener.class,
activationConfig =
{
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "BusinessTransactions")
})
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class BusinessTransactionMDBElasticsearch implements MessageListener {

    /**  */
    private static final String BUSINESS_TRANSACTION_TYPE = "businesstransaction";

    private static final Logger log = Logger.getLogger(BusinessTransactionMDBElasticsearch.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
            };

    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        client = new ElasticsearchClient();
        try {
            client.init();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to initialise Elasticsearch", e);
        }
    }

    /* (non-Javadoc)
     * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
     */
    @Override
    public void onMessage(Message message) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Elasticsearch: Business transactions received=" + message);
        }

        try {
            String tenantId = message.getStringProperty("tenant");

            client.initTenant(tenantId);

            String data = ((TextMessage) message).getText();

            List<BusinessTransaction> btxns = mapper.readValue(data, BUSINESS_TXN_LIST);

            BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

            for (int i = 0; i < btxns.size(); i++) {
                BusinessTransaction btxn = btxns.get(i);
                bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                        BUSINESS_TRANSACTION_TYPE, btxn.getId()).setSource(mapper.writeValueAsString(btxn)));
            }

            BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

            if (bulkItemResponses.hasFailures()) {

                // TODO: Candidate for retry??? HWKBTM-187
                log.severe("Failed to store business transactions: " + bulkItemResponses.buildFailureMessage());

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to store business transactions to elasticsearch: "
                            + bulkItemResponses.buildFailureMessage());
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Success storing business transactions to elasticsearch");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void close() {
        client.close();
    }

}
