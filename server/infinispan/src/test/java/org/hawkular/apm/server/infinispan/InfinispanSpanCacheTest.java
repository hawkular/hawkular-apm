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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class InfinispanSpanCacheTest extends AbstractInfinispanTest {

    private InfinispanSpanCache spanCache;

    @Before
    public void before() {
        spanCache = new InfinispanSpanCache(cacheManager);
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

        Span childServerSpan = new Span(null, serverAnnotations());
        childServerSpan.setId("child1");
        childServerSpan.setParentId("parent");
        storeOne(spanCache, childServerSpan);

        Span childClientSpan = new Span(null, clientAnnotations());
        childClientSpan.setId("child2");
        childClientSpan.setParentId("parent");
        storeOne(spanCache, childClientSpan);

        Span childClientSpan2 = new Span();
        childClientSpan2.setId("child3");
        childClientSpan2.setParentId("parent");
        storeOne(spanCache, childClientSpan2);

        Assert.assertEquals(new HashSet<>(Arrays.asList(childClientSpan, childClientSpan2)),
                new HashSet<>(spanCache.getChildren(null, parent.getId())));
    }

    @Test
    public void testGetChildrenEmpty() {
        Assert.assertNull(spanCache.getChildren(null, "id"));
    }

    @Test
    public void testCacheKeyEntryGenerator() throws CacheException {
        Span span = new Span(null, clientAnnotations());
        span.setId("parent");

        spanCache.store(null, Arrays.asList(span), x -> x.getId() + "-foo");
        Assert.assertEquals(span, spanCache.get(null, span.getId() + "-foo"));
    }

    @Test
    public void testGetTrace() throws CacheException {
        Span rootSpan = new Span();
        rootSpan.setId("trace");
        rootSpan.setTraceId("trace");

        Span descSpan = new Span();
        descSpan.setId("desc1");
        descSpan.setTraceId("trace");
        descSpan.setParentId("trace");

        Span descSpan2 = new Span();
        descSpan2.setId("desc2");
        descSpan2.setTraceId("trace");
        descSpan2.setParentId("desc1");

        Span orphanSpan = new Span();
        orphanSpan.setId("orphan");
        orphanSpan.setTraceId("orphan");
        orphanSpan.setParentId("orphan");

        spanCache.store(null, Arrays.asList(rootSpan, descSpan, descSpan2, orphanSpan));

        Assert.assertEquals(new HashSet<>(Arrays.asList(rootSpan, descSpan, descSpan2)),
                new HashSet<>(spanCache.getTrace(null, "trace")));
    }

    @Test
    public void testGetTraceEmpty() {
        Assert.assertNull(spanCache.getTrace(null, "id"));
    }

    private void storeOne(SpanCache spanCache, Span span) throws CacheException {
        spanCache.store(null, Arrays.asList(span));
    }

    private Span getOne(SpanCache spanCache, String id) throws CacheException {
        return spanCache.get(null, id);
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
