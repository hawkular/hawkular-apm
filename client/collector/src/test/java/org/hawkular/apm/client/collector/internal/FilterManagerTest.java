/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.apm.client.collector.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FilterManagerTest {

    @Test
    public void testGlobalExclusion() {
        CollectorConfiguration config = new CollectorConfiguration();
        TransactionConfig btc1 = new TransactionConfig();
        config.getTransactions().put("btc1", btc1);
        TransactionConfig btc2 = new TransactionConfig();
        config.getTransactions().put("btc2", btc2);

        // Global exclusion filter
        Filter f1 = new Filter();
        btc1.setFilter(f1);
        f1.getExclusions().add("clude");

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getFilterProcessor("include"));
    }

    @Test
    public void testTransactionInclusion() {
        CollectorConfiguration config = new CollectorConfiguration();
        TransactionConfig btc2 = new TransactionConfig();
        config.getTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        FilterManager fm = new FilterManager(config);

        FilterProcessor fp = fm.getFilterProcessor("include and exclude");
        assertNotNull(fp);
        assertEquals("btc2", fp.getTransaction());
    }

    @Test
    public void testTransactionIncludeExclude() {
        CollectorConfiguration config = new CollectorConfiguration();
        TransactionConfig btc2 = new TransactionConfig();
        config.getTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");
        f2.getExclusions().add("exclude");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getFilterProcessor("include and exclude"));
    }

    @Test
    public void testExcludeDefaults() {
        CollectorConfiguration config = new CollectorConfiguration();
        TransactionConfig btc2 = new TransactionConfig();
        config.getTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getExclusions().add("^https?://.*/hawkular/apm");
        f2.getExclusions().add("^https?://.*/auth/");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getFilterProcessor("http://localhost:8080/hawkular/apm/transactions"));

        assertNull(fm.getFilterProcessor(
                "http://localhost:8080/auth/realms/artificer/protocol/openid-connect/token"));
    }

    @Test
    public void testTransactionIncludedByDefault() {
        CollectorConfiguration config = new CollectorConfiguration();
        TransactionConfig btc2 = new TransactionConfig();
        config.getTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        FilterManager fm = new FilterManager(config);

        FilterProcessor fp = fm.getFilterProcessor("notrecognised");
        assertNotNull(fp);
        assertNull(fp.getTransaction());
    }

    @Test
    public void testTransactionExcludedByDefault() {
        CollectorConfiguration config = new CollectorConfiguration();
        config.getProperties().put("HAWKULAR_APM_COLLECTOR_ONLYNAMED", "true");
        TransactionConfig btc2 = new TransactionConfig();
        config.getTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getFilterProcessor("notrecognised"));
    }

    public void testInit() {
        CollectorConfiguration config = new CollectorConfiguration();
        TransactionConfig btc1 = new TransactionConfig();
        btc1.setDescription("Hello");
        config.getTransactions().put("btc", btc1);

        FilterManager fm = new FilterManager(config);

        assertTrue(fm.getFilterMap().containsKey("btc"));
        assertEquals(1, fm.getGlobalExclusionFilters().size());
        assertEquals(0, fm.getBtxnFilters().size());

        TransactionConfig btc2 = new TransactionConfig();
        btc2.setDescription("Changed");
        config.getTransactions().put("btc", btc2);

        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        assertTrue(fm.getFilterMap().containsKey("btc"));
        assertEquals(0, fm.getGlobalExclusionFilters().size());
        assertEquals(1, fm.getBtxnFilters().size());
    }
}
