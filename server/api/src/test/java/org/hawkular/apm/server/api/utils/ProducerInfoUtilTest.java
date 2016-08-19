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
package org.hawkular.apm.server.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ProducerInfoUtilTest {

    @Test
    public void testGetProducerInfoNoClient() throws CacheException {
        SpanCache spanCache = new TestSpanCache();

        Span serverSpan = new Span(null);
        serverSpan.setId("1");
        serverSpan.setParentId("1");

        spanCache.store(null, Arrays.asList(serverSpan), SpanUniqueIdGenerator::toUnique);

        assertNull(ProducerInfoUtil.getProducerInfo(null, serverSpan, spanCache));
    }

    @Test
    public void testGetProducerInfoJustClient() throws CacheException {
        SpanCache spanCache = new TestSpanCache();

        Span serverSpan = new Span(null);
        serverSpan.setId("1");
        serverSpan.setParentId("1");
        serverSpan.setAnnotations(serverAnnotations());

        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey("http.url");
        ba.setValue("http://myhost:8080/myuri");
        Span clientSpan = new Span(Arrays.asList(ba));
        clientSpan.setId("1");
        clientSpan.setParentId("1");
        clientSpan.setAnnotations(clientAnnotations());

        spanCache.store(null, Arrays.asList(serverSpan, clientSpan),
                    SpanUniqueIdGenerator::toUnique);

        ProducerInfo pi = ProducerInfoUtil.getProducerInfo(null, serverSpan, spanCache);

        assertNotNull(pi);
        assertEquals(Constants.URI_CLIENT_PREFIX + "/myuri", pi.getSourceUri());
    }

    @Test
    public void testGetProducerInfoPartialClient() throws CacheException {
        SpanCache spanCache = new TestSpanCache();

        Span serverSpan = new Span();
        serverSpan.setId("2");
        serverSpan.setParentId("1");
        serverSpan.setAnnotations(serverAnnotations());

        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey("http.url");
        ba.setValue("http://myhost:8080/myuri");
        Span clientSpan = new Span(Arrays.asList(ba));
        clientSpan.setId("2");
        clientSpan.setParentId("1");
        clientSpan.setAnnotations(clientAnnotations());

        Span parentSpan = new Span();
        parentSpan.setId("1");
        parentSpan.setParentId("1");

        spanCache.store(null, Arrays.asList(serverSpan, clientSpan, parentSpan),
                    SpanUniqueIdGenerator::toUnique);

        ProducerInfo pi = ProducerInfoUtil.getProducerInfo(null, serverSpan, spanCache);

        assertNotNull(pi);
        assertEquals(Constants.URI_CLIENT_PREFIX + "/myuri", pi.getSourceUri());
    }

    @Test
    public void testGetProducerInfoCompleteClient() throws CacheException {
        SpanCache spanCache = new TestSpanCache();

        Span serverSpan = new Span();
        serverSpan.setId("3");
        serverSpan.setParentId("2");
        serverSpan.setAnnotations(serverAnnotations());

        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey("http.url");
        ba.setValue("http://myhost:8080/myuri");
        Span clientSpan = new Span(Arrays.asList(ba));
        clientSpan.setId("3");
        clientSpan.setParentId("2");
        clientSpan.setAnnotations(clientAnnotations());

        Span midSpan = new Span();
        midSpan.setId("2");
        midSpan.setParentId("1");

        BinaryAnnotation ba2 = new BinaryAnnotation();
        ba2.setKey("http.url");
        ba2.setValue("http://myhost:8080/originaluri");
        Span topServerSpan = new Span(Arrays.asList(ba2));
        topServerSpan.setId("1");
        topServerSpan.setParentId("1");
        topServerSpan.setAnnotations(serverAnnotations());

        spanCache.store(null, Arrays.asList(serverSpan, clientSpan, midSpan, topServerSpan),
                    SpanUniqueIdGenerator::toUnique);

        ProducerInfo pi = ProducerInfoUtil.getProducerInfo(null, serverSpan, spanCache);

        assertNotNull(pi);
        assertEquals("/originaluri", pi.getSourceUri());
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

    public class TestSpanCache implements SpanCache {

        private Map<String, Span> cache = new HashMap<String, Span>();

        /* (non-Javadoc)
         * @see org.hawkular.apm.server.api.services.Cache#get(java.lang.String, java.lang.String)
         */
        @Override
        public Span get(String tenantId, String id) {
            return cache.get(id);
        }

        /* (non-Javadoc)
         * @see org.hawkular.apm.server.api.services.Cache#store(java.lang.String, java.util.List)
         */
        @Override
        public void store(String tenantId, List<Span> spans) throws CacheException {
            spans.forEach(s -> cache.put(s.getId(), s));
        }

        /* (non-Javadoc)
         * @see org.hawkular.apm.server.api.services.SpanCache#getChildren(java.lang.String, java.lang.String)
         */
        @Override
        public List<Span> getChildren(String tenant, String id) {
            throw new UnsupportedOperationException();
        }

        /* (non-Javadoc)
         * @see org.hawkular.apm.server.api.services.SpanCache#store(java.lang.String, java.util.List, java.util.function.Function)
         */
        @Override
        public void store(String tenantId, List<Span> spans, Function<Span, String> cacheKeyEntrySupplier)
                throws CacheException {
            spans.forEach(s -> cache.put(cacheKeyEntrySupplier.apply(s), s));
        }

    }

}
