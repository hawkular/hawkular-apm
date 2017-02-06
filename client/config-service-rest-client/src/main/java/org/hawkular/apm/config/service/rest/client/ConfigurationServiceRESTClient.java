/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.apm.config.service.rest.client;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.hawkular.apm.api.services.ConfigurationLoader;
import org.hawkular.apm.api.services.ConfigurationService;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.rest.AbstractRESTClient;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * This class provides the REST client implementation for the Configuration Service
 * API.
 *
 * @author gbrown
 */
public class ConfigurationServiceRESTClient extends AbstractRESTClient implements ConfigurationService {
    private static final Logger log = Logger.getLogger(ConfigurationServiceRESTClient.class.getName());

    private static final TypeReference<CollectorConfiguration> COLLECTOR_CONFIGURATION_TYPE_REFERENCE = new TypeReference<CollectorConfiguration>() {
    };
    private static final TypeReference<TransactionConfig> TXN_CONFIG_TYPE_REFERENCE = new TypeReference<TransactionConfig>() {
    };
    private static final TypeReference<List<TransactionSummary>> TXN_SUMMARY_LIST = new TypeReference<List<TransactionSummary>>() {
    };
    private static final TypeReference<Map<String, TransactionConfig>> TXN_CONFIG_MAP = new TypeReference<Map<String, TransactionConfig>>() {
    };
    private static final TypeReference<List<ConfigMessage>> CONFIG_MESSAGE_LIST = new TypeReference<List<ConfigMessage>>() {
    };

    public ConfigurationServiceRESTClient() {
        super(PropertyUtil.HAWKULAR_APM_URI_SERVICES);
    }

    public ConfigurationServiceRESTClient(String username, String password, String url) {
        super(username, password, url);
    }

    @Override
    public CollectorConfiguration getCollector(String tenantId, String type, String host, String server) {

        if (PropertyUtil.getProperty(ConfigurationLoader.HAWKULAR_APM_CONFIG) != null) {
            CollectorConfiguration ret = ConfigurationLoader.getConfiguration(type);
            if (log.isLoggable(Logger.Level.FINEST)) {
                try {
                    log.finest("Collector configuration [local] = " + (ret == null ? null : mapper.writeValueAsString(ret)));
                } catch (Throwable t) {
                    log.finest("Collector configuration [local]: failed to serialize as json: " + t);
                }
            }
            return ret;
        }

        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return null;
        }

        StringBuilder parametersBuilder = new StringBuilder();
        if (null != type) parametersBuilder.append("type=").append(type).append("&");
        if (null != host) parametersBuilder.append("host=").append(host).append("&");
        if (null != server) parametersBuilder.append("server=").append(server).append("&");
        String parameters = parametersBuilder.toString();
        if (parameters.length() > 0) {
            parameters = parameters.substring(0, parameters.length()-1);
        }

        String url = "config/collector?" + parameters;
        CollectorConfiguration cc = getResultsForUrl(tenantId, COLLECTOR_CONFIGURATION_TYPE_REFERENCE, url);

        if (cc == null) {
            log.warning("Unable to obtain APM configuration from " + getUrl(url) + ", will retry ...");
        }

        return cc;
    }

    @Override
    public List<ConfigMessage> setTransaction(String tenantId, String name, TransactionConfig config) {
        return withJsonPayloadAndResults(
                "PUT",
                tenantId,
                getUrl("config/transaction/full/%s", name),
                config,
                (connection) -> parseResultsIntoJson(connection, CONFIG_MESSAGE_LIST)
        );
    }

    @Override
    public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs) {
        return withJsonPayloadAndResults(
                "POST",
                tenantId,
                getUrl("config/transaction/full"),
                configs,
                (connection) -> parseResultsIntoJson(connection, CONFIG_MESSAGE_LIST)
        );
    }

    @Override
    public List<ConfigMessage> validateTransaction(TransactionConfig config) {
        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return null;
        }

        return withJsonPayloadAndResults(
                "POST",
                null,
                getUrl("config/transaction/validate"),
                config,
                (connection) -> parseResultsIntoJson(connection, CONFIG_MESSAGE_LIST)
        );
    }

    @Override
    public TransactionConfig getTransaction(String tenantId, String name) {
        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return null;
        }

        String url = "config/transaction/full/%s";
        return getResultsForUrl(tenantId, TXN_CONFIG_TYPE_REFERENCE, url, name);
    }

    @Override
    public List<TransactionSummary> getTransactionSummaries(String tenantId) {
        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return null;
        }

        String url = "config/transaction/summary";
        return getResultsForUrl(tenantId, TXN_SUMMARY_LIST, url);
    }

    @Override
    public Map<String, TransactionConfig> getTransactions(String tenantId, long updated) {
        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return null;
        }

        String url = "config/transaction/full?updated=%d";
        return getResultsForUrl(tenantId, TXN_CONFIG_MAP, url, updated);
    }

    @Override
    public void removeTransaction(String tenantId, String name) {
        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return;
        }

        URL url = getUrl("config/transaction/full/%s", name);
        withContext(tenantId, url, (connection) -> {
            try {
                connection.setRequestMethod("DELETE");
                if (connection.getResponseCode() == 200) {
                    if (log.isLoggable(Logger.Level.FINEST)) {
                        log.finest(String.format("Transaction [%s] removed", name));
                    }
                } else {
                    if (log.isLoggable(Logger.Level.FINEST)) {
                        log.warning("Failed to remove transaction: status=["
                                + connection.getResponseCode() + "]:"
                                + connection.getResponseMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.log(Logger.Level.SEVERE, String.format("Failed to remove transaction [%s]", name), e);
            }
            return null;
        });
    }

    @Override
    public void clear(String tenantId) {
        if (!isAvailable()) {
            if (log.isLoggable(Logger.Level.FINEST)) {
                log.finest("Configuration Service is not enabled");
            }
            return;
        }

        clear(tenantId, "config");
    }
}
