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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BusinessTransactionCriteriaTest {

    @Test
    public void testIsValidNoCriteria() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();

        BusinessTransaction btxn = new BusinessTransaction();

        assertTrue("BTxn no criteria should be valid", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidStartTimeTrue() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);

        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setStartTime(200);

        assertTrue("BTxn in start time range", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidStartTimeFalse() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);

        BusinessTransaction btxn = new BusinessTransaction();
        Consumer c1 = new Consumer();
        c1.setBaseTime(20);
        btxn.getNodes().add(c1);

        assertFalse("BTxn NOT in start time range", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidEndTimeTrue() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setEndTime(100);

        BusinessTransaction btxn = new BusinessTransaction();
        Consumer c1 = new Consumer();
        c1.setBaseTime(20);
        c1.setDuration(10);
        btxn.getNodes().add(c1);

        assertTrue("BTxn in end time range", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidEndTimeFalse() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setEndTime(100);

        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setStartTime(110); // Criteria based on start time of btxn only

        assertFalse("BTxn NOT in end time range", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidPropertiesTrue() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.getProperties().put("prop1", "value1");

        BusinessTransaction btxn = new BusinessTransaction();
        btxn.getProperties().put("prop1", "value1");

        assertTrue("BTxn property/value should be found", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidPropertiesFalse() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.getProperties().put("prop1", "value1");

        BusinessTransaction btxn = new BusinessTransaction();
        btxn.getProperties().put("prop2", "value2");

        assertFalse("BTxn property should NOT be found", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidCorrelationTrue() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Global);
        cid1.setValue("myid");
        criteria.getCorrelationIds().add(cid1);

        BusinessTransaction btxn = new BusinessTransaction();

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Global);
        cid2.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid2);
        btxn.getNodes().add(c1);

        assertTrue("BTxn correlation should be found", criteria.isValid(btxn));
    }

    @Test
    public void testIsValidCorrelationFalse() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Global);
        cid1.setValue("myid1");
        criteria.getCorrelationIds().add(cid1);

        BusinessTransaction btxn = new BusinessTransaction();

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Global);
        cid2.setValue("myid2");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid2);
        btxn.getNodes().add(c1);

        assertFalse("BTxn correlation should NOT be found", criteria.isValid(btxn));
    }

}
