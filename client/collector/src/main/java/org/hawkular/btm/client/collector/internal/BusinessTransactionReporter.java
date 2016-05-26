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
package org.hawkular.btm.client.collector.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;
import org.hawkular.btm.api.services.ServiceResolver;
import org.hawkular.btm.client.api.BusinessTransactionCollector;

/**
 * This class is responsible for managing a set of business transactions and
 * reporting them to the server.
 *
 * @author gbrown
 */
public class BusinessTransactionReporter {

    /**  */
    private static final int DEFAULT_BATCH_THREAD_POOL_SIZE = 5;

    /**  */
    private static final String HAWKULAR_BTM_TENANT_ID = "hawkular-btm.tenantId";

    private static final Logger log = Logger.getLogger(BusinessTransactionReporter.class.getName());

    /**  */
    private static final int DEFAULT_BATCH_TIME = 500;

    /**  */
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private BusinessTransactionPublisher businessTransactionPublisher;

    private int batchSize = DEFAULT_BATCH_SIZE;
    private int batchTime = DEFAULT_BATCH_TIME;

    private String tenantId = System.getProperty(HAWKULAR_BTM_TENANT_ID);

    private ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_BATCH_THREAD_POOL_SIZE);
    private final ReentrantLock lock=new ReentrantLock();
    private List<BusinessTransaction> businessTxns = new ArrayList<BusinessTransaction>();

    private boolean enabled = false;

    {
        setBusinessTransactionPublisher(ServiceResolver.getSingletonService(BusinessTransactionPublisher.class));
    }

    /**
     * This method sets the business transaction publisher.
     *
     * @param btp The business transaction publisher
     */
    public void setBusinessTransactionPublisher(BusinessTransactionPublisher btp) {
        this.businessTransactionPublisher = btp;

        // Check whether publisher is enabled
        if (btp != null) {
            enabled = btp.isEnabled();
        } else {
            enabled = false;
        }
    }

    /**
     * @return the businessTransactionPublisher
     */
    public BusinessTransactionPublisher getBusinessTransactionPublisher() {
        return businessTransactionPublisher;
    }

    /**
     * This method initialises the reporter with the collector configuration.
     *
     * @param config The configuration
     */
    public void init(CollectorConfiguration config) {
        if (config != null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Initializing BusinessTransactionReporter with collector configuration");
            }

            // Get properties
            String size = config.getProperty(BusinessTransactionCollector.BATCH_SIZE, null);
            if (size != null) {
                batchSize = Integer.parseInt(size);
            }

            String time = config.getProperty(BusinessTransactionCollector.BATCH_TIME, null);
            if (time != null) {
                batchTime = Integer.parseInt(time);
            }

            tenantId = config.getProperty(HAWKULAR_BTM_TENANT_ID, null);

            String pool = config.getProperty(BusinessTransactionCollector.BATCH_THREADS, null);
            if (pool != null) {
                executor = Executors.newFixedThreadPool(Integer.parseInt(pool));
            }
        }

        // Create scheduled task
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // Initial check, to avoid doing too much work if no business
                // transactions reported
                if (businessTxns.size() > 0) {
                    try {
                        lock.lock();
                        submitBusinessTransactions();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }, batchTime, batchTime, TimeUnit.MILLISECONDS);
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the tenantId
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param tenantId the tenantId to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * This method reports the business transaction to the server.
     *
     * @param btxn The business transaction
     */
    public void report(BusinessTransaction btxn) {
        if (businessTransactionPublisher != null) {
            try {
                lock.lock();
                businessTxns.add(btxn);

                if (businessTxns.size() >= batchSize) {
                    submitBusinessTransactions();
                }
            } finally {
                lock.unlock();
            }
        } else {
            log.warning("Business transaction service is not available!");
        }
    }

    /**
     * This method submits the current list of business transactions
     */
    protected void submitBusinessTransactions() {
        if (businessTxns.size() > 0) {
            // Locally store list and create new list for subsequent business transactions
            List<BusinessTransaction> toSend=businessTxns;
            businessTxns = new ArrayList<BusinessTransaction>();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        businessTransactionPublisher.publish(tenantId, toSend);
                    } catch (Exception e) {
                        // TODO: Retain for retry
                        log.log(Level.SEVERE, "Failed to publish business transactions", e);
                    }
                }
            });
        }
    }
}
