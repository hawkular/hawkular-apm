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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.apm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.apm.api.model.config.btxn.ConfigMessage;
import org.hawkular.apm.api.services.AbstractConfigurationService;
import org.hawkular.apm.api.services.ConfigurationLoader;
import org.hawkular.apm.server.cassandra.log.MsgLogger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
@Singleton
public class ConfigurationServiceCassandra extends AbstractConfigurationService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private static ObjectMapper mapper = new ObjectMapper();

    private PreparedStatement getBusinessTxnConfig;
    private PreparedStatement getBusinessTxnConfigInvalid;

    private PreparedStatement getBusinessTxnConfigs;
    private PreparedStatement getBusinessTxnConfigsInvalid;

    private PreparedStatement insertBusinessTxnConfig;
    private PreparedStatement insertBusinessTxnConfigInvalid;

    private PreparedStatement deleteBusinessTxnConfigInvalid;

    private PreparedStatement deleteAllBTxnConfig;
    private PreparedStatement deleteAllBTxnConfigInvalid;

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
        getBusinessTxnConfig = client.getSession().prepare(
                "SELECT doc FROM hawkular_apm.businesstxnconfig "
                        + "WHERE tenantId = ? AND id = ?;");
        getBusinessTxnConfigInvalid = client.getSession().prepare(
                "SELECT doc FROM hawkular_apm.businesstxnconfiginvalid "
                        + "WHERE tenantId = ? AND id = ?;");

        getBusinessTxnConfigs = client.getSession().prepare(
                "SELECT id, doc FROM hawkular_apm.businesstxnconfig "
                        + "WHERE tenantId = ?;");
        getBusinessTxnConfigsInvalid = client.getSession().prepare(
                "SELECT id, doc FROM hawkular_apm.businesstxnconfiginvalid "
                        + "WHERE tenantId = ?;");

        insertBusinessTxnConfig = client.getSession().prepare(
                "INSERT INTO hawkular_apm.businesstxnconfig "
                        + "(tenantId, id, doc) "
                        + "VALUES (?, ?, ?);");
        insertBusinessTxnConfigInvalid = client.getSession().prepare(
                "INSERT INTO hawkular_apm.businesstxnconfiginvalid "
                        + "(tenantId, id, doc) "
                        + "VALUES (?, ?, ?);");

        deleteBusinessTxnConfigInvalid = client.getSession().prepare(
                "DELETE FROM hawkular_apm.businesstxnconfiginvalid "
                        + "WHERE tenantId = ? AND id = ?;");
        deleteAllBTxnConfig = client.getSession().prepare(
                "DELETE FROM hawkular_apm.businesstxnconfig "
                        + "WHERE tenantId = ?;");
        deleteAllBTxnConfigInvalid = client.getSession().prepare(
                "DELETE FROM hawkular_apm.businesstxnconfiginvalid "
                        + "WHERE tenantId = ?;");
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getCollector(java.lang.String,
     *                  java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {
        CollectorConfiguration config = ConfigurationLoader.getConfiguration(type);

        ResultSet results = client.getSession().execute(new BoundStatement(getBusinessTxnConfigs).bind(
                CassandraServiceUtil.tenant(tenantId)));
        for (Row row : results) {
            try {
                BusinessTxnConfig btc = mapper.readValue(row.getString("doc"),
                        BusinessTxnConfig.class);
                if (!btc.isDeleted()) {
                    config.getBusinessTransactions().put(row.getString("id"), btc);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return config;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#setBusinessTransaction(java.lang.String,
     *              java.lang.String, org.hawkular.apm.api.model.config.btxn.BusinessTxnConfig)
     */
    @Override
    public List<ConfigMessage> setBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
            throws Exception {
        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Update business transaction config with name[%s] config=%s", name, config);
        }

        List<ConfigMessage> messages = validateBusinessTransaction(config);

        // Set last updated time
        config.setLastUpdated(System.currentTimeMillis());

        PreparedStatement statement = (messages.isEmpty() ? insertBusinessTxnConfig : insertBusinessTxnConfigInvalid);

        client.getSession().execute(new BoundStatement(statement).bind(
                CassandraServiceUtil.tenant(tenantId),
                name,
                mapper.writeValueAsString(config)));

        if (messages.isEmpty()) {
            // Delete any invalid version
            client.getSession().execute(new BoundStatement(deleteBusinessTxnConfigInvalid).bind(
                    CassandraServiceUtil.tenant(tenantId),
                    name));
        }

        return messages;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getBusinessTransaction(java.lang.String,
     *                          java.lang.String)
     */
    @Override
    public BusinessTxnConfig getBusinessTransaction(String tenantId, String name) {
        ResultSet results = client.getSession().execute(new BoundStatement(getBusinessTxnConfigInvalid).bind(
                CassandraServiceUtil.tenant(tenantId), name));
        Row row = results.one();

        if (row == null) {
            results = client.getSession().execute(new BoundStatement(getBusinessTxnConfig).bind(
                    CassandraServiceUtil.tenant(tenantId), name));
            row = results.one();
        }

        if (row != null) {
            try {
                BusinessTxnConfig btc = mapper.readValue(row.getString("doc"),
                        BusinessTxnConfig.class);
                if (!btc.isDeleted()) {
                    return btc;
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getBusinessTransactions(java.lang.String, long)
     */
    @Override
    public Map<String, BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated) {
        Map<String, BusinessTxnConfig> ret = new HashMap<String, BusinessTxnConfig>();

        ResultSet results = client.getSession().execute(new BoundStatement(getBusinessTxnConfigs).bind(
                CassandraServiceUtil.tenant(tenantId)));
        for (Row row : results) {
            try {
                BusinessTxnConfig btc = mapper.readValue(row.getString("doc"),
                        BusinessTxnConfig.class);
                if ((updated == 0 && !btc.isDeleted()) || (updated > 0 && btc.getLastUpdated() > updated)) {
                    ret.put(row.getString("id"), btc);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#getBusinessTransactionSummaries(java.lang.String)
     */
    @Override
    public List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId) {
        List<BusinessTxnSummary> ret = new ArrayList<BusinessTxnSummary>();

        List<String> names = new ArrayList<String>();

        ResultSet results = client.getSession().execute(new BoundStatement(getBusinessTxnConfigs).bind(
                CassandraServiceUtil.tenant(tenantId)));
        for (Row row : results) {
            try {
                BusinessTxnConfig btc = mapper.readValue(row.getString("doc"),
                        BusinessTxnConfig.class);
                if (!btc.isDeleted()) {
                    BusinessTxnSummary summary = new BusinessTxnSummary();
                    summary.setName(row.getString("id"));
                    summary.setDescription(btc.getDescription());
                    summary.setLevel(btc.getLevel());
                    ret.add(summary);
                    names.add(summary.getName());
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        results = client.getSession().execute(new BoundStatement(getBusinessTxnConfigsInvalid).bind(
                CassandraServiceUtil.tenant(tenantId)));
        for (Row row : results) {
            try {
                BusinessTxnConfig btc = mapper.readValue(row.getString("doc"),
                        BusinessTxnConfig.class);
                String name = row.getString("id");
                if (!names.contains(name)) {
                    BusinessTxnSummary summary = new BusinessTxnSummary();
                    summary.setName(name);
                    summary.setDescription(btc.getDescription());
                    summary.setLevel(btc.getLevel());
                    ret.add(summary);
                }
            } catch (Exception e) {
                msgLog.errorFailedToParse(e);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#removeBusinessTransaction(java.lang.String,
     *                      java.lang.String)
     */
    @Override
    public void removeBusinessTransaction(String tenantId, String name) throws Exception {
        BusinessTxnConfig config = new BusinessTxnConfig();
        config.setDeleted(true);
        config.setLastUpdated(System.currentTimeMillis());

        client.getSession().execute(new BoundStatement(insertBusinessTxnConfig).bind(
                CassandraServiceUtil.tenant(tenantId),
                name,
                mapper.writeValueAsString(config)));

        // Delete any invalid version
        client.getSession().execute(new BoundStatement(deleteBusinessTxnConfigInvalid).bind(
                CassandraServiceUtil.tenant(tenantId),
                name));

        if (msgLog.isTraceEnabled()) {
            msgLog.tracef("Remove business transaction config with name[%s]", name);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        client.getSession().execute(new BoundStatement(deleteAllBTxnConfig).bind(
                CassandraServiceUtil.tenant(tenantId)));
        client.getSession().execute(new BoundStatement(deleteAllBTxnConfigInvalid).bind(
                CassandraServiceUtil.tenant(tenantId)));
    }

}
