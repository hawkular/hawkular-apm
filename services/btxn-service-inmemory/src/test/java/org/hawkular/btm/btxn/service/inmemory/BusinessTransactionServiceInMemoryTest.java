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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BusinessTransactionServiceInMemoryTest {

    @Test
    public void testStoreMax() {
        BusinessTransactionServiceInMemory.setMaxTransactions(3);

        BusinessTransactionServiceInMemory bts = new BusinessTransactionServiceInMemory();

        try {
            bts.store(null, createTransactions(1));
            bts.store(null, createTransactions(11));
        } catch (Exception e) {
            fail("Failed to store txns: " + e);
        }

        // Check only 3 txns left
        assertEquals(3, BusinessTransactionServiceInMemory.getTxns().size());

        assertNotNull(bts.get(null, "2"));
        assertNotNull(bts.get(null, "11"));
        assertNotNull(bts.get(null, "12"));

        assertNull(bts.get(null, "1"));
    }

    protected static List<BusinessTransaction> createTransactions(int baseid) {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("" + baseid);

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
        btxn2.setId("" + (baseid + 1));

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
