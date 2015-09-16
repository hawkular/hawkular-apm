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
package org.hawkular.btm.btxn.service.casses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BusinessTransactionServiceCassEsTest {

    /**  */
    private static final String TEST_TENANT = UUID.randomUUID().toString();

    private static BusinessTransactionService service = new BusinessTransactionServiceCassEs();

    private static List<BusinessTransaction> businessTransactions = transactions();

    @BeforeClass
    public static void init() {

        // TODO: Setup DB and ensure clean

        try {
            service.store(TEST_TENANT, businessTransactions);
        } catch (Exception e) {
            fail("Failed to store business transactions: " + e);
        }
    }

    // TODO: Add more tests to exercise the Cassandra/Elasticsearch impl

    @Test
    public void testStoreAndRetrieveAll() {
        retrieveAndVerify(new BusinessTransactionCriteria());
    }

    protected void retrieveAndVerify(BusinessTransactionCriteria criteria) {
        List<BusinessTransaction> results = service.query(TEST_TENANT, criteria);

        // Build expected result list
        List<BusinessTransaction> expected = new ArrayList<BusinessTransaction>();

        for (BusinessTransaction btxn : businessTransactions) {
            if (criteria.isValid(btxn)) {
                expected.add(btxn);
            }
        }

        assertEquals(expected.size(), results.size());
    }

    protected static List<BusinessTransaction> transactions() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer node1_1 = new Consumer();
        node1_1.setDuration(10000);
        node1_1.setBaseTime(100);
        btxn1.getNodes().add(node1_1);

        Component node1_2 = new Component();
        node1_2.setDuration(9000);
        node1_2.setBaseTime(150);
        node1_1.getNodes().add(node1_2);

        btxn1.getNodes().add(node1_1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("2");

        Consumer node2_1 = new Consumer();
        node2_1.setDuration(10000);
        node2_1.setBaseTime(100);
        btxn2.getNodes().add(node2_1);

        Component node2_2 = new Component();
        node2_2.setDuration(9000);
        node2_2.setBaseTime(150);
        node2_1.getNodes().add(node2_2);

        btxn1.getNodes().add(node2_1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);
        btxns.add(btxn2);

        return btxns;
    }

}
