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
package org.hawkular.btm.btxn.service.rest.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.btxn.publisher.rest.client.BusinessTransactionPublisherRESTClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Business Transaction Service
 * API.
 *
 * @author gbrown
 */
public class BusinessTransactionServiceRESTClient extends BusinessTransactionPublisherRESTClient
implements BusinessTransactionService, BusinessTransactionPublisher {

    private static final Logger log = Logger.getLogger(BusinessTransactionServiceRESTClient.class.getName());

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction: tenantId=[" + tenantId + "] id=[" + id + "]");
        }

        try {
            URL url = new URL(getBaseUrl() + "fragments/" + id);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.InputStream is = connection.getInputStream();

            byte[] b = new byte[is.available()];

            is.read(b);

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + new String(b) + "]");
                }
                try {
                    return mapper.readValue(b, BusinessTransaction.class);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to deserialize", t);
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get business transaction: status=[" + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'get' business transaction request", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction fragments: tenantId=[" + tenantId + "] query=[" + criteria + "]");
        }

        try {
            URL url = new URL(getQueryURL(criteria));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            java.io.InputStream is = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder builder = new StringBuilder();
            String str = null;

            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + builder.toString() + "]");
                }
                try {
                    return mapper.readValue(builder.toString(), BUSINESS_TXN_LIST);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to deserialize", t);
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to query business transaction fragment: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'query' business transaction request", e);
        }

        return null;
    }

    /**
     * This method returns a query URL associated with the supplied
     * criteria.
     *
     * @param criteria The criteria
     * @return The query URL
     */
    protected String getQueryURL(Criteria criteria) {
        Map<String, String> queryParams = criteria.parameters();

        StringBuilder builder = new StringBuilder().append(getBaseUrl()).append("fragments");

        if (!queryParams.isEmpty()) {
            builder.append('?');

            boolean first = true;
            for (String key : queryParams.keySet()) {
                if (!first) {
                    builder.append('&');
                }
                String value = queryParams.get(key);
                builder.append(key);
                builder.append('=');
                builder.append(value);
                first = false;
            }
        }

        return builder.toString();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#storeBusinessTransactions(java.lang.String,
     *                              java.util.List)
     */
    @Override
    public void storeBusinessTransactions(String tenantId, List<BusinessTransaction> businessTransactions)
            throws Exception {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Clear business transaction fragments: tenantId=[" + tenantId + "]");
        }

        try {
            URL url = new URL(new StringBuilder().append(getBaseUrl()).append("fragments").toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("DELETE");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            addHeaders(connection, tenantId);

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Business transaction fragments cleared");
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to clear business transaction fragments: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'query' business transaction request", e);
        }
    }
}
