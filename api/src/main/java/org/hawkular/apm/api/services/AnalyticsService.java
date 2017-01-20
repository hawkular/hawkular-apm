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
package org.hawkular.apm.api.services;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

/**
 * This interface represents the available analytics capabilities.
 *
 * @author gbrown
 */
public interface AnalyticsService {

    /**
     * This method returns the unbound endpoints (i.e. ones not
     * associated with a transaction).
     *
     * @param tenantId The optional tenant id
     * @param startTime The start time in milliseconds
     * @param endTime The end time (if 0, then current time) in milliseconds
     * @param compress Whether to compress the list and show common patterns
     * @return The unbound endpoints
     */
    List<EndpointInfo> getUnboundEndpoints(String tenantId, long startTime, long endTime, boolean compress);

    /**
     * This method returns the bound endpoints associated with a
     * transaction.
     *
     * @param tenantId The optional tenant id
     * @param transaction The transaction name
     * @param startTime The start time in milliseconds
     * @param endTime The end time (if 0, then current time) in milliseconds
     * @return The bound endpoints
     */
    List<EndpointInfo> getBoundEndpoints(String tenantId, String transaction, long startTime, long endTime);

    /**
     * This method returns the transactions associated with the specified
     * criteria. Note: the 'transaction' property in the criteria will be
     * ignored for this method.
     *
     * @param tenantId The optional tenant id
     * @param criteria The criteria
     * @return The list of transaction info
     */
    List<TransactionInfo> getTransactionInfo(String tenantId, Criteria criteria);

    /**
     * This method returns the properties associated with the specified
     * criteria.
     *
     * @param tenantId The optional tenant id
     * @param criteria The criteria
     * @return The list of property info
     */
    List<PropertyInfo> getPropertyInfo(String tenantId, Criteria criteria);

    /**
     * This method returns the number of completed transactions, of the specified named
     * trace, that were executed during the time range. The
     * transaction name must be specified as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction count
     */
    long getTraceCompletionCount(String tenantId, Criteria criteria);

    /**
     * This method returns the number of completed transactions, of the specified named
     * trace, that were executed during the time range and returned
     * a fault. The transaction name must be specified as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction fault count
     */
    long getTraceCompletionFaultCount(String tenantId, Criteria criteria);

    /**
     * This method returns the list of trace completion times that meet the supplied
     * criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The list of trace completion times
     */
    List<CompletionTime> getTraceCompletions(String tenantId, Criteria criteria);

    /**
     * This method returns the completion time percentiles, for the specified criteria, that were
     * executed during the time range. The transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The completion time percentiles
     */
    Percentiles getTraceCompletionPercentiles(String tenantId, Criteria criteria);

    /**
     * This method returns the completion timeseries statistics, for the specified criteria, that were
     * executed during the time range. The transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param interval The aggregation interval (in milliseconds)
     * @return The completion timeseries statistics
     */
    List<TimeseriesStatistics> getTraceCompletionTimeseriesStatistics(String tenantId,
            Criteria criteria, long interval);

    /**
     * This method returns the completion time fault details, for the specified criteria, that were
     * executed during the time range. The transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The completion time fault details
     */
    List<Cardinality> getTraceCompletionFaultDetails(String tenantId, Criteria criteria);

    /**
     * This method returns the completion time property details, for the specified criteria, that were
     * executed during the time range. The transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param property The property name
     * @return The completion time property details
     */
    List<Cardinality> getTraceCompletionPropertyDetails(String tenantId, Criteria criteria,
            String property);

    /**
     * This method returns the node timeseries statistics, for the specified criteria, that were
     * executed during the time range.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param interval The aggregation interval (in milliseconds)
     * @return The node timeseries statistics
     */
    List<NodeTimeseriesStatistics> getNodeTimeseriesStatistics(String tenantId, Criteria criteria,
            long interval);

    /**
     * This method returns the node summary statistics, for the specified criteria, that were
     * executed during the time range.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The node summary statistics
     */
    Collection<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, Criteria criteria);

    /**
     * This method returns the communication summary statistics, for the specified criteria, that were
     * executed during the time range. The representation can either be returned as flat list of
     * nodes, each node optionally defning the links that connect them to other nodes, or as pre-built
     * trees.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param asTree Whether to build the nodes and links in a tree
     * @return The communication summary statistics
     */
    Collection<CommunicationSummaryStatistics> getCommunicationSummaryStatistics(String tenantId,
                                Criteria criteria, boolean asTree);

    /**
     * This method returns the endpoint response timeseries statistics, for the specified criteria, that were
     * executed during the time range.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param interval The aggregation interval (in milliseconds)
     * @return The endpoint timeseries statistics
     */
    List<TimeseriesStatistics> getEndpointResponseTimeseriesStatistics(String tenantId,
            Criteria criteria, long interval);

    /**
     * This method returns the list of host names where activities were executed, subject to the supplied
     * criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The list of host names
     */
    Set<String> getHostNames(String tenantId, Criteria criteria);

    /**
     * This method stores the supplied list of node details.
     *
     * @param tenantId The tenant id
     * @param nodeDetails The node details
     * @throws StoreException Failed to store
     */
    void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws StoreException;

    /**
     * This method stores the supplied list of communication details.
     *
     * @param tenantId The tenant id
     * @param communicationDetails The communication details
     * @throws StoreException Failed to store
     */
    void storeCommunicationDetails(String tenantId, List<CommunicationDetails> communicationDetails)
            throws StoreException;

    /**
     * This method stores the supplied list of completion times for end to end traces.
     *
     * @param tenantId The tenant id
     * @param completionTimes The completion times
     * @throws StoreException Failed to store
     */
    void storeTraceCompletions(String tenantId, List<CompletionTime> completionTimes) throws StoreException;

    /**
     * This method clears the analytics data for the specified tenant.
     *
     * @param tenantId The tenant id
     */
    void clear(String tenantId);

}
