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
package org.hawkular.apm.analytics.service.rest.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.EndpointInfo;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.Percentiles;
import org.hawkular.apm.api.model.analytics.PrincipalInfo;
import org.hawkular.apm.api.model.analytics.PropertyInfo;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.services.AnalyticsService;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.utils.PropertyUtil;

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

    private static final TypeReference<java.util.List<EndpointInfo>> URIINFO_LIST =
            new TypeReference<java.util.List<EndpointInfo>>() {
            };

    private static final TypeReference<java.util.List<String>> STRING_LIST =
            new TypeReference<java.util.List<String>>() {
            };

    private static final TypeReference<java.util.List<CompletionTimeseriesStatistics>> COMPLETION_STATISTICS_LIST =
            new TypeReference<java.util.List<CompletionTimeseriesStatistics>>() {
            };

    private static final TypeReference<java.util.List<NodeTimeseriesStatistics>> NODE_TIMESERIES_STATISTICS_LIST =
            new TypeReference<java.util.List<NodeTimeseriesStatistics>>() {
            };

    private static final TypeReference<java.util.List<NodeSummaryStatistics>> NODE_SUMMARY_STATISTICS_LIST =
            new TypeReference<java.util.List<NodeSummaryStatistics>>() {
            };

    private static final TypeReference<java.util.List<CommunicationSummaryStatistics>> COMMS_SUMMARY_STATISTICS_LIST =
            new TypeReference<java.util.List<CommunicationSummaryStatistics>>() {
            };

    private static final TypeReference<java.util.List<Cardinality>> CARDINALITY_LIST =
            new TypeReference<java.util.List<Cardinality>>() {
            };

    private static final TypeReference<java.util.List<PropertyInfo>> PROPERTY_INFO_LIST =
            new TypeReference<java.util.List<PropertyInfo>>() {
            };

    private static final TypeReference<java.util.List<PrincipalInfo>> PRINCIPAL_INFO_LIST =
            new TypeReference<java.util.List<PrincipalInfo>>() {
            };

    private static final String HAWKULAR_TENANT = "Hawkular-Tenant";

    private String username = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_USERNAME);
    private String password = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_PASSWORD);

    private String authorization = null;

    private String uri;

    {
        uri = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI);

        if (uri != null && !uri.isEmpty() && uri.charAt(uri.length() - 1) != '/') {
            uri = uri + '/';
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
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getUnboundEndpoints(java.lang.String, long, long, boolean)
     */
    @Override
    public List<EndpointInfo> getUnboundEndpoints(String tenantId, long startTime, long endTime, boolean compress) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get unbound endpoints: tenantId=[" + tenantId + "] startTime=" + startTime
                    + " endTime=" + endTime + " compress=" + compress);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/unboundendpoints?startTime=")
                .append(startTime)
                .append("&endTime=")
                .append(endTime)
                .append("&compress=")
                .append(compress);

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), URIINFO_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get unbound endpoints: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get unbound endpoints", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getBoundEndpoints(java.lang.String, java.lang.String,
     *                                  long, long)
     */
    @Override
    public List<EndpointInfo> getBoundEndpoints(String tenantId, String businessTransaction, long startTime,
                                    long endTime) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get bound endpoints: tenantId=[" + tenantId + "] businessTransaction="
                    + businessTransaction + " startTime=" + startTime + " endTime=" + endTime);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/boundendpoints/")
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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), URIINFO_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get bound endpoints: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get bound endpoints", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getPropertyInfo(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<PropertyInfo> getPropertyInfo(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get property info: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/properties");

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), PROPERTY_INFO_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get property info: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get property info", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getPrincipalInfo(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<PrincipalInfo> getPrincipalInfo(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get principal info: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/principals");

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), PRINCIPAL_INFO_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get principal info: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get principal info", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionCount(java.lang.String,
     *                          org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public long getTraceCompletionCount(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion count: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/trace/completion/count");

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
                if (!resp.toString().trim().isEmpty()) {
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
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionFaultCount(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public long getTraceCompletionFaultCount(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion fault count: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/trace/completion/faultcount");

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
                if (!resp.toString().trim().isEmpty()) {
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
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionPercentiles(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public Percentiles getTraceCompletionPercentiles(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion percentiles: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/trace/completion/percentiles");

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
                if (!resp.toString().trim().isEmpty()) {
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

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionStatistics(java.lang.String,
     *                  org.hawkular.apm.api.services.Criteria, long)
     */
    @Override
    public List<CompletionTimeseriesStatistics> getTraceCompletionTimeseriesStatistics(String tenantId,
            Criteria criteria,
            long interval) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria + " interval=" + interval);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/trace/completion/statistics");

        buildQueryString(builder, criteria);

        builder.append("&interval=");
        builder.append(interval);

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), COMPLETION_STATISTICS_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get completion statistics: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get completion statistics", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionFaultDetails(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<Cardinality> getTraceCompletionFaultDetails(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion fault details: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/trace/completion/faults");

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), CARDINALITY_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get completion fault details: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get completion fault details", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionPropertyDetails(java.lang.String,
     *              org.hawkular.apm.api.services.Criteria, java.lang.String)
     */
    @Override
    public List<Cardinality> getTraceCompletionPropertyDetails(String tenantId, Criteria criteria,
            String property) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion property details: tenantId=[" + tenantId + "] criteria="
                    + criteria + " property=" + property);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/trace/completion/property/")
                .append(property);

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), CARDINALITY_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get completion property details: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get completion property details", e);
        }

        return null;
    }

    /**
     * This method builds the URL query string based on the supplied criteria.
     *
     * @param builder The url
     * @param criteria The criteria
     */
    protected boolean buildQueryString(StringBuilder builder, Criteria criteria) {
        Map<String, String> queryParams = criteria.parameters();

        if (!queryParams.isEmpty()) {
            builder.append('?');

            boolean first = true;
            for (Map.Entry<String, String> stringStringEntry : queryParams.entrySet()) {
                if (!first) {
                    builder.append('&');
                }
                String value = stringStringEntry.getValue();
                builder.append(stringStringEntry.getKey());
                builder.append('=');
                builder.append(value);
                first = false;
            }

            return true;
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getNodeStatistics(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria, long)
     */
    @Override
    public List<NodeTimeseriesStatistics> getNodeTimeseriesStatistics(String tenantId,
            Criteria criteria, long interval) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get node timeseries statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria + " interval=" + interval);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/node/statistics");

        if (buildQueryString(builder, criteria)) {
            builder.append('&');
        } else {
            builder.append('?');
        }

        builder.append("interval=");
        builder.append(interval);

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), NODE_TIMESERIES_STATISTICS_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get node timeseries statistics: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get node timeseries statistics", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getNodeSummaryStatistics(java.lang.String,
     *                          org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public Collection<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get node summary statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/node/summary");

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), NODE_SUMMARY_STATISTICS_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get node summary statistics: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get node summary statistics", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCommunicationSummaryStatistics(java.lang.String,
     *                          org.hawkular.apm.api.services.Criteria, boolean)
     */
    @Override
    public Collection<CommunicationSummaryStatistics> getCommunicationSummaryStatistics(String tenantId,
            Criteria criteria, boolean tree) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get communication summary statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria + " as tree? " + tree);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/communication/summary");

        if (buildQueryString(builder, criteria)) {
            builder.append('&');
        } else {
            builder.append('?');
        }

        builder.append("tree=");
        builder.append(tree);

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), COMMS_SUMMARY_STATISTICS_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get communication summary statistics: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get communication summary statistics", e);
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
        if (tenantId == null) {
            // Check if default tenant provided as property
            tenantId = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_TENANT);
        }

        if (tenantId != null) {
            connection.setRequestProperty(HAWKULAR_TENANT, tenantId);
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
     * @see org.hawkular.apm.api.services.AnalyticsService#storeCommunicationDetails(java.lang.String, java.util.List)
     */
    @Override
    public void storeCommunicationDetails(String tenantId, List<CommunicationDetails> communicationDetails)
            throws StoreException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#storeNodeDetails(java.lang.String, java.util.List)
     */
    @Override
    public void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws StoreException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#storeCompletionTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeTraceCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws StoreException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#storeFragmentCompletionTimes(java.lang.String,
     *                      java.util.List)
     */
    @Override
    public void storeFragmentCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws StoreException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getHostNames(java.lang.String,
     *                      org.hawkular.apm.api.services.BaseCriteria)
     */
    @Override
    public List<String> getHostNames(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get host names: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        StringBuilder builder = new StringBuilder()
                .append(uri)
                .append("hawkular/apm/analytics/hostnames");

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
                if (!resp.toString().trim().isEmpty()) {
                    try {
                        return mapper.readValue(resp.toString(), STRING_LIST);
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to get host names: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to get host names", e);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Clear analytics: tenantId=[" + tenantId + "]");
        }

        try {
            URL url = new URL(new StringBuilder().append(getUri()).append("hawkular/apm/analytics").toString());
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
                    log.finest("Analytics cleared");
                }
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Failed to clear analytics: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to send 'clear' analytics request", e);
        }
    }

}
