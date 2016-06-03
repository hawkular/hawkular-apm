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
package org.hawkular.apm.server.cassandra;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.server.cassandra.log.MsgLogger;

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
public class TraceServiceCassandra implements TraceService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private static final Logger log = Logger.getLogger(TraceServiceCassandra.class.getName());

    private static ObjectMapper mapper = new ObjectMapper();

    private PreparedStatement getTraces;

    private PreparedStatement insertTrace;

    private PreparedStatement deleteTraces;

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
        getTraces = getClient().getSession().prepare(
                "SELECT doc FROM hawkular_apm.traces "
                        + "WHERE tenantId = ? AND id = ?;");

        insertTrace = getClient().getSession().prepare(
                "INSERT INTO hawkular_apm.traces "
                        + "(tenantId, datetime, id, businessTransaction, hostName, "
                        + "properties, correlationIds, doc) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

        deleteTraces = getClient().getSession().prepare(
                "DELETE FROM hawkular_apm.traces "
                        + "WHERE tenantId = ?;");
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#get(java.lang.String, java.lang.String)
     */
    @Override
    public Trace get(String tenantId, String id) {
        ResultSet results = getClient().getSession().execute(new BoundStatement(getTraces).bind(
                tenantId, id));
        Row row = results.one();
        if (row != null) {
            try {
                return mapper.readValue(row.getString("doc"), Trace.class);
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#query(java.lang.String,
     *              org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<Trace> query(String tenantId, Criteria criteria) {
        List<Trace> ret = new ArrayList<Trace>();

        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_apm.traces");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Query statement = " + statement.toString());
        }

        ResultSet results = getClient().getSession().execute(statement.toString());
        for (Row row : results) {
            try {
                Trace trace = mapper.readValue(row.getString("doc"), Trace.class);
                if (!CassandraServiceUtil.exclude(trace.getProperties(), null, criteria)) {
                    ret.add(trace);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#storeBusinessTransactions(java.lang.String,
     *                          java.util.List)
     */
    @Override
    public void storeTraces(String tenantId, List<Trace> traces)
            throws Exception {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Store traces (tenantId=" + tenantId + "):" + traces);
        }

        BatchStatement batch = new BatchStatement();

        for (int i = 0; i < traces.size(); i++) {
            Trace trace = traces.get(i);
            List<CorrelationIdentifier> cids = null;
            if (!trace.getNodes().isEmpty()) {
                cids = trace.getNodes().get(0).getCorrelationIds();
            }
            batch.add(new BoundStatement(insertTrace).bind(
                    CassandraServiceUtil.tenant(tenantId),
                    new Date(trace.getStartTime()),
                    trace.getId(),
                    CassandraServiceUtil.emptyStringForNull(trace.getBusinessTransaction()),
                    trace.getHostName(),
                    CassandraServiceUtil.toTagList(trace.getProperties()),
                    CassandraServiceUtil.toTagList(cids),
                    mapper.writeValueAsString(trace)));
        }

        getClient().getSession().execute(batch);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TraceService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        client.getSession().execute(new BoundStatement(deleteTraces).bind(
                CassandraServiceUtil.tenant(tenantId)));
    }

}
