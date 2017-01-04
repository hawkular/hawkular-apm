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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.config.txn.Filter;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.junit.Test;

/**
 * @author gbrown
 */
public class FilterProcessorTest {

    @Test
    public void testGlobalExclusionFilter() {
        TransactionConfig btc=new TransactionConfig();
        Filter f1 = new Filter();
        btc.setFilter(f1);
        f1.getExclusions().add("exclude");

        FilterProcessor fp = new FilterProcessor("btc1", btc);

        assertTrue(fp.isIncludeAll());

        assertTrue(fp.isExcluded("exclude"));
    }

    @Test
    public void testIncludeFilter() {
        TransactionConfig btc=new TransactionConfig();
        Filter f1 = new Filter();
        btc.setFilter(f1);
        f1.getInclusions().add("include");

        FilterProcessor fp = new FilterProcessor("btc1", btc);

        assertFalse(fp.isIncludeAll());

        assertTrue(fp.isIncluded("include"));

        assertFalse(fp.isExcluded("include"));
    }

    @Test
    public void testIncludeAndExcludeFilter() {
        TransactionConfig btc=new TransactionConfig();
        Filter f1 = new Filter();
        btc.setFilter(f1);
        f1.getInclusions().add("include");
        f1.getExclusions().add("exclude");

        FilterProcessor fp = new FilterProcessor("btc1", btc);

        assertFalse(fp.isIncludeAll());

        assertTrue(fp.isIncluded("include and exclude"));

        assertTrue(fp.isExcluded("include and exclude"));
    }

    @Test
    public void testExcludeFilter() {
        TransactionConfig btc=new TransactionConfig();
        Filter f1 = new Filter();
        btc.setFilter(f1);
        f1.getExclusions().add("https?://.*/hawkular/apm");

        FilterProcessor fp = new FilterProcessor("btc1", btc);

        assertTrue(fp.isIncludeAll());

        assertTrue(fp.isExcluded("http://localhost:8080/hawkular/apm/transactions"));
    }

}
