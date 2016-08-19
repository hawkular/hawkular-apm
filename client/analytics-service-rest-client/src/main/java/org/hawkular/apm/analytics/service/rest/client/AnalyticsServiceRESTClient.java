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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.hawkular.apm.client.api.rest.AbstractRESTClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Analytics Service
 * API.
 *
 * @author gbrown
 */
public class AnalyticsServiceRESTClient extends AbstractRESTClient implements AnalyticsService {

    private static final Logger log = Logger.getLogger(AnalyticsServiceRESTClient.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<EndpointInfo>> URIINFO_LIST =
            new TypeReference<java.util.List<EndpointInfo>>() {
            };

    private static final TypeReference<java.util.Set<String>> STRING_SET =
            new TypeReference<java.util.Set<String>>() {
            };

    private static final TypeReference<Long> LONG =
            new TypeReference<Long>() {
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

    private static final TypeReference<java.util.List<CompletionTime>> COMPLETION_TIME_LIST =
            new TypeReference<java.util.List<CompletionTime>>() {
            };

    private static final TypeReference<Percentiles> PERCENTILES_TYPE_REFERENCE =
            new TypeReference<Percentiles>() {
            };

    public AnalyticsServiceRESTClient() {
        super(PropertyUtil.HAWKULAR_APM_URI_SERVICES);
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

        String path = "analytics/unboundendpoints?startTime=%d&endTime=%d&compress=%b";
        return getResultsForUrl(tenantId, URIINFO_LIST, path, startTime, endTime, compress);
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

        String path = "analytics/boundendpoints/%s?startTime=%d&endTime=%d";
        return getResultsForUrl(tenantId, URIINFO_LIST, path, businessTransaction, startTime, endTime);
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

        String path = "analytics/properties?criteria=%s";
        return getResultsForUrl(tenantId, PROPERTY_INFO_LIST, path, criteria);
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

        String path = "analytics/principals?criteria=%s";
        return getResultsForUrl(tenantId, PRINCIPAL_INFO_LIST, path, criteria);
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

        String path = "analytics/trace/completion/count?criteria=%s";
        return getResultsForUrl(tenantId, LONG, path, criteria);
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

        String path = "analytics/trace/completion/faultcount?criteria=%s";
        return getResultsForUrl(tenantId, LONG, path, criteria);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getTraceCompletionTimes(java.lang.String, org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<CompletionTime> getTraceCompletionTimes(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion times: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/trace/completion/times?criteria=%s";
        return getResultsForUrl(tenantId, COMPLETION_TIME_LIST, path, criteria);
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

        String path = "analytics/trace/completion/percentiles?criteria=%s";
        return getResultsForUrl(tenantId, PERCENTILES_TYPE_REFERENCE, path, criteria);
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

        String path = "analytics/trace/completion/statistics?criteria=%s&interval=%d";
        return getResultsForUrl(tenantId, COMPLETION_STATISTICS_LIST, path, criteria, interval);
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

        String path = "analytics/trace/completion/faults?criteria=%s";
        return getResultsForUrl(tenantId, CARDINALITY_LIST, path, criteria);
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

        // attention! the second property parameter (2nd parameter to the formatter) is used first
        // and the first parameter to the formatter (criteria) is used second
        String path = "analytics/trace/completion/property/%2$s/?criteria=%1$s";
        return getResultsForUrl(tenantId, CARDINALITY_LIST, path, criteria, property);
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

        if (criteria.parameters().isEmpty()) {
            String path = "analytics/node/statistics?interval=%d";
            return getResultsForUrl(tenantId, NODE_TIMESERIES_STATISTICS_LIST, path, interval);
        } else {
            String path = "analytics/node/statistics?criteria=%s&interval=%d";
            return getResultsForUrl(tenantId, NODE_TIMESERIES_STATISTICS_LIST, path, criteria, interval);
        }
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

        String path = "analytics/node/summary?criteria=%s";
        return getResultsForUrl(tenantId, NODE_SUMMARY_STATISTICS_LIST, path, criteria);
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

        if (criteria.parameters().isEmpty()) {
            String path = "analytics/communication/summary?tree=%b";
            return getResultsForUrl(tenantId, COMMS_SUMMARY_STATISTICS_LIST, path, tree);
        } else {
            String path = "analytics/communication/summary?criteria=%s&tree=%b";
            return getResultsForUrl(tenantId, COMMS_SUMMARY_STATISTICS_LIST, path, criteria, tree);
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
    public Set<String> getHostNames(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get host names: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/hostnames?criteria=%s";
        return getResultsForUrl(tenantId, STRING_SET, path, criteria);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ConfigurationService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Clear analytics: tenantId=[" + tenantId + "]");
        }

        URL url = getUrl("analytics");
        withContext(tenantId, url, (connection) -> {
            try {
                connection.setRequestMethod("DELETE");
                if (connection.getResponseCode() == 200) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Analytics cleared");
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.warning("Failed to clear analytics: status=["
                                + connection.getResponseCode() + "]:"
                                + connection.getResponseMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.log(Level.SEVERE, "Failed to send 'clear' analytics request", e);
            }
            return null;
        });
    }

    private URL getUrl(String path, Object... args) {
        return getUrl(String.format(path, args));
    }

    private URL getUrl(String path) {
        try {
            return new URL(getUri() + "hawkular/apm/" + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T withContext(String tenantId, URL url, Function<HttpURLConnection, T> function) {
        HttpURLConnection connection = null;
        try {
            connection = getConnectionForGetRequest(tenantId, url);
            return function.apply(connection);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private <T, E> T getResultsForUrl(String tenantId, TypeReference<E> typeReference, String path, Object... parameters) {
        return withContext(tenantId, getUrl(path, parameters), (connection) -> {
            try {
                String response = getResponse(connection);

                if (connection.getResponseCode() == 200) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Returned json=[" + response + "]");
                    }
                    if (!response.trim().isEmpty()) {
                        try {
                            return mapper.readValue(response, typeReference);
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, "Failed to deserialize", t);
                        }
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Failed to get results: status=["
                                + connection.getResponseCode() + "]:"
                                + connection.getResponseMessage());
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to get results", e);
            }

            return null;
        });
    }

    private <T, E> T getResultsForUrl(String tenantId, TypeReference<E> typeReference, String path, Criteria criteria) {
        return getResultsForUrl(tenantId, typeReference, path, encodedCriteria(criteria));
    }
    private <T, E> T getResultsForUrl(String tenantId, TypeReference<E> typeReference, String path, Criteria criteria, Object arg) {
        return getResultsForUrl(tenantId, typeReference, path, encodedCriteria(criteria), arg);
    }

    private String encodedCriteria(Criteria criteria) {
        try {
            return URLEncoder.encode(mapper.writeValueAsString(criteria), "UTF-8");
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpURLConnection getConnectionForGetRequest(String tenantId, URL url) throws IOException {
        return getConnectionForRequest(tenantId, url, "GET");
    }

    private HttpURLConnection getConnectionForRequest(String tenantId, URL url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        addHeaders(connection, tenantId);
        return connection;
    }

    private String getResponse(HttpURLConnection connection) throws IOException {
        InputStream is = connection.getInputStream();
        String response;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            response = buffer.lines().collect(Collectors.joining("\n"));
        }
        return response;
    }
}