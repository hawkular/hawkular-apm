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

package org.hawkular.apm.server.api.utils.zipkin;

import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Endpoint;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class ZipkinSpanConvertorTest {

    @Test
    public void testConvertFromZipkinEndpoint() {
        zipkin.Endpoint zipkinEndpoint =  zipkin.Endpoint.builder()
                .serviceName("service")
                .ipv4(-1408106489)
                .port(80)
                .build();

        Endpoint endpoint = ZipkinSpanConvertor.endpoint(zipkinEndpoint);
        Assert.assertEquals("172.18.0.7", endpoint.getIpv4());
        Assert.assertEquals("service", endpoint.getServiceName());
        Assert.assertEquals(80, endpoint.getPort().intValue());
    }

    @Test
    public void testConvertSpan() {
        zipkin.Span zipkinSpan = zipkin.Span.builder()
                .name("get")
                .id(1)
                .traceId(1)
                .parentId(null)
                .debug(false)
                .timestamp(2L)
                .duration(2L)
                .addAnnotation(zipkin.Annotation.builder()
                    .timestamp(1)
                    .value("cs")
                    .build())
                .addBinaryAnnotation(zipkin.BinaryAnnotation.builder()
                    .key("key")
                    .value("value")
                    .build())
                .build();

        Span span = ZipkinSpanConvertor.span(zipkinSpan);
        Assert.assertEquals("get", span.getName());
        Assert.assertEquals("0000000000000001", span.getId());
        Assert.assertEquals("0000000000000001", span.getTraceId());
        Assert.assertNull(span.getParentId());
        Assert.assertEquals(false, span.getDebug());
        Assert.assertEquals(new Long(2), span.getTimestamp());
        Assert.assertEquals(new Long(2), span.getDuration());
        Assert.assertEquals(1, span.getAnnotations().size());
        Assert.assertEquals(1, span.getBinaryAnnotations().size());

        Annotation annotation = span.getAnnotations().get(0);
        Assert.assertEquals("cs", annotation.getValue());
        Assert.assertEquals(1, annotation.getTimestamp());

        BinaryAnnotation binaryAnnotation = span.getBinaryAnnotations().get(0);
        Assert.assertEquals("key", binaryAnnotation.getKey());
        Assert.assertEquals("value", binaryAnnotation.getValue());
    }

    @Test
    public void testConvertSpanDefaultTimestampAndDuration() {
        zipkin.Span zipkinSpan = zipkin.Span.builder()
                .id(1)
                .traceId(1)
                .name("get")
                .build();

        Span span = ZipkinSpanConvertor.span(zipkinSpan);
        Assert.assertEquals(null, span.getTimestamp());
        Assert.assertEquals(new Long(0), span.getDuration());
    }
}
