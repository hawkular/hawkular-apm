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
package org.hawkular.btm.server.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AnalyticsProviderTest {

    @Test
    public void testAllDistinctUnboundURIs() {
        AnalyticsProvider ap = new AnalyticsProvider();

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri4");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri5");

        btxn2.getNodes().add(c2);

        ap.setBusinessTransactionService(new TestBusinessTransactionService(btxns));

        List<String> uris = ap.getUnboundURIs(null, 0, 0);

        assertNotNull(uris);
        assertEquals(5, uris.size());
    }

    @Test
    public void testAllDuplicationUnboundURIs() {
        AnalyticsProvider ap = new AnalyticsProvider();

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri3");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri2");

        btxn2.getNodes().add(c2);

        ap.setBusinessTransactionService(new TestBusinessTransactionService(btxns));

        List<String> uris = ap.getUnboundURIs(null, 0, 0);

        assertNotNull(uris);
        assertEquals(3, uris.size());
    }

    @Test
    public void testSomeBoundURIs() {
        AnalyticsProvider ap = new AnalyticsProvider();

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setStartTime(100);
        btxns.add(btxn1);

        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        btxn1.getNodes().add(c1);

        Component t1 = new Component();
        t1.setUri("uri2");
        c1.getNodes().add(t1);

        Component t2 = new Component();
        t2.setUri("uri3");
        c1.getNodes().add(t2);

        Producer p1 = new Producer();
        p1.setUri("uri3");
        c1.getNodes().add(p1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setName("A Name");
        btxns.add(btxn2);

        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        btxn2.getNodes().add(c2);

        Component t3 = new Component();
        t3.setUri("uri4");
        c2.getNodes().add(t3);

        ap.setBusinessTransactionService(new TestBusinessTransactionService(btxns));

        List<String> uris = ap.getUnboundURIs(null, 0, 0);

        assertNotNull(uris);
        assertEquals(2, uris.size());
    }

    public class TestBusinessTransactionService implements BusinessTransactionService {

        private List<BusinessTransaction> businessTransactions;

        public TestBusinessTransactionService(List<BusinessTransaction> btxns) {
            businessTransactions = btxns;
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
         */
        @Override
        public BusinessTransaction get(String tenantId, String id) {
            return null;
        }

        /* (non-Javadoc)
         * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
         *                      org.hawkular.btm.api.services.BusinessTransactionCriteria)
         */
        @Override
        public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
            return businessTransactions;
        }

    }
}
