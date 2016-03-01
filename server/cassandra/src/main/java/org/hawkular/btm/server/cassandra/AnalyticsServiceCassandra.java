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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics.NodeComponentTypeStatistics;
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.events.CommunicationDetails;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.api.model.events.NodeDetails;
import org.hawkular.btm.api.services.AbstractAnalyticsService;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.api.services.Criteria;
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
public class AnalyticsServiceCassandra extends AbstractAnalyticsService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private static final Logger log = Logger.getLogger(AnalyticsServiceCassandra.class.getName());

    private static final Logger perfLog = Logger.getLogger("org.hawkular.btm.performance.cassandra");

    private static ObjectMapper mapper = new ObjectMapper();

    private PreparedStatement insertCompletionTimes;

    private PreparedStatement insertNodeDetails;

    private PreparedStatement deleteCompletionTimes;

    private PreparedStatement deleteNodeDetails;

    @Inject
    private CassandraClient client;

    @Inject
    private BusinessTransactionService businessTransactionService;

    @PostConstruct
    public void init() {
        insertCompletionTimes = getClient().getSession().prepare(
                "INSERT INTO hawkular_btm.completiontimes "
                        + "(tenantId, datetime, key, id, businessTransaction, "
                        + "fault, properties, doc) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

        insertNodeDetails = getClient().getSession().prepare(
                "INSERT INTO hawkular_btm.nodedetails "
                        + "(tenantId, datetime, key, id, businessTransaction, uri, "
                        + "componentType, operation, fault, hostName, "
                        + "properties, details, correlationIds, doc) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

        deleteCompletionTimes = getClient().getSession().prepare(
                "DELETE FROM hawkular_btm.completiontimes "
                        + "WHERE tenantId = ?;");

        deleteNodeDetails = getClient().getSession().prepare(
                "DELETE FROM hawkular_btm.nodedetails "
                        + "WHERE tenantId = ?;");

    }

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

    /**
     * This method gets the business transaction service.
     *
     * @return The business transaction service
     */
    public BusinessTransactionService getBusinessTransactionService() {
        return this.businessTransactionService;
    }

    /**
     * This method sets the business transaction service.
     *
     * @param bts The business transaction service
     */
    public void setBusinessTransactionService(BusinessTransactionService bts) {
        this.businessTransactionService = bts;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AbstractAnalyticsService#getFragments(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria)
     */
    @Override
    protected List<BusinessTransaction> getFragments(String tenantId, Criteria criteria) {
        return businessTransactionService.query(tenantId, criteria);
    }

    /**
     * This method determines whether a full evaluation is required for the supplied
     * criteria.
     *
     * @param criteria The criteria
     * @return Whether full evaluation is required
     */
    protected boolean requiresFullEvaluation(Criteria criteria) {
        return CassandraServiceUtil.hasExclusions(criteria) ||
                criteria.getLowerBound() > 0 ||
                criteria.getUpperBound() > 0;
    }

    /**
     * This method determines whether the supplied completion time should be excluded
     * based on the supplied criteris.
     *
     * @param ct The completion time
     * @param criteria The criteria
     * @return Whether to exclude the completion time
     */
    protected boolean exclude(CompletionTime ct, Criteria criteria) {
        return CassandraServiceUtil.exclude(ct.getProperties(), ct.getFault(), criteria) ||
                (criteria.getLowerBound() > 0 && ct.getDuration() < criteria.getLowerBound()) ||
                (criteria.getUpperBound() > 0 && ct.getDuration() > criteria.getUpperBound());
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionCount(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public long getCompletionCount(String tenantId, Criteria criteria) {
        StringBuilder statement = new StringBuilder();
        boolean fullEvaluation = requiresFullEvaluation(criteria);

        if (fullEvaluation) {
            statement.append("SELECT doc FROM hawkular_btm.completiontimes");
        } else {
            statement.append("SELECT count(*) FROM hawkular_btm.completiontimes");
        }
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Query statement = " + statement.toString());
        }

        long queryTime = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        ResultSet results = getClient().getSession().execute(statement.toString());

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results returned");
        }

        long ret = 0;

        if (fullEvaluation) {
            for (Row row : results) {
                try {
                    CompletionTime ct = mapper.readValue(row.getString("doc"), CompletionTime.class);
                    if (!exclude(ct, criteria)) {
                        ret++;
                    }
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }
        } else {
            ret = results.one().getLong(0);
        }

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms");
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionFaultCount(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public long getCompletionFaultCount(String tenantId, Criteria criteria) {
        StringBuilder statement = new StringBuilder();
        boolean fullEvaluation = requiresFullEvaluation(criteria);

        if (fullEvaluation) {
            statement.append("SELECT doc FROM hawkular_btm.completiontimes");
        } else {
            statement.append("SELECT count(*) FROM hawkular_btm.completiontimes");
        }
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" AND fault = '' ALLOW FILTERING;");

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Query statement = " + statement.toString());
        }

        long queryTime = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        ResultSet results = getClient().getSession().execute(statement.toString());

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results returned");
        }

        long nofault = 0;
        if (fullEvaluation) {
            for (Row row : results) {
                try {
                    CompletionTime ct = mapper.readValue(row.getString("doc"), CompletionTime.class);
                    if (!exclude(ct, criteria)) {
                        nofault++;
                    }
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }
        } else {
            nofault = results.one().getLong(0);
        }

        long count = getCompletionCount(tenantId, criteria);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Fault count: total count = " + count + " nofault count = " + nofault);
        }

        long ret = count - nofault;

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms");
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionPercentiles(java.lang.String,
     *                          org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public Percentiles getCompletionPercentiles(String tenantId, Criteria criteria) {
        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.completiontimes");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Query statement = " + statement.toString());
        }

        ResultSet results = getClient().getSession().execute(statement.toString());
        double average = 0;
        long count = 0;

        for (Row row : results) {
            try {
                CompletionTime ct = mapper.readValue(row.getString("doc"), CompletionTime.class);
                if (!exclude(ct, criteria)) {
                    // Incremental averaging
                    average = ((average * count) + ct.getDuration()) /
                            (count + 1);
                    count++;
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        // TODO: Need to work out percentile - also change to request percentile of interest
        Percentiles ret = new Percentiles();
        ret.getPercentiles().put(95, average);
        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionTimeseriesStatistics(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria, long)
     */
    @Override
    public List<CompletionTimeseriesStatistics> getCompletionTimeseriesStatistics(String tenantId,
            Criteria criteria, long interval) {
        // Calculate the number of entries required
        long endTime = criteria.calculateEndTime();

        // Calculate a stable start time, based on the interval
        long startTime = CassandraServiceUtil.getBaseTimestamp(criteria.calculateStartTime(), interval, 0);
        criteria.setStartTime(startTime);

        long timeDiff = endTime - startTime;

        int numberOfEntries = Math.round(timeDiff / interval);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("NumberOfEntries=" + numberOfEntries + " startTime=" + startTime + " endTime=" + endTime +
                    " timeDiff=" + timeDiff + " interval=" + interval);
        }

        // NOTE: Add a couple of entries, as if endTime is 0 then will not be included in
        // Cassandra where clause, which means nodes that occur after the query is issued may
        // be returned - which would lead to an index not found exception. So additional
        // slots created just in case.
        CompletionTimeseriesStatistics[] stats = new CompletionTimeseriesStatistics[numberOfEntries + 3];

        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.completiontimes");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Query statement = " + statement.toString());
        }

        long queryTime = 0;
        int numOfNodes = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        ResultSet results = getClient().getSession().execute(statement.toString());

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results returned");
        }

        for (Row row : results) {
            if (log.isLoggable(Level.FINEST)) {
                numOfNodes++;
            }

            try {
                CompletionTime ct = mapper.readValue(row.getString("doc"), CompletionTime.class);
                if (!exclude(ct, criteria)) {
                    int index = CassandraServiceUtil.getPosition(startTime, interval, ct.getTimestamp());
                    if (stats[index] == null) {
                        stats[index] = new CompletionTimeseriesStatistics();
                        stats[index].setTimestamp(CassandraServiceUtil.getBaseTimestamp(startTime, interval, index));
                        stats[index].setMin(Double.MAX_VALUE);
                    }

                    // Incremental averaging
                    stats[index].setAverage(((stats[index].getAverage() * stats[index].getCount()) + ct.getDuration())
                            /
                            (stats[index].getCount() + 1));
                    stats[index].setCount(stats[index].getCount() + 1);

                    if (ct.getFault() != null && ct.getFault().trim().length() > 0) {
                        stats[index].setFaultCount(stats[index].getFaultCount() + 1);
                    }

                    if (ct.getDuration() > stats[index].getMax()) {
                        stats[index].setMax(ct.getDuration());
                    }

                    if (ct.getDuration() < stats[index].getMin()) {
                        stats[index].setMin(ct.getDuration());
                    }
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms and "
                    + "number of nodes processed = " + numOfNodes);
        }

        // Iterate through list removing null entries
        List<CompletionTimeseriesStatistics> ret = new ArrayList<CompletionTimeseriesStatistics>();

        for (int i = 0; i < stats.length; i++) {
            if (stats[i] != null) {
                ret.add(stats[i]);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionFaultDetails(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public List<Cardinality> getCompletionFaultDetails(String tenantId, Criteria criteria) {
        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.completiontimes");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Query statement = " + statement.toString());
        }

        long queryTime = 0;
        int numOfNodes = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        ResultSet results = getClient().getSession().execute(statement.toString());

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results returned");
        }

        Map<String, Cardinality> cards = new HashMap<String, Cardinality>();

        for (Row row : results) {
            try {
                CompletionTime ct = mapper.readValue(row.getString("doc"), CompletionTime.class);
                if (!exclude(ct, criteria)) {

                    if (ct.getFault() != null && ct.getFault().trim().length() > 0) {
                        Cardinality card = cards.get(ct.getFault());
                        if (card == null) {
                            card = new Cardinality();
                            card.setValue(ct.getFault());
                            cards.put(ct.getFault(), card);
                        }
                        card.setCount(card.getCount() + 1);
                    }
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms and "
                    + "number of nodes processed = " + numOfNodes);
        }

        List<Cardinality> ret = new ArrayList<Cardinality>(cards.values());

        Collections.sort(ret, new Comparator<Cardinality>() {
            @Override
            public int compare(Cardinality arg0, Cardinality arg1) {
                return (int) (arg1.getCount() - arg0.getCount());
            }
        });

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionPropertyDetails(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria, java.lang.String)
     */
    @Override
    public List<Cardinality> getCompletionPropertyDetails(String tenantId, Criteria criteria,
            String property) {
        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.completiontimes");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Query statement = " + statement.toString());
        }

        long queryTime = 0;
        int numOfNodes = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        ResultSet results = getClient().getSession().execute(statement.toString());

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results returned");
        }

        Map<String, Cardinality> cards = new HashMap<String, Cardinality>();

        for (Row row : results) {
            try {
                CompletionTime ct = mapper.readValue(row.getString("doc"), CompletionTime.class);
                if (!exclude(ct, criteria)) {

                    if (ct.getProperties().containsKey(property)) {
                        String value = ct.getProperties().get(property);
                        Cardinality card = cards.get(value);
                        if (card == null) {
                            card = new Cardinality();
                            card.setValue(value);
                            cards.put(value, card);
                        }
                        card.setCount(card.getCount() + 1);
                    }
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms and "
                    + "number of nodes processed = " + numOfNodes);
        }

        List<Cardinality> ret = new ArrayList<Cardinality>(cards.values());

        Collections.sort(ret, new Comparator<Cardinality>() {
            @Override
            public int compare(Cardinality arg0, Cardinality arg1) {
                return arg0.getValue().compareTo(arg1.getValue());
            }
        });

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getAlertCount(java.lang.String, java.lang.String)
     */
    @Override
    public int getAlertCount(String tenantId, String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getNodeTimeseriesStatistics(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria, long)
     */
    @Override
    public List<NodeTimeseriesStatistics> getNodeTimeseriesStatistics(String tenantId, Criteria criteria,
            long interval) {
        // Calculate the number of entries required
        long endTime = criteria.calculateEndTime();

        // Calculate a stable start time, based on the interval
        long startTime = CassandraServiceUtil.getBaseTimestamp(criteria.calculateStartTime(), interval, 0);
        criteria.setStartTime(startTime);

        long timeDiff = endTime - startTime;

        int numberOfEntries = Math.round(timeDiff / interval);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("NumberOfEntries=" + numberOfEntries + " startTime=" + startTime + " endTime=" + endTime +
                    " timeDiff=" + timeDiff + " interval=" + interval);
        }

        // NOTE: Add a couple of entries, as if endTime is 0 then will not be included in
        // Cassandra where clause, which means nodes that occur after the query is issued may
        // be returned - which would lead to an index not found exception. So additional
        // slots created just in case.
        NodeTimeseriesStatistics[] stats = new NodeTimeseriesStatistics[numberOfEntries + 3];

        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.nodedetails");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Query statement = " + statement.toString());
        }

        long queryTime = 0;
        int numOfNodes = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        ResultSet results = getClient().getSession().execute(statement.toString());

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results returned");
        }

        for (Row row : results) {
            if (log.isLoggable(Level.FINEST)) {
                numOfNodes++;
            }

            try {
                NodeDetails nd = mapper.readValue(row.getString("doc"), NodeDetails.class);
                if (!CassandraServiceUtil.exclude(nd.getProperties(), nd.getFault(), criteria)) {
                    String componentType = null;
                    if (nd.getType() == NodeType.Consumer) {
                        componentType = "consumer";
                    } else if (nd.getType() == NodeType.Producer) {
                        componentType = "producer";
                    } else {
                        componentType = nd.getComponentType();
                    }

                    int index = CassandraServiceUtil.getPosition(startTime, interval, nd.getTimestamp());
                    if (stats[index] == null) {
                        stats[index] = new NodeTimeseriesStatistics();
                        stats[index].setTimestamp(CassandraServiceUtil.getBaseTimestamp(startTime, interval, index));
                    }

                    NodeComponentTypeStatistics ncts = stats[index].getComponentTypes().get(componentType);
                    if (ncts == null) {
                        ncts = new NodeComponentTypeStatistics();
                        stats[index].getComponentTypes().put(componentType, ncts);
                    }
                    // Incremental averaging
                    ncts.setDuration(((ncts.getDuration() * ncts.getCount()) + nd.getActual()) /
                            (ncts.getCount() + 1));
                    ncts.setCount(ncts.getCount() + 1);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms and "
                    + "number of nodes processed = " + numOfNodes);
        }

        // Iterate through list removing null entries
        List<NodeTimeseriesStatistics> ret = new ArrayList<NodeTimeseriesStatistics>();

        for (int i = 0; i < stats.length; i++) {
            if (stats[i] != null) {
                ret.add(stats[i]);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getNodeSummaryStatistics(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public Collection<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, Criteria criteria) {
        Map<String, NodeSummaryStatistics> intermediate = new HashMap<String, NodeSummaryStatistics>();

        StringBuilder statement = new StringBuilder("SELECT doc FROM hawkular_btm.nodedetails");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(" ALLOW FILTERING;");

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Query statement = " + statement.toString());
        }

        ResultSet results = getClient().getSession().execute(statement.toString());
        for (Row row : results) {
            try {
                NodeDetails nd = mapper.readValue(row.getString("doc"), NodeDetails.class);
                if (!CassandraServiceUtil.exclude(nd.getProperties(), nd.getFault(), criteria)) {
                    String key = nodeDetailsKey(nd);
                    NodeSummaryStatistics summary = intermediate.get(key);
                    if (summary == null) {
                        summary = new NodeSummaryStatistics();
                        if (nd.getType() == NodeType.Consumer) {
                            summary.setComponentType("consumer");
                        } else if (nd.getType() == NodeType.Producer) {
                            summary.setComponentType("producer");
                        } else {
                            summary.setComponentType(nd.getComponentType());
                        }
                        summary.setUri(nd.getUri());
                        summary.setOperation(nd.getOperation());
                        intermediate.put(key, summary);
                    }
                    // Incremental averaging
                    summary.setActual(((summary.getActual() * summary.getCount()) + nd.getActual()) /
                            (summary.getCount() + 1));
                    summary.setElapsed(((summary.getElapsed() * summary.getCount()) + nd.getElapsed()) /
                            (summary.getCount() + 1));
                    summary.setCount(summary.getCount() + 1);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return intermediate.values();
    }

    protected static String nodeDetailsKey(NodeDetails nd) {
        return nd.getType() + ":" + nd.getComponentType() + ":" + nd.getUri() + ":" + nd.getOperation();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCommunicationSummaryStatistics(java.lang.String,
     *                          org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public Collection<CommunicationSummaryStatistics> getCommunicationSummaryStatistics(String tenantId,
            Criteria criteria) {

        // TODO HWKBTM-324

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeCommunicationDetails(java.lang.String, java.util.List)
     */
    @Override
    public void storeCommunicationDetails(String tenantId, List<CommunicationDetails> communicationDetails)
            throws Exception {

        // TODO HWKBTM-324

    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeNodeDetails(java.lang.String, java.util.List)
     */
    @Override
    public void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws Exception {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Store node details (tenantId=" + tenantId + "):" + nodeDetails);
        }

        BatchStatement batch = new BatchStatement();

        for (int i = 0; i < nodeDetails.size(); i++) {
            NodeDetails details = nodeDetails.get(i);
            batch.add(new BoundStatement(insertNodeDetails).bind(
                    CassandraServiceUtil.tenant(tenantId),
                    new Date(details.getTimestamp()),
                    UUID.randomUUID(),
                    details.getId(),
                    details.getBusinessTransaction(),
                    details.getUri(),
                    details.getComponentType(),
                    details.getOperation(),
                    details.getFault(),
                    details.getHostName(),
                    CassandraServiceUtil.toTagList(details.getProperties()),
                    CassandraServiceUtil.toTagList(details.getDetails()),
                    CassandraServiceUtil.toTagList(details.getCorrelationIds()),
                    mapper.writeValueAsString(details)));
        }

        getClient().getSession().execute(batch);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeCompletionTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeBTxnCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Store completion times (tenantId=" + tenantId + "):" + completionTimes);
        }

        BatchStatement batch = new BatchStatement();

        for (int i = 0; i < completionTimes.size(); i++) {
            CompletionTime completionTime = completionTimes.get(i);
            batch.add(new BoundStatement(insertCompletionTimes).bind(
                    CassandraServiceUtil.tenant(tenantId),
                    new Date(completionTime.getTimestamp()),
                    UUID.randomUUID(),
                    completionTime.getId(),
                    completionTime.getBusinessTransaction(),
                    valueOrEmptyString(completionTime.getFault()),
                    CassandraServiceUtil.toTagList(completionTime.getProperties()),
                    mapper.writeValueAsString(completionTime)));
        }

        getClient().getSession().execute(batch);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeFragmentCompletionTimes(java.lang.String,
     *                          java.util.List)
     */
    @Override
    public void storeFragmentCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception {

        // TODO HWKBTM-324

    }

    protected String valueOrEmptyString(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getHostNames(java.lang.String,
     *                  org.hawkular.btm.api.services.BaseCriteria)
     */
    @Override
    public List<String> getHostNames(String tenantId, Criteria criteria) {
        List<String> ret = new ArrayList<String>();

        StringBuilder statement = new StringBuilder("SELECT hostName FROM hawkular_btm.businesstransactions");
        statement.append(CassandraServiceUtil.whereClause(tenantId, criteria));
        statement.append(";");

        ResultSet results = getClient().getSession().execute(statement.toString());
        for (Row row : results) {
            String hostName = row.getString("hostName");
            if (!ret.contains(hostName)) {
                ret.add(hostName);
            }
        }

        Collections.sort(ret);

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        client.getSession().execute(new BoundStatement(deleteCompletionTimes).bind(
                CassandraServiceUtil.tenant(tenantId)));
        client.getSession().execute(new BoundStatement(deleteNodeDetails).bind(
                CassandraServiceUtil.tenant(tenantId)));
    }

}
