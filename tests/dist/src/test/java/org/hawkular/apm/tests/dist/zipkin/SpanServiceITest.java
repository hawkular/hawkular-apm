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

package org.hawkular.apm.tests.dist.zipkin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
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
        traceService = new TraceServiceRESTClient();
        traceService.setUsername(HAWKULAR_APM_USERNAME);
        traceService.setPassword(HAWKULAR_APM_PASSWORD);
    }

    @Before
    public void before() {
        traceService.clear(null);
    }

    @Test
    public void testStoreSpanGetTrace() throws IOException {
        Span span = new Span(null,  serverAnnotations(millisToMicroSeconds(1), millisToMicroSeconds(30)));
        span.setTimestamp(millisToMicroSeconds(1L));
        span.setDuration(calculateDuration(1L, 30L));
        span.setTraceId("root");
        span.setId("root");
        span.setName("get");

        post(Server.Zipkin, "/spans", null, Arrays.asList(span));

        Wait.until(() -> traceService.getTrace(null, "root") != null);

        Trace trace = traceService.getTrace(null, "root");
        Assert.assertEquals("root", trace.getId());
        Assert.assertEquals(1, trace.getNodes().size());
        Node node = trace.getNodes().get(0);
        Assert.assertEquals(NodeType.Consumer, node.getType());
        CorrelationIdentifier correlationIdentifier = node.getCorrelationIds().get(0);
        Assert.assertEquals("root", correlationIdentifier.getValue());
        Assert.assertEquals(CorrelationIdentifier.Scope.Interaction, correlationIdentifier.getScope());
    }

    static List<Annotation> clientAnnotation(Long timestampCS, Long timestampCR) {
        Annotation cs = new Annotation();
        cs.setValue("cs");
        cs.setTimestamp(timestampCS);

        Annotation cr = new Annotation();
        cr.setValue("cr");
        cr.setTimestamp(timestampCR);
        return Arrays.asList(cs, cr);
    }

    static List<Annotation> serverAnnotations(Long timestampSR, Long timestampSS) {
        Annotation sr = new Annotation();
        sr.setValue("sr");
        sr.setTimestamp(timestampSR);

        Annotation ss = new Annotation();
        ss.setValue("ss");
        ss.setTimestamp(timestampSS);
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
