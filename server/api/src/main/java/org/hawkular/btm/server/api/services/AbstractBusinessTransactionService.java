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
package org.hawkular.btm.server.api.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.server.api.processors.BusinessTransactionFragmentHandler;
import org.jboss.logging.Logger;

/**
 * This abstract class provides the base implementation of the Business Transaction
 * Service.
 *
 * @author gbrown
 */
public abstract class AbstractBusinessTransactionService implements BusinessTransactionService {

    private final Logger log = Logger.getLogger(AbstractBusinessTransactionService.class);

    @Inject
    private List<BusinessTransactionFragmentHandler> handlers =
    new ArrayList<BusinessTransactionFragmentHandler>();

    @Resource
    private ManagedExecutorService executorService;

    /**
     * @return the handlers
     */
    public List<BusinessTransactionFragmentHandler> getBusinessTransactionFragmentHandlers() {
        return handlers;
    }

    /**
     * @param handlers the handlers to set
     */
    public void setBusinessTransactionFragmentHandlers(List<BusinessTransactionFragmentHandler> handlers) {
        this.handlers = handlers;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#store(java.lang.String,java.util.List)
     */
    @Override
    public void store(String tenantId, List<BusinessTransaction> btxns) throws Exception {
        log.tracef("Store business transactions: %s", btxns);

        if (btxns.size() == 0) {
            return;
        }

        for (int i = 0; i < btxns.size(); i++) {
            doStore(tenantId, btxns.get(i));
        }

        if (handlers.size() > 0) {
            log.tracef("Distribute business transactions to " + handlers.size() +
                    " handlers: " + handlers + " (with executor=" + executorService + ")");

            // Process business transaction fragments
            for (int i = 0; i < handlers.size(); i++) {
                if (executorService == null) {
                    handlers.get(i).handle(tenantId, btxns);
                } else {
                    executorService.execute(new BTxnFragmentHandlerTask(tenantId, handlers.get(i), btxns));
                }
            }
        }
    }

    /**
     * This method is overridden by the concrete business transaction service
     * to implement storing a business transaction.
     *
     * @param tenantId The tenant
     * @param btxn The business transaction
     * @throws Exception Failed to store business transaction
     */
    protected abstract void doStore(String tenantId, BusinessTransaction btxn) throws Exception;

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String,java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        BusinessTransaction ret = doGet(tenantId, id);

        log.tracef("Get business transaction with id[%s] is: %s", id, ret);

        return ret;
    }

    /**
     * This method is overridden by the concrete business transaction service
     * to implement retrieval of the business transaction.
     *
     * @param tenantId The tenant
     * @param id The id
     * @return The business transaction
     */
    protected abstract BusinessTransaction doGet(String tenantId, String id);

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
     *          org.hawkular.btm.api.services.BusinessTransactionQuery)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> ret = doQuery(tenantId, criteria);

        log.tracef("Query business transactions with criteria[%s] is: %s", criteria, ret);

        return ret;
    }

    /**
     * This method is overridden by the concrete business transaction service
     * to implement querying for a set of business transactions.
     *
     * @param tenantId The tenant
     * @param criteria The query criteria
     * @return The list of business transactions
     */
    protected abstract List<BusinessTransaction> doQuery(String tenantId, BusinessTransactionCriteria criteria);

    /**
     * This task processes a list of business transaction fragments using a provided
     * handler.
     *
     * @author gbrown
     */
    private static class BTxnFragmentHandlerTask implements Runnable {

        private String tenantId;
        private BusinessTransactionFragmentHandler handler;
        private List<BusinessTransaction> businessTransactions;

        public BTxnFragmentHandlerTask(String tenantId, BusinessTransactionFragmentHandler handler,
                List<BusinessTransaction> btxns) {
            this.tenantId = tenantId;
            this.handler = handler;
            this.businessTransactions = btxns;
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            handler.handle(tenantId, businessTransactions);
        }

    }
}
