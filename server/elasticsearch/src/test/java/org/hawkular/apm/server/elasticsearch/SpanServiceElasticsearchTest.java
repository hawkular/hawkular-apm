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

package org.hawkular.apm.server.elasticsearch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.AnnotationType;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Endpoint;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class SpanServiceElasticsearchTest {

    private SpanService spanService;

    @BeforeClass
    public static void initClass() {
        System.setProperty("HAWKULAR_APM_CONFIG_DIR", "target");
    }

    @Before
    public void beforeTest() {
        spanService = new SpanServiceElasticsearch();
    }

    @After
    public void afterTest() {
        spanService.clear(null);
    }

    @Test
    public void testStoreOneAndGetOne() throws StoreException, InterruptedException {
        Annotation annotation = new Annotation();
        annotation.setValue("foo bar");
        annotation.setTimestamp(123456789L);
        annotation.setEndpoint(createEndpoint("123.123.123.1233", 123, "foo service"));

        BinaryAnnotation binaryAnnotation = new BinaryAnnotation();
        binaryAnnotation.setEndpoint(createEndpoint("123.123.123.1233", 123, "foo service"));
        binaryAnnotation.setValue("foo");
        binaryAnnotation.setKey("foo key");
        binaryAnnotation.setType(AnnotationType.I64);

        Span span = new Span();
        span.setId("id");
        span.setTraceId("traceId");
        span.setParentId("parentId");
        span.setName("foo");
        span.setDebug(true);
        span.setTimestamp(1234456L);
        span.setDuration(55468L);
        span.setAnnotations(Arrays.asList(annotation));
        span.setBinaryAnnotations(Arrays.asList(binaryAnnotation));

        storeAndWait(null, span);
        Span spanFromDb = spanService.getSpan(null, span.getId());

        assertSpansEquals(span, spanFromDb);
    }

    @Test
    public void testGetChildren() throws StoreException, InterruptedException {
        Span parentSpan = new Span();
        parentSpan.setId("parent");
        parentSpan.setName("foo parent");

        storeAndWait(null, parentSpan);

        Annotation annotation = new Annotation();
        annotation.setValue("foo bar");
        annotation.setTimestamp(123456789);
        annotation.setEndpoint(createEndpoint("123.123.123.1233", 123, "foo service"));

        BinaryAnnotation binaryAnnotation = new BinaryAnnotation();
        binaryAnnotation.setEndpoint(createEndpoint("123.123.123.1233", 123, "foo service"));
        binaryAnnotation.setValue("foo");
        binaryAnnotation.setKey("foo key");
        binaryAnnotation.setType(AnnotationType.I64);

        Span span = new Span();
        span.setId("id1");
        span.setTraceId("traceId");
        span.setParentId("parent");
        span.setName("foo");
        span.setDebug(true);
        span.setTimestamp(1234456L);
        span.setDuration(55468L);
        span.setAnnotations(Arrays.asList(annotation));
        span.setBinaryAnnotations(Arrays.asList(binaryAnnotation));

        storeAndWait(null, span);

        span = new Span();
        span.setId("id2");
        span.setParentId("parent");

        storeAndWait(null, span);

        List<Span> children = spanService.getChildren(null, "parent");

        Assert.assertEquals(2, children.size());
    }

    @Test
    public void testStoreWithIdSupplier() throws StoreException, InterruptedException {
        final String clientIdsuffix = "-client";

        Span clientSpan = new Span();
        clientSpan.setId("foo");
        clientSpan.setDuration(111);
        clientSpan.setAnnotations(annotations());

        storeAndWait(null, clientSpan, x -> x.getId() + clientIdsuffix);

        Span span = new Span();
        span.setId("foo");
        span.setDuration(444);

        storeAndWait(null, span, x -> x.getId());

        Span spanFromDb = spanService.getSpan(null, "foo");
        assertSpansEquals(span, spanFromDb);

        spanFromDb = spanService.getSpan(null, "foo" + clientIdsuffix);
        assertSpansEquals(clientSpan, spanFromDb);
    }

    @Test
    public void testGetChildrenFromEmptyDB() throws StoreException, InterruptedException {
        List<Span> spans = spanService.getChildren(null, "id");
        Assert.assertTrue(spans.isEmpty());
    }

    @Test
    public void testGetChildrenTenantIndexExists() throws StoreException, InterruptedException {
        storeAndWait(null, new Span());
        List<Span> spans = spanService.getChildren(null, "id");
        Assert.assertTrue(spans.isEmpty());
    }

    @Test
    public void testGetSpanFromEmptyDB() throws StoreException, InterruptedException {
        Span spanFromDB = spanService.getSpan(null, "id");
        Assert.assertNull(spanFromDB);
    }

    private void storeAndWait(String tenant, Span span) throws StoreException, InterruptedException {
        spanService.storeSpan(tenant, Arrays.asList(span));
        synchronized (this) {
            wait(1000);
        }
    }

    private void storeAndWait(String tenant, Span span, Function<Span, String> idSupplier) throws StoreException,
            InterruptedException {
        spanService.storeSpan(tenant, Arrays.asList(span), idSupplier);
        synchronized (this) {
            wait(1000);
        }
    }

    private Endpoint createEndpoint(String ipv4, int port, String serviceName) {
        Endpoint endpoint = new Endpoint();
        endpoint.setIpv4(ipv4);
        endpoint.setPort(port);
        endpoint.setServiceName(serviceName);

        return endpoint;
    }

    private void assertSpansEquals(Span expected, Span actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getTraceId(), actual.getTraceId());
        Assert.assertEquals(expected.getParentId(), actual.getParentId());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getDuration(), actual.getDuration());
        Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());
        Assert.assertEquals(expected.getDebug(), actual.getDebug());

        Assert.assertEquals(expected.getAnnotations().size(), actual.getAnnotations().size());
        Assert.assertEquals(expected.getBinaryAnnotations().size(), actual.getBinaryAnnotations().size());

        for (int i = 0; i < expected.getAnnotations().size(); i++) {
            assertAnnotationsEquals(expected.getAnnotations().get(i), actual.getAnnotations().get(i));
        }
        for (int i = 0; i < expected.getBinaryAnnotations().size(); i++) {
            assertBinaryAnnotationsEquals(expected.getBinaryAnnotations().get(i), actual.getBinaryAnnotations().get(i));
        }
    }

    private void assertAnnotationsEquals(Annotation expected, Annotation actual) {
        Assert.assertEquals(expected.getTimestamp(), actual.getTimestamp());
        Assert.assertEquals(expected.getValue(), actual.getValue());

        assertEndpointsEquals(expected.getEndpoint(), actual.getEndpoint());
    }

    private void assertBinaryAnnotationsEquals(BinaryAnnotation expected, BinaryAnnotation actual) {
        Assert.assertEquals(expected.getValue(), actual.getValue());
        Assert.assertEquals(expected.getKey(), actual.getKey());
        Assert.assertEquals(expected.getType(), actual.getType());

        assertEndpointsEquals(expected.getEndpoint(), actual.getEndpoint());
    }

    private void assertEndpointsEquals(Endpoint expected, Endpoint actual) {
        if (expected == null && actual == null) {
            return;
        } else if (expected == null && actual != null || expected != null && actual == null) {
            Assert.fail();
        }

        Assert.assertEquals(expected.getServiceName(), actual.getServiceName());
        Assert.assertEquals(expected.getPort(), actual.getPort());
        Assert.assertEquals(expected.getIpv4(), actual.getIpv4());
    }

    private List<Annotation> annotations() {
        Annotation csAnnotation = new Annotation();
        csAnnotation.setValue("cs");
        Annotation crAnnotation = new Annotation();
        crAnnotation.setValue("cr");

        return Collections.unmodifiableList(Arrays.asList(csAnnotation, crAnnotation));
    }
}
