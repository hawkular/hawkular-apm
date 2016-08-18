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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class InfinispanSpanCacheTest extends AbstractInfinispanTest {

    private static final String TENANT = "tenant";

    private InfinispanSpanCache spanCache;

    @Before
    public void before() {
        spanCache = new InfinispanSpanCache(cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME));
    }

    @After
    public void after() {
        cacheManager.removeCache(InfinispanCommunicationDetailsCache.CACHE_NAME);
    }

    @Test
    public void testNull() {
        Assert.assertNull(spanCache.get(null, "id1"));
    }

    @Test
    public void testGetOne() throws CacheException {
        Span span = new Span();
        span.setId("parent");
        storeOne(spanCache, span);

        span = new Span();
        span.setId("id2");
        storeOne(spanCache, span);

        Assert.assertEquals("parent", getOne(spanCache, "parent").getId());
    }

    @Test
    public void testGetChildren() throws CacheException {
        Span parent = new Span();
        parent.setId("parent");
        storeOne(spanCache, parent);

        Span childServerSpan = new Span();
        childServerSpan.setId("child1");
        childServerSpan.setParentId("parent");
        childServerSpan.setAnnotations(serverAnnotations());
        storeOne(spanCache, childServerSpan);

        Span childClientSpan = new Span();
        childClientSpan.setId("child2");
        childClientSpan.setParentId("parent");
        childClientSpan.setAnnotations(clientAnnotations());
        storeOne(spanCache, childClientSpan);

        Span childClientSpan2 = new Span();
        childClientSpan2.setId("child3");
        childClientSpan2.setParentId("parent");
        storeOne(spanCache, childClientSpan2);

        Assert.assertEquals(new HashSet<>(Arrays.asList(childClientSpan, childClientSpan2)),
                new HashSet<>(spanCache.getChildren(TENANT, parent.getId())));
    }

    @Test
    public void testCacheKeyEntryGenerator() throws CacheException {
        Span span = new Span();
        span.setId("parent");
        span.setAnnotations(clientAnnotations());

        spanCache.store(TENANT, Arrays.asList(span), x -> x.getId() + "-foo");
        Assert.assertEquals(span, spanCache.get(TENANT, span.getId() + "-foo"));
    }

    private void storeOne(SpanCache spanCache, Span span) throws CacheException {
        spanCache.store(TENANT, Arrays.asList(span));
    }

    private Span getOne(SpanCache spanCache, String id) throws CacheException {
        return spanCache.get(TENANT, id);
    }

    private List<Annotation> clientAnnotations() {
        Annotation csAnnotation = new Annotation();
        csAnnotation.setValue("cs");
        Annotation crAnnotation = new Annotation();
        crAnnotation.setValue("cr");

        return Collections.unmodifiableList(Arrays.asList(csAnnotation, crAnnotation));
    }

    private List<Annotation> serverAnnotations() {
        Annotation csAnnotation = new Annotation();
        csAnnotation.setValue("sr");
        Annotation crAnnotation = new Annotation();
        crAnnotation.setValue("ss");

        return Collections.unmodifiableList(Arrays.asList(csAnnotation, crAnnotation));
    }
}
