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
package org.hawkular.btm.api.services;

import java.util.List;

import org.hawkular.btm.api.model.analytics.BusinessTransactionStats;
import org.hawkular.btm.api.model.analytics.CompletionTime;

/**
 * This interface represents the available analytics capabilities.
 *
 * @author gbrown
 */
public interface AnalyticsService {

    /**
     * This method returns the list of unbound URIs (i.e. ones not
     * associated with a business transaction).
     *
     * @param tenantId The optional tenant id
     * @param startTime The start time
     * @param endTime The end time (if 0, then current time)
     * @return The list of unbound URIs
     */
    List<String> getUnboundURIs(String tenantId, long startTime, long endTime);

    /**
     * This method returns the number of completed transactions, of the specified named
     * business transaction, that were executed during the time range. The business
     * transaction name must be specified as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction count
     */
    long getCompletionCount(String tenantId, BusinessTransactionCriteria criteria);

    /**
     * This method returns the number of completed transactions, of the specified named
     * business transaction, that were executed during the time range and returned
     * a fault. The business transaction name must be specified as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction fault count
     */
    long getCompletionFaultCount(String tenantId, BusinessTransactionCriteria criteria);

    /**
     * This method returns the statistics, for the specified criteria, that were
     * executed during the time range. The business transaction name must be specified
     * as part of the criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The transaction stats
     */
    BusinessTransactionStats getCompletionStats(String tenantId, BusinessTransactionCriteria criteria);

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
     * This method stores the supplied list of completion times.
     *
     * @param tenantId The tenant id
     * @param completionTimes The completion times
     * @throws Exception Failed to store
     */
    void store(String tenantId, List<CompletionTime> completionTimes) throws Exception;

}
