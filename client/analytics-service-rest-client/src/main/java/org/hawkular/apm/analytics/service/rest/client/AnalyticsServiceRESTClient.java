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
package org.hawkular.apm.analytics.service.rest.client;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.EndpointInfo;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.Percentiles;
import org.hawkular.apm.api.model.analytics.PropertyInfo;
import org.hawkular.apm.api.model.analytics.TimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.TransactionInfo;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.services.AnalyticsService;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.rest.AbstractRESTClient;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * This class provides the REST client implementation for the Analytics Service
 * API.
 *
 * @author gbrown
 */
public class AnalyticsServiceRESTClient extends AbstractRESTClient implements AnalyticsService {
    private static final Logger log = Logger.getLogger(AnalyticsServiceRESTClient.class.getName());

    private static final TypeReference<java.util.List<EndpointInfo>> URIINFO_LIST =
            new TypeReference<java.util.List<EndpointInfo>>() {
            };

    private static final TypeReference<java.util.Set<String>> STRING_SET =
            new TypeReference<java.util.Set<String>>() {
            };

    private static final TypeReference<Long> LONG =
            new TypeReference<Long>() {
            };

    private static final TypeReference<java.util.List<TimeseriesStatistics>> TIMESERIES_STATISTICS_LIST =
            new TypeReference<java.util.List<TimeseriesStatistics>>() {
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

    private static final TypeReference<java.util.List<TransactionInfo>> TRANSACTION_INFO_LIST =
            new TypeReference<java.util.List<TransactionInfo>>() {
            };

    private static final TypeReference<java.util.List<PropertyInfo>> PROPERTY_INFO_LIST =
            new TypeReference<java.util.List<PropertyInfo>>() {
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

    public AnalyticsServiceRESTClient(String username, String password, String url) {
        super(username, password, url);
    }

    @Override
    public List<EndpointInfo> getUnboundEndpoints(String tenantId, long startTime, long endTime, boolean compress) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get unbound endpoints: tenantId=[" + tenantId + "] startTime=" + startTime
                    + " endTime=" + endTime + " compress=" + compress);
        }

        String path = "analytics/unboundendpoints?startTime=%d&endTime=%d&compress=%b";
        return getResultsForUrl(tenantId, URIINFO_LIST, path, startTime, endTime, compress);
    }

    @Override
    public List<EndpointInfo> getBoundEndpoints(String tenantId, String transaction, long startTime,
                                                long endTime) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get bound endpoints: tenantId=[" + tenantId + "] transaction="
                    + transaction + " startTime=" + startTime + " endTime=" + endTime);
        }

        String path = "analytics/boundendpoints/%s?startTime=%d&endTime=%d";
        return getResultsForUrl(tenantId, URIINFO_LIST, path, transaction, startTime, endTime);
    }

    @Override
    public List<TransactionInfo> getTransactionInfo(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get transaction info: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        String path = "analytics/transactions?criteria=%s";
        return getResultsForUrl(tenantId, TRANSACTION_INFO_LIST, path, criteria);
    }

    @Override
    public List<PropertyInfo> getPropertyInfo(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get property info: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        String path = "analytics/properties?criteria=%s";
        return getResultsForUrl(tenantId, PROPERTY_INFO_LIST, path, criteria);
    }

    @Override
    public long getTraceCompletionCount(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion count: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/trace/completion/count?criteria=%s";
        return getResultsForUrl(tenantId, LONG, path, criteria);
    }

    @Override
    public long getTraceCompletionFaultCount(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion fault count: tenantId=[" + tenantId + "] criteria=" + criteria);
        }

        String path = "analytics/trace/completion/faultcount?criteria=%s";
        return getResultsForUrl(tenantId, LONG, path, criteria);
    }

    @Override
    public List<CompletionTime> getTraceCompletions(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion times: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/trace/completion/times?criteria=%s";
        return getResultsForUrl(tenantId, COMPLETION_TIME_LIST, path, criteria);
    }

    @Override
    public Percentiles getTraceCompletionPercentiles(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion percentiles: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/trace/completion/percentiles?criteria=%s";
        return getResultsForUrl(tenantId, PERCENTILES_TYPE_REFERENCE, path, criteria);
    }

    @Override
    public List<TimeseriesStatistics> getTraceCompletionTimeseriesStatistics(String tenantId,
                                                                                       Criteria criteria,
                                                                                       long interval) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria + " interval=" + interval);
        }

        String path = "analytics/trace/completion/statistics?criteria=%s&interval=%d";
        return getResultsForUrl(tenantId, TIMESERIES_STATISTICS_LIST, path, criteria, interval);
    }

    @Override
    public List<Cardinality> getTraceCompletionFaultDetails(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get completion fault details: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/trace/completion/faults?criteria=%s";
        return getResultsForUrl(tenantId, CARDINALITY_LIST, path, criteria);
    }

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

    @Override
    public Collection<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get node summary statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/node/summary?criteria=%s";
        return getResultsForUrl(tenantId, NODE_SUMMARY_STATISTICS_LIST, path, criteria);
    }

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

    @Override
    public void storeCommunicationDetails(String tenantId, List<CommunicationDetails> communicationDetails)
            throws StoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws StoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeTraceCompletions(String tenantId, List<CompletionTime> completionTimes) throws StoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TimeseriesStatistics> getEndpointResponseTimeseriesStatistics(String tenantId,
                                                                                       Criteria criteria,
                                                                                       long interval) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get endpoint response statistics: tenantId=[" + tenantId + "] criteria="
                    + criteria + " interval=" + interval);
        }

        String path = "analytics/endpoint/response/statistics?criteria=%s&interval=%d";
        return getResultsForUrl(tenantId, TIMESERIES_STATISTICS_LIST, path, criteria, interval);
    }

    @Override
    public Set<String> getHostNames(String tenantId, Criteria criteria) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get host names: tenantId=[" + tenantId + "] criteria="
                    + criteria);
        }

        String path = "analytics/hostnames?criteria=%s";
        return getResultsForUrl(tenantId, STRING_SET, path, criteria);
    }

    @Override
    public void clear(String tenantId) {
        clear(tenantId, "analytics");
    }
}
