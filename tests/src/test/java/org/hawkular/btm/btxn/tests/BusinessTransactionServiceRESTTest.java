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
package org.hawkular.btm.btxn.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.btxn.service.rest.client.BusinessTransactionServiceRESTClient;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BusinessTransactionServiceRESTTest {

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    @Test
    public void testStoreAndRetrieveById() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Retrieve stored business transaction
        BusinessTransaction result = service.get(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void testStoreAndQueryAll() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryStartTimeInclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.setStartTime(100);

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryStartTimeExclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.setStartTime(1100);

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryEndTimeInclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        c1.setDuration(500);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.setEndTime(2000);

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryEndTimeExclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        c1.setDuration(500);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.setEndTime(1100);

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryPropertiesInclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.getProperties().put("hello", "world");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.getProperties().put("hello", "world");

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryPropertiesExclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.getProperties().put("hello", "world");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.getProperties().put("hello", "fred");

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryCorrelationsInclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "myid"));

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryCorrelationsExclude() {
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.store(null, btxns);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Exchange, "notmyid"));

        List<BusinessTransaction> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

}
