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
package org.hawkular.btm.server.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.server.cassandra.log.MsgLogger;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
@Singleton
public class BusinessTransactionServiceCassandra implements BusinessTransactionService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private static final Logger log = Logger.getLogger(BusinessTransactionServiceCassandra.class.getName());

    private static ObjectMapper mapper = new ObjectMapper();

    private PreparedStatement getBusinessTransaction;

    private PreparedStatement insertBusinessTransaction;

    private PreparedStatement deleteBusinessTransactions;

    @Inject
    private CassandraClient client;

    /**
     * @return the client
     */
    public CassandraClient getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(CassandraClient client) {
        this.client = client;
    }

    @PostConstruct
    public void init() {
        getBusinessTransaction = getClient().getSession().prepare(
                "SELECT doc FROM hawkular_btm.businesstransactions "
                        + "WHERE tenantId = ? AND id = ?;");

        insertBusinessTransaction = getClient().getSession().prepare(
                "INSERT INTO hawkular_btm.businesstransactions "
                        + "(tenantId, datetime, id, businessTransaction, hostName, "
                        + "properties, correlationIds, doc) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

        deleteBusinessTransactions = getClient().getSession().prepare(
                "DELETE FROM hawkular_btm.businesstransactions "
                        + "WHERE tenantId = ?;");
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        ResultSet results = getClient().getSession().execute(new BoundStatement(getBusinessTransaction).bind(
                tenantId, id));
        Row row = results.one();
        if (row != null) {
            try {
                return mapper.readValue(row.getString("doc"), BusinessTransaction.class);
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
     *              org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> ret = new ArrayList<BusinessTransaction>();

        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.businesstransactions");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Query statement = "+statement.toString());
        }

        ResultSet results = getClient().getSession().execute(statement.toString());
        for (Row row : results) {
            try {
                BusinessTransaction btxn=mapper.readValue(row.getString("doc"), BusinessTransaction.class);
                if (!CassandraServiceUtil.exclude(btxn.getProperties(), null, criteria)) {
                    ret.add(btxn);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#storeBusinessTransactions(java.lang.String,
     *                          java.util.List)
     */
    @Override
    public void storeBusinessTransactions(String tenantId, List<BusinessTransaction> businessTransactions)
            throws Exception {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Store business transactions (tenantId=" + tenantId + "):" + businessTransactions);
        }

        BatchStatement batch = new BatchStatement();

        for (int i = 0; i < businessTransactions.size(); i++) {
            BusinessTransaction btxn = businessTransactions.get(i);
            List<CorrelationIdentifier> cids = null;
            if (!btxn.getNodes().isEmpty()) {
                cids = btxn.getNodes().get(0).getCorrelationIds();
            }
            batch.add(new BoundStatement(insertBusinessTransaction).bind(
                    CassandraServiceUtil.tenant(tenantId),
                    new Date(btxn.getStartTime()),
                    btxn.getId(),
                    CassandraServiceUtil.emptyStringForNull(btxn.getName()),
                    btxn.getHostName(),
                    CassandraServiceUtil.toTagList(btxn.getProperties()),
                    CassandraServiceUtil.toTagList(cids),
                    mapper.writeValueAsString(btxn)));
        }

        getClient().getSession().execute(batch);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        client.getSession().execute(new BoundStatement(deleteBusinessTransactions).bind(
                CassandraServiceUtil.tenant(tenantId)));
    }

}
