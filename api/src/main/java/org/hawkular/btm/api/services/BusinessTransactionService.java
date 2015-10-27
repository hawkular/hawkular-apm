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

import org.hawkular.btm.api.model.btxn.BusinessTransaction;

/**
 * This interface represents the service used to store and retrieve business
 * transactions.
 *
 * @author gbrown
 */
public interface BusinessTransactionService {

    /**
     * This method returns the business transaction associated with the
     * supplied id.
     *
     * @param tenantId The tenant
     * @param id The id
     * @return The business transaction, or null if not found
     */
    BusinessTransaction get(String tenantId, String id);

    /**
     * This method returns a set of business transactions that meet the
     * supplied query criteria.
     *
     * @param tenantId The tenant
     * @param criteria The query criteria
     * @return The list of business transactions that meet the criteria
     */
    List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria);

    /**
     * This method stores the supplied list of business transaction fragments.
     *
     * @param tenantId The tenant id
     * @param businessTransactions The business transactions
     * @throws Exception Failed to store
     */
    void storeBusinessTransactions(String tenantId, List<BusinessTransaction> businessTransactions) throws Exception;

}
