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
package org.hawkular.btm.btxn.service.inmemory;

import java.util.List;

import javax.inject.Singleton;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.jboss.logging.Logger;

/**
 * This class provides the in-memory implementation of the Business Transaction
 * Service. This implementation is only intended for testing and non-persistent
 * low transaction count usage.
 *
 * @author gbrown
 */
@Singleton
public class BusinessTransactionServiceInMemory implements BusinessTransactionService {

    private final Logger log = Logger.getLogger(BusinessTransactionServiceInMemory.class);

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String,java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        BusinessTransaction ret = BusinessTransactionRepository.get(tenantId, id);

        log.tracef("Get business transaction with id[%s] is: %s", id, ret);

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
     *          org.hawkular.btm.api.services.BusinessTransactionQuery)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> ret = BusinessTransactionRepository.query(tenantId, criteria);

        log.tracef("Query business transactions with criteria[%s] is: %s", criteria, ret);

        return ret;
    }

}
