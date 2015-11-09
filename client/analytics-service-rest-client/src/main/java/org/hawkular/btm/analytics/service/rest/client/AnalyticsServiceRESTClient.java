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
package org.hawkular.btm.analytics.service.rest.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.analytics.CompletionTime;
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.analytics.ResponseTime;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Analytics Service
 * API.
 *
 * @author gbrown
 */
public class AnalyticsServiceRESTClient implements AnalyticsService {

    private static final Logger log = Logger.getLogger(AnalyticsServiceRESTClient.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<URIInfo>> URIINFO_LIST =
            new TypeReference<java.util.List<URIInfo>>() {
            };

    private static final TypeReference<java.util.List<String>> STRING_LIST =
            new TypeReference<java.util.List<String>>() {
            };

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
     * @see org.hawkular.btm.api.services.AnalyticsService#getUnboundURIs(java.lang.String, long, long)
     */
    @Override
    public List<URIInfo> getUnboundURIs(String tenantId, long startTime, long endTime) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get unbound URIs: tenantId=[" + tenantId + "] startTime=" + startTime + " endTime=" + endTime);
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("analytics/businesstxn/unbounduris?startTime=")
                .append(startTime)
                .append("&endTime=")
                .append(endTime);

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

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), URIINFO_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get unbound URIs: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get unbound URIs", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getBoundURIs(java.lang.String, java.lang.String, long, long)
     */
    @Override
    public List<String> getBoundURIs(String tenantId, String businessTransaction, long startTime, long endTime) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get bound URIs: tenantId=[" + tenantId + "] businessTransaction="
                    + businessTransaction + " startTime=" + startTime + " endTime=" + endTime);
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("analytics/businesstxn/bounduris/")
                .append(businessTransaction)
                .append("?startTime=")
                .append(startTime)
                .append("&endTime=")
                .append(endTime);

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

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), STRING_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get unbound URIs: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get unbound URIs", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionCount(java.lang.String,
     *                          org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public long getCompletionCount(String tenantId, BusinessTransactionCriteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion count: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("analytics/businesstxn/completion/count");

        buildQueryString(builder, criteria);

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

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return Long.parseLong(resp.toString());
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get completion count: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get completion count", e);
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionFaultCount(java.lang.String,
     *                      org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public long getCompletionFaultCount(String tenantId, BusinessTransactionCriteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion fault count: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("analytics/businesstxn/completion/faultcount");

        buildQueryString(builder, criteria);

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

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return Long.parseLong(resp.toString());
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get completion fault count: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get completion fault count", e);
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionPercentiles(java.lang.String,
     *                      org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public Percentiles getCompletionPercentiles(String tenantId, BusinessTransactionCriteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion percentiles: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("analytics/businesstxn/completion/percentiles");

        buildQueryString(builder, criteria);

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

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return mapper.readValue(resp.toString(), Percentiles.class);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get completion percentiles: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get completion percentiles", e);
        }

        return null;
    }

    /**
     * This method builds the URL query string based on the supplied criteria.
     *
     * @param builder The url
     * @param criteria The criteria
     */
    protected void buildQueryString(StringBuilder builder, BusinessTransactionCriteria criteria) {
        Map<String, String> queryParams = criteria.parameters();

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
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getAlertCount(java.lang.String, java.lang.String)
     */
    @Override
    public int getAlertCount(String tenantId, String name) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get alert count: tenantId=[" + tenantId + "] name=" + name);
        }

        StringBuilder builder = new StringBuilder()
                .append(baseUrl)
                .append("analytics/alerts/count/")
                .append(name);

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

            StringBuilder resp = new StringBuilder();
            byte[] b = new byte[10000];

            while (true) {
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                resp.append(new String(b, 0, len));
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Returned json=[" + resp.toString() + "]");
                }
                if (resp.toString().trim().length() > 0) {
                    try {
                        return Integer.parseInt(resp.toString());
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get alert count: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get alert count", e);
        }

        return 0;
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

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeResponseTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeResponseTimes(String tenantId, List<ResponseTime> responseTimes) throws Exception {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeCompletionTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception {
        throw new UnsupportedOperationException();
    }

}
