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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.server.api.services.CacheException;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InfinispanCommunicationDetailsCacheTest extends AbstractInfinispanTest {

    @Test
    public void testSingleConsumerNotFound() {
        InfinispanCommunicationDetailsCache cdc = new InfinispanCommunicationDetailsCache();

        cdc.setCommunicationDetails(cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME));

        assertNull(cdc.get(null, "id1"));
    }

    @Test
    public void testSingleConsumerFound() {
        InfinispanCommunicationDetailsCache cdc = new InfinispanCommunicationDetailsCache();

        cdc.setCommunicationDetails(cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME));

        List<CommunicationDetails> details = new ArrayList<CommunicationDetails>();
        CommunicationDetails cd = new CommunicationDetails();
        cd.setId("id1");
        details.add(cd);

        try {
            cdc.store(null, details);
        } catch (CacheException e) {
            fail("Failed: "+e);
        }

        assertEquals(cd, cdc.get(null, "id1"));
    }

}
