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

import java.util.Arrays;

import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class InfinispanSpanCacheTest extends AbstractInfinispanTest {

    private static final String TENANT = "tenant";

    @Test
    public void testNull() {
        InfinispanSpanCache spanCache = new InfinispanSpanCache();
        spanCache.setSpansCache(cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME));

        Assert.assertNull(spanCache.get(null, "id1"));
    }

    @Test
    public void testGetOne() throws CacheException {
        InfinispanSpanCache spanCache = new InfinispanSpanCache();
        spanCache.setSpansCache(cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME));

        Span span = new Span();
        span.setId("parent");
        storeOne(spanCache, span);

        span = new Span();
        span.setId("id2");
        storeOne(spanCache, span);

        Assert.assertEquals("parent", getOne(spanCache, "parent").getId());
    }

    @Test
    public void testGetParent() throws CacheException {
        InfinispanSpanCache spanCache = new InfinispanSpanCache();
        spanCache.setSpansCache(cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME));

        Span parent = new Span();
        parent.setId("parent");
        storeOne(spanCache, parent);

        Span child1 = new Span();
        child1.setId("child1");
        child1.setParentId("parent");
        storeOne(spanCache, child1);

        Span child2 = new Span();
        child2.setId("child2");
        child2.setParentId("parent");
        storeOne(spanCache, child2);

        Assert.assertEquals(2, spanCache.getChildren(TENANT, parent.getId()).size());
    }

    private void storeOne(SpanCache spanCache, Span span) throws CacheException {
        spanCache.store(TENANT, Arrays.asList(span));
    }

    private Span getOne(SpanCache spanCache, String id) throws CacheException {
        return spanCache.get(TENANT, id);
    }
}
