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

package org.hawkular.apm.server.processor.zipkin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Gary Brown
 */
public class NodeDetailsDeriverTest {

    @Test
    public void testServerSpanWithURL() throws RetryAttemptException {
        SpanCache spanCacheMock = Mockito.mock(SpanCache.class);
        NodeDetailsDeriver nodeDetailsDeriver = new NodeDetailsDeriver(spanCacheMock);

        Annotation sr = new Annotation();
        sr.setValue("sr");
        Annotation ss = new Annotation();
        ss.setValue("ss");
        BinaryAnnotation httpAddress = new BinaryAnnotation();
        httpAddress.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        httpAddress.setValue("http://localhost/hello");
        Span serverSpan = new Span(Arrays.asList(httpAddress), Arrays.asList(sr, ss));
        serverSpan.setId("2");
        serverSpan.setTraceId("1");

        NodeDetails result = nodeDetailsDeriver.processOneToOne(null, serverSpan);
        assertEquals("/hello", result.getUri());
    }

    @Test(expected = RetryAttemptException.class)
    public void testServerSpanNoURLClientSpanUnavailable() throws RetryAttemptException {
        SpanCache spanCacheMock = Mockito.mock(SpanCache.class);
        NodeDetailsDeriver nodeDetailsDeriver = new NodeDetailsDeriver(spanCacheMock);

        Annotation sr = new Annotation();
        sr.setValue("sr");
        Annotation ss = new Annotation();
        ss.setValue("ss");
        Span serverSpan = new Span(Collections.emptyList(), Arrays.asList(sr, ss));
        serverSpan.setId("2");
        serverSpan.setTraceId("1");
        Mockito.when(spanCacheMock.get(null, SpanUniqueIdGenerator.getClientId(serverSpan.getId())))
                .thenReturn(null);

        nodeDetailsDeriver.processOneToOne(null, serverSpan);
    }

    @Test
    public void testServerSpanNoURLClientSpanNoURL() throws RetryAttemptException {
        SpanCache spanCacheMock = Mockito.mock(SpanCache.class);
        NodeDetailsDeriver nodeDetailsDeriver = new NodeDetailsDeriver(spanCacheMock);

        Annotation cs = new Annotation();
        cs.setValue("cs");
        Annotation cr = new Annotation();
        cr.setValue("cr");
        Span clientSpan = new Span(Collections.emptyList(), Arrays.asList(cs, cr));
        clientSpan.setId("2");
        clientSpan.setTraceId("1");

        Annotation sr = new Annotation();
        sr.setValue("sr");
        Annotation ss = new Annotation();
        ss.setValue("ss");
        Span serverSpan = new Span(Collections.emptyList(), Arrays.asList(sr, ss));
        serverSpan.setId("2");
        serverSpan.setTraceId("1");
        Mockito.when(spanCacheMock.get(null, SpanUniqueIdGenerator.getClientId(serverSpan.getId())))
                .thenReturn(clientSpan);

        assertNull(nodeDetailsDeriver.processOneToOne(null, serverSpan));
    }

    @Test
    public void testServerSpanNoURLClientSpanWithURL() throws RetryAttemptException {
        SpanCache spanCacheMock = Mockito.mock(SpanCache.class);
        NodeDetailsDeriver nodeDetailsDeriver = new NodeDetailsDeriver(spanCacheMock);

        Annotation cs = new Annotation();
        cs.setValue("cs");
        Annotation cr = new Annotation();
        cr.setValue("cr");
        BinaryAnnotation httpAddress = new BinaryAnnotation();
        httpAddress.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        httpAddress.setValue("http://localhost/hello");
        Span clientSpan = new Span(Collections.singletonList(httpAddress), Arrays.asList(cs, cr));
        clientSpan.setId("2");
        clientSpan.setTraceId("1");

        Annotation sr = new Annotation();
        sr.setValue("sr");
        Annotation ss = new Annotation();
        ss.setValue("ss");
        Span serverSpan = new Span(Collections.emptyList(), Arrays.asList(sr, ss));
        serverSpan.setId("2");
        serverSpan.setTraceId("1");
        Mockito.when(spanCacheMock.get(null, SpanUniqueIdGenerator.getClientId(serverSpan.getId())))
                .thenReturn(clientSpan);

        NodeDetails result = nodeDetailsDeriver.processOneToOne(null, serverSpan);
        assertNotNull(result);
        assertEquals("/hello", result.getUri());
    }
}
