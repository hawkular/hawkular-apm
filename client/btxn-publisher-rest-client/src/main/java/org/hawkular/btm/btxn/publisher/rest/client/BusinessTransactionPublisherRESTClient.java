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
package org.hawkular.btm.btxn.publisher.rest.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Business Transaction Publisher
 * API.
 *
 * @author gbrown
 */
public class BusinessTransactionPublisherRESTClient implements BusinessTransactionPublisher {

    private static final Logger log = Logger.getLogger(BusinessTransactionPublisherRESTClient.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String HAWKULAR_PERSONA = "Hawkular-Persona";

    private String username = System.getProperty("hawkular-btm.username");
    private String password = System.getProperty("hawkular-btm.password");

    private String authorization = null;

    private String baseUrl;

    {
        baseUrl = System.getProperty("hawkular-btm.base-uri");

        if (baseUrl != null && baseUrl.length() > 0 && baseUrl.charAt(baseUrl.length() - 1) != '/') {
            baseUrl = baseUrl + '/';
        }
    }

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
     * @see org.hawkular.btm.api.services.Publisher#getInitialRetryCount()
     */
    @Override
    public int getInitialRetryCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionPublisher#publish(java.lang.String, java.util.List)
     */
    @Override
    public void publish(String tenantId, List<BusinessTransaction> btxns) throws Exception {

        URL url = new URL(baseUrl + "fragments");

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Publish btxns [tenant=" + tenantId + "][url=" + url + "]: " + btxns);
        }

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

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Status code is: " + statusCode);
        }

        if (statusCode != 200) {
            if (log.isLoggable(Level.FINER)) {
                log.finer("Failed to publish business transaction fragments: status=[" + statusCode + "]");
            }
            throw new Exception(connection.getResponseMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.Publisher#publish(java.lang.String, java.util.List, int, long)
     */
    @Override
    public void publish(String tenantId, List<BusinessTransaction> items, int retryCount, long delay)
                            throws Exception {
        throw new java.lang.UnsupportedOperationException("Cannot set the retry count and delay");
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

}
