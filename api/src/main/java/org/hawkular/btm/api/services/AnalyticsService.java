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
package org.hawkular.btm.api.services;

import java.util.List;

import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.api.model.events.NodeDetails;

/**
 * This interface represents the available analytics capabilities.
 *
 * @author gbrown
 */
public interface AnalyticsService {

    /**
     * This method returns the unbound URIs (i.e. ones not
     * associated with a business transaction).
     *
     * @param tenantId The optional tenant id
     * @param startTime The start time
     * @param endTime The end time (if 0, then current time)
     * @param compress Whether to compress the list and show common patterns
     * @return The unbound URIs
     */
    List<URIInfo> getUnboundURIs(String tenantId, long startTime, long endTime, boolean compress);

    /**
     * This method returns the bound URIs associated with a business
     * transaction.
     *
     * @param tenantId The optional tenant id
     * @param businessTransaction The business transaction name
     * @param startTime The start time
     * @param endTime The end time (if 0, then current time)
     * @return The bound URIs
     */
    List<String> getBoundURIs(String tenantId, String businessTransaction, long startTime, long endTime);

    /**
     * This method returns the properties
     * associated with a business transaction during the specified
     * time range.
     *
     * @param tenantId The optional tenant id
     * @param businessTransaction The business transaction name
     * @param startTime The start time
     * @param endTime The end time (if 0, then current time)
     * @return The list of property info
     */
    List<PropertyInfo> getPropertyInfo(String tenantId, String businessTransaction, long startTime, long endTime);

    /**
     * This method returns the number of completed transactions, of the specified named
     * business transaction, that were executed during the time range. The business
     * transaction name must be specified as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction count
     */
    long getCompletionCount(String tenantId, CompletionTimeCriteria criteria);

    /**
     * This method returns the number of completed transactions, of the specified named
     * business transaction, that were executed during the time range and returned
     * a fault. The business transaction name must be specified as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction fault count
     */
    long getCompletionFaultCount(String tenantId, CompletionTimeCriteria criteria);

    /**
     * This method returns the completion time percentiles, for the specified criteria, that were
     * executed during the time range. The business transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The completion time percentiles
     */
    Percentiles getCompletionPercentiles(String tenantId, CompletionTimeCriteria criteria);

    /**
     * This method returns the completion timeseries statistics, for the specified criteria, that were
     * executed during the time range. The business transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param interval The aggregation interval (in milliseconds)
     * @return The completion timeseries statistics
     */
    List<CompletionTimeseriesStatistics> getCompletionTimeseriesStatistics(String tenantId,
            CompletionTimeCriteria criteria, long interval);

    /**
     * This method returns the completion time fault details, for the specified criteria, that were
     * executed during the time range. The business transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The completion time fault details
     */
    List<Cardinality> getCompletionFaultDetails(String tenantId, CompletionTimeCriteria criteria);

    /**
     * This method returns the completion time property details, for the specified criteria, that were
     * executed during the time range. The business transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param property The property name
     * @return The completion time property details
     */
    List<Cardinality> getCompletionPropertyDetails(String tenantId, CompletionTimeCriteria criteria,
            String property);

    /**
     * This method returns the number of alerts associated with the specified
     * business transaction.
     *
     * @param tenantId The tenant id
     * @param name The business transaction name
     * @return The number of alerts
     */
    int getAlertCount(String tenantId, String name);

    /**
     * This method returns the node timeseries statistics, for the specified criteria, that were
     * executed during the time range.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @param interval The aggregation interval (in milliseconds)
     * @return The node timeseries statistics
     */
    List<NodeTimeseriesStatistics> getNodeTimeseriesStatistics(String tenantId, NodeCriteria criteria,
            long interval);

    /**
     * This method returns the node summary statistics, for the specified criteria, that were
     * executed during the time range.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The node summary statistics
     */
    List<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, NodeCriteria criteria);

    /**
     * This method stores the supplied list of node details.
     *
     * @param tenantId The tenant id
     * @param nodeDetails The node details
     * @throws Exception Failed to store
     */
    void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws Exception;

    /**
     * This method stores the supplied list of completion times.
     *
     * @param tenantId The tenant id
     * @param completionTimes The completion times
     * @throws Exception Failed to store
     */
    void storeCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception;

    /**
     * This method returns the list of host names where activities were executed, subject to the supplied
     * criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The list of host names
     */
    List<String> getHostNames(String tenantId, BaseCriteria criteria);

}
