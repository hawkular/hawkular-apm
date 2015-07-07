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
package org.hawkular.btm.client.collector.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hawkular.btm.api.model.admin.BusinessTxnConfig;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;
import org.hawkular.btm.api.model.admin.Filter;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FilterManagerTest {

    @Test
    public void testGlobalExclusion() {
        CollectorConfiguration config = new CollectorConfiguration();
        BusinessTxnConfig btc1 = new BusinessTxnConfig();
        config.getBusinessTransactions().put("btc1", btc1);
        BusinessTxnConfig btc2 = new BusinessTxnConfig();
        config.getBusinessTransactions().put("btc2", btc2);

        // Global exclusion filter
        Filter f1 = new Filter();
        btc1.setFilter(f1);
        f1.getExclusions().add("clude");

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getBusinessTransactionName("include"));
    }

    @Test
    public void testBusinessTransactionInclusion() {
        CollectorConfiguration config = new CollectorConfiguration();
        BusinessTxnConfig btc2 = new BusinessTxnConfig();
        config.getBusinessTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");

        FilterManager fm = new FilterManager(config);

        assertNotNull(fm.getBusinessTransactionName("include and exclude"));
    }

    @Test
    public void testBusinessTransactionIncludeExclude() {
        CollectorConfiguration config = new CollectorConfiguration();
        BusinessTxnConfig btc2 = new BusinessTxnConfig();
        config.getBusinessTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getInclusions().add("include");
        f2.getExclusions().add("exclude");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getBusinessTransactionName("include and exclude"));
    }

    @Test
    public void testExcludeDefaults() {
        CollectorConfiguration config = new CollectorConfiguration();
        BusinessTxnConfig btc2 = new BusinessTxnConfig();
        config.getBusinessTransactions().put("btc2", btc2);

        // Business txn specific
        Filter f2 = new Filter();
        btc2.setFilter(f2);
        f2.getExclusions().add("^https?://.*/hawkular/btm");
        f2.getExclusions().add("^https?://.*/auth/");

        FilterManager fm = new FilterManager(config);

        assertNull(fm.getBusinessTransactionName("http://localhost:8080/hawkular/btm/transactions"));

        assertNull(fm.getBusinessTransactionName(
                "http://localhost:8080/auth/realms/artificer/protocol/openid-connect/token"));
    }

}
