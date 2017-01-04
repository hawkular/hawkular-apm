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

package org.hawkular.apm.tests.dist.zipkin;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.utils.zipkin.ZipkinSpanConvertor;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.tests.dist.AbstractITest;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class SpanServiceITest extends AbstractITest {

    private static TraceServiceRESTClient traceService;

    @BeforeClass
    public static void beforeClass() {
        traceService = new TraceServiceRESTClient(HAWKULAR_APM_USERNAME, HAWKULAR_APM_PASSWORD, HAWKULAR_APM_URI);
    }

    @Before
    public void before() {
        traceService.clear(null);
    }

    @Test
    public void testStoreSpanGetTrace() throws IOException {
        final long traceId = UUID.randomUUID().getMostSignificantBits();
        final String traceStringId = ZipkinSpanConvertor.parseSpanId(traceId);
        zipkin.Span span = zipkin.Span.builder()
                .annotations(serverAnnotations(millisToMicroSeconds(1), millisToMicroSeconds(30)))
                .timestamp(millisToMicroSeconds(1))
                .duration(calculateDuration(1L, 30L))
                .traceId(traceId)
                .id(traceId)
                .name("get")
                .build();

        post(Server.Zipkin, "/spans", null, Arrays.asList(span));
        Wait.until(() -> traceService.getTrace(null, traceStringId) != null);

        Trace trace = traceService.getTrace(null, traceStringId);
        Assert.assertEquals(traceStringId, trace.getFragmentId());
        Assert.assertEquals(1, trace.getNodes().size());
        Node node = trace.getNodes().get(0);
        Assert.assertEquals(NodeType.Consumer, node.getType());
        CorrelationIdentifier correlationIdentifier = node.getCorrelationIds().get(0);
        Assert.assertEquals(traceStringId, correlationIdentifier.getValue());
        Assert.assertEquals(CorrelationIdentifier.Scope.Interaction, correlationIdentifier.getScope());
    }

    @Test
    public void testGZIPEncodedSpans() throws IOException {
        final long traceId = UUID.randomUUID().getMostSignificantBits();
        final String traceStringId = ZipkinSpanConvertor.parseSpanId(traceId);
        zipkin.Span span = zipkin.Span.builder()
                .traceId(traceId)
                .id(traceId)
                .name("get")
                .build();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Encoding", "gzip");

        post(Server.Zipkin, "/spans", null, Arrays.asList(span), headers);
        Wait.until(() -> traceService.getTrace(null, traceStringId) != null);

        Trace trace = traceService.getTrace(null, traceStringId);
        Assert.assertEquals(traceStringId, trace.getTraceId());
    }

    static List<zipkin.Annotation> clientAnnotation(Long timestampCS, Long timestampCR) {
        zipkin.Annotation cs = zipkin.Annotation.builder()
                .value("cs")
                .timestamp(timestampCS)
                .build();

        zipkin.Annotation cr = zipkin.Annotation.builder()
                .value("cr")
                .timestamp(timestampCR)
                .build();

        return Arrays.asList(cs, cr);
    }

    static List<zipkin.Annotation> serverAnnotations(Long timestampSR, Long timestampSS) {
        zipkin.Annotation sr = zipkin.Annotation.builder()
                .value("sr")
                .timestamp(timestampSR)
                .build();

        zipkin.Annotation ss = zipkin.Annotation.builder()
                .value("ss")
                .timestamp(timestampSS)
                .build();

        return Arrays.asList(sr, ss);
    }

    static Long millisToMicroSeconds(long millis) {
        return TimeUnit.MICROSECONDS.convert(millis, TimeUnit.MILLISECONDS);
    }

    static Long calculateDuration(Long startMillis, Long endMillis) {
        if (startMillis == null || endMillis == null) {
            return null;
        }
        return millisToMicroSeconds(endMillis) - millisToMicroSeconds(startMillis);
    }
}
