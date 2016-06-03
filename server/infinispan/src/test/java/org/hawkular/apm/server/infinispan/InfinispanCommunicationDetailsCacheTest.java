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
package org.hawkular.apm.server.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InfinispanCommunicationDetailsCacheTest {

    private static DefaultCacheManager cm;

    @BeforeClass
    public static void initClass() {
        cm = new DefaultCacheManager();
    }

    @Test
    public void testSingleConsumerNotFound() {
        InfinispanCommunicationDetailsCache cdc = new InfinispanCommunicationDetailsCache();

        cdc.setCommunicationDetails(cm.getCache());

        assertNull(cdc.getSingleConsumer(null, "id1"));
    }

    @Test
    public void testSingleConsumerFound() {
        InfinispanCommunicationDetailsCache cdc = new InfinispanCommunicationDetailsCache();

        cdc.setCommunicationDetails(cm.getCache());

        List<CommunicationDetails> details = new ArrayList<CommunicationDetails>();
        CommunicationDetails cd = new CommunicationDetails();
        cd.setId("id1");
        details.add(cd);

        cdc.store(null, details);

        assertEquals(cd, cdc.getSingleConsumer(null, "id1"));
    }

    @Test
    @org.junit.Ignore("HWKBTM-356 Support multiple consumers")
    public void testMultiConsumerFound() {
        InfinispanCommunicationDetailsCache cdc = new InfinispanCommunicationDetailsCache();

        cdc.setCommunicationDetails(cm.getCache());

        List<CommunicationDetails> details = new ArrayList<CommunicationDetails>();
        CommunicationDetails cd1 = new CommunicationDetails();
        cd1.setId("id1");
        cd1.setTargetFragmentId("fid1");
        details.add(cd1);

        CommunicationDetails cd2 = new CommunicationDetails();
        cd2.setId("id1");
        cd2.setTargetFragmentId("fid2");
        details.add(cd2);

        cdc.store(null, details);

        List<CommunicationDetails> result = cdc.getMultipleConsumers(null, "id1");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

}
