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
import java.util.Map;

import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.ConfigMessage;

/**
 * This interface provides the configuration information.
 *
 * @author gbrown
 */
public interface ConfigurationService {

    /**
     * This method returns the collector configuration, used by the
     * collector within an execution environment to instrument and filter
     * information to be reported to the server, based on the optional
     * host and server names. Only valid business configurations will be
     * included in the collector configuration.
     *
     * @param tenantId The optional tenant id
     * @param host The optional host name
     * @param server The optional server name
     * @return The collector configuration
     */
    CollectorConfiguration getCollector(String tenantId, String host, String server);

    /**
     * This method adds (if does not exist) or updates (if exists) a business transaction
     * configuration. If validation errors occur, then the configuration will be held in a
     * staging area until fixed.
     *
     * @param tenantId The optional tenant id
     * @param name The business transaction name
     * @param config The configuration
     * @throws Exception Failed to perform update
     * @return The list of messages resulting from validation of the saved config
     */
    List<ConfigMessage> updateBusinessTransaction(String tenantId, String name, BusinessTxnConfig config)
            throws Exception;

    /**
     * This method validates the supplied business transaction configuration.
     *
     * @param config The configuration
     * @return The list of messages resulting from validation of the supplied config
     */
    List<ConfigMessage> validateBusinessTransaction(BusinessTxnConfig config);

    /**
     * This method retrieves a business transaction configuration. This will retrieve the
     * most recent version of the configuration, regardless of whether it is valid or
     * invalid.
     *
     * @param tenantId The optional tenant id
     * @param name The business transaction name
     * @return The configuration, or null if not found
     */
    BusinessTxnConfig getBusinessTransaction(String tenantId, String name);

    /**
     * This method retrieves the list of valid business transaction configurations updated
     * after the specified time.
     *
     * @param tenantId The optional tenant id
     * @param updated The updated time, or 0 to return all
     * @return The valid business transaction configurations
     */
    Map<String,BusinessTxnConfig> getBusinessTransactions(String tenantId, long updated);

    /**
     * This method retrieves the list of business transaction summaries (regardless of whether
     * the current business transaction config is valid).
     *
     * @param tenantId The optional tenant id
     * @return The list of business transaction summaries
     */
    List<BusinessTxnSummary> getBusinessTransactionSummaries(String tenantId);

    /**
     * This method removes a business transaction configuration.
     *
     * @param tenantId The optional tenant id
     * @param name The business transaction name
     * @throws Exception Failed to perform remove
     */
    void removeBusinessTransaction(String tenantId, String name) throws Exception;

    /**
     * This method clears the configuration data for the supplied tenant.
     *
     * @param tenantId The tenant id
     */
    void clear(String tenantId);

}
