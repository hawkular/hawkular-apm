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
package org.hawkular.apm.server.infinispan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.server.api.services.CacheException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InfinispanCommunicationDetailsCacheTest extends AbstractInfinispanTest {

    private InfinispanCommunicationDetailsCache cdc;

    @Before
    public void before() {
        cdc = new InfinispanCommunicationDetailsCache(cacheManager);
    }

    @Test
    public void testSingleConsumerNotFound() {
        assertNull(cdc.get(null, "id1"));
    }

    @Test
    public void testSingleConsumerFound() {
        List<CommunicationDetails> details = new ArrayList<CommunicationDetails>();
        CommunicationDetails cd = new CommunicationDetails();
        cd.setLinkId("id1");
        details.add(cd);

        try {
            cdc.store(null, details);
        } catch (CacheException e) {
            fail("Failed: "+e);
        }

        assertEquals(cd, cdc.get(null, "id1"));
    }

    @Test
    public void testGetByIdMultiple() throws CacheException {
        CommunicationDetails cdMultipleConsumer1 = new CommunicationDetails();
        cdMultipleConsumer1.setLinkId("id1");
        cdMultipleConsumer1.setTargetFragmentId("fragmentId1");
        cdMultipleConsumer1.setMultiConsumer(true);

        CommunicationDetails cdMultipleConsumer2 = new CommunicationDetails();
        cdMultipleConsumer2.setLinkId("id1");
        cdMultipleConsumer2.setTargetFragmentId("fragmentId2");
        cdMultipleConsumer2.setMultiConsumer(true);

        CommunicationDetails cd3 = new CommunicationDetails();
        cd3.setLinkId("id2");

        cdc.store(null, Arrays.asList(cdMultipleConsumer1, cdMultipleConsumer2, cd3));

        List<CommunicationDetails> cdFromCache = cdc.getById(null, "id1");

        Assert.assertEquals(2, cdFromCache.size());
        Assert.assertEquals(new HashSet<>(Arrays.asList(cdMultipleConsumer1, cdMultipleConsumer2)),
                new HashSet<>(cdFromCache));
    }

    @Test
    public void testGetMultipleEmpty() {
        List<CommunicationDetails> id = cdc.getById(null, "id");
        Assert.assertTrue(id.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetMultipleNull() {
        cdc.getById(null, null);
    }
}
