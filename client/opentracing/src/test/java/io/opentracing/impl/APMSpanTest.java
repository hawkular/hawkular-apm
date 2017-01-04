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

package io.opentracing.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.client.opentracing.APMTracer;
import org.junit.Test;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.impl.AbstractSpanBuilder.Reference;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

/**
 * @author gbrown
 */
public class APMSpanTest {

    private static final String TEST_TXN = "TestBTxn";
    private static final String TEST_APM_ID1 = "id1";
    private static final String TEST_APM_ID2 = "id2";
    private static final String TEST_APM_TRACEID = "xyz";

    @Test
    public void testFindPrimaryReferenceNoRefs() {
        assertNull(APMSpan.findPrimaryReference(Collections.emptyList()));
    }

    @Test
    public void testFindPrimaryReferenceSingleChildOfSpan() {
        Tracer tracer = new APMTracer();
        Span span = tracer.buildSpan("test").start();

        Reference ref = new Reference(References.CHILD_OF, span.context());
        assertEquals(ref, APMSpan.findPrimaryReference(Arrays.asList(ref)));
    }

    @Test
    public void testFindPrimaryReferenceMultipleChildOfSpan() {
        Tracer tracer = new APMTracer();
        Span span1 = tracer.buildSpan("test1").start();
        Reference ref1 = new Reference(References.CHILD_OF, span1.context());
        Span span2 = tracer.buildSpan("test2").start();
        Reference ref2 = new Reference(References.CHILD_OF, span2.context());
        assertNull(APMSpan.findPrimaryReference(Arrays.asList(ref1, ref2)));
    }

    @Test
    public void testFindPrimaryReferenceSingleChildOfSpanContext() {
        Tracer tracer = new APMTracer();

        SpanContext spanCtx = extractSpanContext(tracer, TEST_APM_ID1);

        Reference ref = new Reference(References.CHILD_OF, spanCtx);
        assertEquals(ref, APMSpan.findPrimaryReference(Arrays.asList(ref)));
    }

    @Test
    public void testFindPrimaryReferenceMultipleChildOfSpanContext() {
        Tracer tracer = new APMTracer();

        SpanContext spanCtx1 = extractSpanContext(tracer, TEST_APM_ID1);
        SpanContext spanCtx2 = extractSpanContext(tracer, TEST_APM_ID2);

        Reference ref1 = new Reference(References.CHILD_OF, spanCtx1);
        Reference ref2 = new Reference(References.CHILD_OF, spanCtx2);
        assertNull(APMSpan.findPrimaryReference(Arrays.asList(ref1, ref2)));
    }

    @Test
    public void testFindPrimaryReferenceSingleFollowsFrom() {
        Tracer tracer = new APMTracer();
        Span span = tracer.buildSpan("test").start();

        Reference ref = new Reference(References.FOLLOWS_FROM, span.context());
        assertEquals(ref, APMSpan.findPrimaryReference(Arrays.asList(ref)));
    }

    @Test
    public void testFindPrimaryReferenceMultipleFollowsFrom() {
        Tracer tracer = new APMTracer();
        Span span1 = tracer.buildSpan("test1").start();
        Reference ref1 = new Reference(References.FOLLOWS_FROM, span1.context());
        Span span2 = tracer.buildSpan("test2").start();
        Reference ref2 = new Reference(References.FOLLOWS_FROM, span2.context());
        assertNull(APMSpan.findPrimaryReference(Arrays.asList(ref1, ref2)));
    }

    @Test
    public void testFindPrimaryReferenceSingleChildOfSpanContextWithOtherRefs() {
        Tracer tracer = new APMTracer();

        SpanContext spanCtx1 = extractSpanContext(tracer, TEST_APM_ID1);

        Reference ref1 = new Reference(References.CHILD_OF, spanCtx1);

        Span span2 = tracer.buildSpan("test2").start();
        Reference ref2 = new Reference(References.FOLLOWS_FROM, span2.context());

        Span span3 = tracer.buildSpan("test3").start();
        Reference ref3 = new Reference(References.CHILD_OF, span3.context());
        Span span4 = tracer.buildSpan("test4").start();
        Reference ref4 = new Reference(References.CHILD_OF, span4.context());

        assertEquals(ref1, APMSpan.findPrimaryReference(Arrays.asList(ref1, ref2, ref3, ref4)));
    }

    protected SpanContext extractSpanContext(Tracer tracer, String id) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TRACEID, TEST_APM_TRACEID);
        headers.put(Constants.HAWKULAR_APM_ID, id);
        headers.put(Constants.HAWKULAR_APM_TXN, TEST_TXN);

        return tracer.extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(headers));
    }

}
