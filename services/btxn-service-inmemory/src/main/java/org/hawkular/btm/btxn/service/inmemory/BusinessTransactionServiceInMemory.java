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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.AbstractBusinessTransactionService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.jboss.logging.Logger;

/**
 * This class provides the in-memory implementation of the Business Transaction
 * Service. This implementation is only intended for testing and non-persistent
 * low transaction count usage.
 *
 * @author gbrown
 */
@Singleton
public class BusinessTransactionServiceInMemory extends AbstractBusinessTransactionService {

    /**  */
    private static final String INMEM_MAXTXNS = "hawkular-btm.btxn-service.inmem-maxtxns";

    private final Logger log = Logger.getLogger(BusinessTransactionServiceInMemory.class);

    private static Map<String, BusinessTransaction> idMap = new HashMap<String, BusinessTransaction>();
    private static List<BusinessTransaction> txns = new ArrayList<BusinessTransaction>();

    private static int maxTransactions = 1000;

    static {
        String prop = System.getProperty(INMEM_MAXTXNS);

        if (prop != null) {
            maxTransactions = Integer.parseInt(prop);
        }
    }

    /**
     * @return the txns
     */
    public static List<BusinessTransaction> getTxns() {
        return txns;
    }

    @Override
    protected void doStore(String tenantId, BusinessTransaction btxn) throws Exception {
        synchronized (txns) {
            BusinessTransaction old = idMap.put(btxn.getId(), btxn);

            if (old != null) {
                txns.remove(old);
            }

            txns.add(btxn);

            while (txns.size() > maxTransactions) {
                BusinessTransaction toRemove=txns.remove(0);
                idMap.remove(toRemove.getId());
            }
        }
    }

    @Override
    protected BusinessTransaction doGet(String tenantId, String id) {
        BusinessTransaction ret = null;

        synchronized (txns) {
            ret = idMap.get(id);
        }

        log.tracef("Get business transaction with id[%s] is: %s", id, ret);

        return ret;
    }

    @Override
    protected List<BusinessTransaction> doQuery(String tenantId, BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> ret = new ArrayList<BusinessTransaction>();

        synchronized (txns) {
            txns.stream().filter(p -> criteria.isValid(p)).forEach(p -> ret.add(p));
        }

        log.tracef("Query business transactions with criteria[%s] is: %s", criteria, ret);

        return ret;
    }

    /**
     * @return the maxTransactions
     */
    public static int getMaxTransactions() {
        return maxTransactions;
    }

    /**
     * @param maxTransactions the maxTransactions to set
     */
    public static void setMaxTransactions(int maxTransactions) {
        BusinessTransactionServiceInMemory.maxTransactions = maxTransactions;
    }

}
