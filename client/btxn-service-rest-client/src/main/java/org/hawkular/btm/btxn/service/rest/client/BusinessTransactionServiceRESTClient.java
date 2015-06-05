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
package org.hawkular.btm.btxn.service.rest.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Business Transaction Service
 * API.
 *
 * @author gbrown
 */
public class BusinessTransactionServiceRESTClient implements BusinessTransactionService {

    private static final Logger log = Logger.getLogger(BusinessTransactionServiceRESTClient.class.getName());

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String HAWKULAR_PERSONA = "Hawkular-Persona";

    private String username = System.getProperty("hawkular-btm.username");
    private String password = System.getProperty("hawkular-btm.password");

    private String authorization = null;

    private String baseUrl = System.getProperty("hawkular-btm.base-uri");

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;

        // Clear any previously computed authorization string
        this.authorization = null;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;

        // Clear any previously computed authorization string
        this.authorization = null;
    }

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl the baseUrl to set
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#store(java.lang.String, java.util.List)
     */
    @Override
    public void store(String tenantId, List<BusinessTransaction> btxns) throws Exception {
        URL url = new URL(baseUrl + "transactions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type",
                "application/json");

        addHeaders(connection, tenantId);

        java.io.OutputStream os = connection.getOutputStream();

        os.write(mapper.writeValueAsBytes(btxns));

        os.flush();
        os.close();

        int statusCode = connection.getResponseCode();
        if (statusCode != 200) {
            if (log.isLoggable(Level.FINER)) {
                log.finer("Failed to store business transactions: status=[" + statusCode + "]");
            }
            throw new Exception(connection.getResponseMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transaction: tenantId=[" + tenantId + "] id=[" + id + "]");
        }

        try {
            URL url = new URL(baseUrl + "transactions/" + id);
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
     *                      org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get business transactions: tenantId=[" + tenantId + "] query=[" + criteria + "]");
        }

        StringBuilder builder = new StringBuilder().append(baseUrl).append("transactions");

        Map<String, String> queryParams = getQueryParameters(criteria);

        if (!queryParams.isEmpty()) {
            builder.append('?');

            for (String key : queryParams.keySet()) {
                if (builder.length() > 0) {
                    builder.append('&');
                }
                String value = queryParams.get(key);
                builder.append(key);
                builder.append('=');
                builder.append(value);
            }
        }

        try {
            URL url = new URL(builder.toString());
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
                    return mapper.readValue(b, BUSINESS_TXN_LIST);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Failed to deserialize", t);
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to query business transaction: status=[" + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'query' business transaction request", e);
        }

        return null;
    }

    /**
     * Add the header values to the supplied connection.
     *
     * @param connection The connection
     * @param tenantId The optional tenant id
     */
    protected void addHeaders(HttpURLConnection connection, String tenantId) {
        if (tenantId != null) {
            connection.setRequestProperty(HAWKULAR_PERSONA, tenantId);
        }

        if (authorization == null && username != null) {
            String authString = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(authString.getBytes());

            authorization = "Basic " + encoded;
        }

        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
    }

    /**
     * This method returns a query string representing the criteria
     * specified. If a blank criteria is specified, then an empty
     * string will be returned, otherwise the relevant query
     * parameters/values will be defined following an initial '?'
     * character.
     *
     * @param criteria The business transaction criteria
     * @return The query parameters
     */
    protected static Map<String, String> getQueryParameters(BusinessTransactionCriteria criteria) {
        Map<String, String> ret = new HashMap<String, String>();

        if (criteria.getStartTime() > 0) {
            ret.put("startTime", "" + criteria.getStartTime());
        }

        if (criteria.getEndTime() > 0) {
            ret.put("endTime", "" + criteria.getEndTime());
        }

        if (!criteria.getProperties().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (String key : criteria.getProperties().keySet()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(key);
                buf.append('|');
                buf.append(criteria.getProperties().get(key));
            }

            ret.put("properties", buf.toString());
        }

        if (!criteria.getCorrelationIds().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (CorrelationIdentifier cid : criteria.getCorrelationIds()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(cid.getScope().name());
                buf.append('|');
                buf.append(cid.getValue());
            }

            ret.put("correlations", buf.toString());
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Criteria [" + criteria + "] query parameters [" + ret + "]");
        }

        return ret;
    }
}
