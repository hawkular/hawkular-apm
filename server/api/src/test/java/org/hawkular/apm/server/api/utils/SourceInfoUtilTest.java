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
import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.junit.Test;

/**
 * @author gbrown
 */
public class SourceInfoUtilTest {

    @Test
    public void testInitialiseIds() throws RetryAttemptException {
        Trace trace = new Trace();
        trace.setId("trace1");

        Consumer consumer1 = new Consumer();
        consumer1.setUri("uri1");
        consumer1.setOperation("op1");
        consumer1.addCausedByCorrelationId("cb1");
        consumer1.addInteractionCorrelationId("in1");
        trace.getNodes().add(consumer1);

        Component component1 = new Component();
        consumer1.getNodes().add(component1);

        Producer producer1 = new Producer();
        producer1.addControlFlowCorrelationId("cf1");
        component1.getNodes().add(producer1);

        Producer producer2 = new Producer();
        producer2.addInteractionCorrelationId("in2");
        component1.getNodes().add(producer2);

        List<SourceInfo> sourceInfoList = SourceInfoUtil.getSourceInfo(null, Collections.singletonList(trace));

        assertNotNull(sourceInfoList);
        assertEquals(6, sourceInfoList.size());

        SourceInfo si1 = sourceInfoList.get(0);
        SourceInfo si2 = sourceInfoList.get(1);
        SourceInfo si3 = sourceInfoList.get(2);
        SourceInfo si4 = sourceInfoList.get(3);
        SourceInfo si5 = sourceInfoList.get(4);
        SourceInfo si6 = sourceInfoList.get(5);

        // Each node should have a source info with id representing the trace
        // fragment id + path to node.
        // The source info associated with the two Producer nodes should have an
        // id representing the correlation value.
        assertEquals("trace1:0", si1.getId());
        assertEquals("trace1:0:0", si2.getId());
        assertEquals("trace1:0:0:0", si3.getId());
        assertEquals("cf1", si4.getId());
        assertEquals("trace1:0:0:1", si5.getId());
        assertEquals("in2", si6.getId());
    }

    @Test
    public void testInitialiseFragmentOrigins() throws RetryAttemptException {
        Trace trace1 = new Trace();
        trace1.setId("trace1");

        Component component1 = new Component();
        trace1.getNodes().add(component1);

        Producer producer1 = new Producer();
        producer1.setUri("uri1");
        producer1.setOperation("op1");
        producer1.addInteractionCorrelationId("in1");
        component1.getNodes().add(producer1);

        Trace trace2 = new Trace();
        trace2.setId("trace2");

        Consumer consumer1 = new Consumer();
        consumer1.setUri("uri1");
        consumer1.setOperation("op1");
        consumer1.addInteractionCorrelationId("in1");
        trace2.getNodes().add(consumer1);

        Component component2 = new Component();
        consumer1.getNodes().add(component2);

        List<SourceInfo> sourceInfoList = SourceInfoUtil.getSourceInfo(null, Arrays.asList(trace1, trace2));

        assertNotNull(sourceInfoList);
        assertEquals(5, sourceInfoList.size());

        SourceInfo si1 = sourceInfoList.get(0);
        SourceInfo si2 = sourceInfoList.get(1);
        SourceInfo si3 = sourceInfoList.get(2);
        SourceInfo si4 = sourceInfoList.get(3);
        SourceInfo si5 = sourceInfoList.get(4);

        assertEquals("client:uri1", si1.getFragmentUri());
        assertEquals("op1", si1.getFragmentOperation());
        assertEquals("client:uri1", si2.getFragmentUri());
        assertEquals("op1", si2.getFragmentOperation());
        assertEquals("client:uri1", si3.getFragmentUri());
        assertEquals("op1", si3.getFragmentOperation());
        assertEquals("uri1", si4.getFragmentUri());
        assertEquals("op1", si4.getFragmentOperation());
        assertEquals("uri1", si5.getFragmentUri());
        assertEquals("op1", si5.getFragmentOperation());
    }

    @Test
    public void testGetSourceInfoNoClient() throws CacheException {
        SpanCache spanCache = new TestSpanCache();

        Span serverSpan = new Span(null);
        serverSpan.setId("1");
        serverSpan.setParentId("1");

        spanCache.store(null, Arrays.asList(serverSpan), SpanUniqueIdGenerator::toUnique);

        assertNull(SourceInfoUtil.getSourceInfo(null, serverSpan, spanCache));
    }

    @Test
    public void testGetSourceInfoJustClient() throws CacheException {
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

        SourceInfo si = SourceInfoUtil.getSourceInfo(null, serverSpan, spanCache);

        assertNotNull(si);
        assertEquals(Constants.URI_CLIENT_PREFIX + "/myuri", si.getFragmentUri());
    }

    @Test
    public void testGetSourceInfoPartialClient() throws CacheException {
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

        SourceInfo si = SourceInfoUtil.getSourceInfo(null, serverSpan, spanCache);

        assertNotNull(si);
        assertEquals(Constants.URI_CLIENT_PREFIX + "/myuri", si.getFragmentUri());
    }

    @Test
    public void testGetSourceInfoCompleteClient() throws CacheException {
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

        SourceInfo si = SourceInfoUtil.getSourceInfo(null, serverSpan, spanCache);

        assertNotNull(si);
        assertEquals("/originaluri", si.getFragmentUri());
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
