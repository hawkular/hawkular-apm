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

import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.InteractionNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.AnnotationType;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Endpoint;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanService;
import org.hawkular.apm.server.api.utils.SpanUniqueIdGenerator;
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

        storeAndWait(null, Collections.singletonList(span));
        Span spanFromDb = spanService.getSpan(null, span.getId());

        assertSpansEquals(span, spanFromDb);
    }

    @Test
    public void testGetChildren() throws StoreException, InterruptedException {
        Span parentSpan = new Span();
        parentSpan.setId("parent");
        parentSpan.setName("foo parent");

        storeAndWait(null, Collections.singletonList(parentSpan));

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

        storeAndWait(null, Collections.singletonList(span));

        span = new Span();
        span.setId("id2");
        span.setParentId("parent");

        storeAndWait(null, Collections.singletonList(span));

        List<Span> children = spanService.getChildren(null, "parent");

        Assert.assertEquals(2, children.size());
    }

    @Test
    public void testStoreWithIdSupplier() throws StoreException, InterruptedException {
        final String clientIdsuffix = "-client";

        Span clientSpan = new Span();
        clientSpan.setId("foo");
        clientSpan.setDuration(111);
        clientSpan.setAnnotations(clientAnnotations());

        storeAndWait(null, Collections.singletonList(clientSpan), x -> x.getId() + clientIdsuffix);

        Span span = new Span();
        span.setId("foo");
        span.setDuration(444);

        storeAndWait(null, Collections.singletonList(span), x -> x.getId());

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
        storeAndWait(null, Collections.singletonList(new Span()));
        List<Span> spans = spanService.getChildren(null, "id");
        Assert.assertTrue(spans.isEmpty());
    }

    @Test
    public void testGetSpanFromEmptyDB() throws StoreException, InterruptedException {
        Span spanFromDB = spanService.getSpan(null, "id");
        Assert.assertNull(spanFromDB);
    }

    @Test
    public void testGetTraceFragmentRootServerSpan() throws StoreException, InterruptedException {
        Span rootServerSpan = new Span();
        rootServerSpan.setId("root");
        rootServerSpan.setTraceId("root");
        rootServerSpan.setAnnotations(serverAnnotations());

        Span clientDescendant = new Span();
        clientDescendant.setId("descendant");
        clientDescendant.setParentId("root");
        clientDescendant.setTraceId("root");
        clientDescendant.setAnnotations(clientAnnotations());

        Span clientDescendant2 = new Span();
        clientDescendant2.setId("descendant2");
        clientDescendant2.setParentId("root");
        clientDescendant2.setTraceId("root");
        clientDescendant2.setAnnotations(clientAnnotations());

        Span serverDescendantOfDescendant2 = new Span();
        serverDescendantOfDescendant2.setId("descendant2");
        serverDescendantOfDescendant2.setParentId("root");
        serverDescendantOfDescendant2.setTraceId("root");
        serverDescendantOfDescendant2.setAnnotations(serverAnnotations());

        Span serverDescendantOfDescendant = new Span();
        serverDescendantOfDescendant.setId("descendant");
        serverDescendantOfDescendant.setParentId("root");
        serverDescendantOfDescendant.setTraceId("root");
        serverDescendantOfDescendant.setAnnotations(serverAnnotations());

        storeAndWait(null, Arrays.asList(rootServerSpan,
                clientDescendant,
                clientDescendant2,
                serverDescendantOfDescendant,
                serverDescendantOfDescendant2),
                SpanUniqueIdGenerator::toUnique);

        Trace trace = spanService.getTraceFragment(null, "root");
        Assert.assertEquals("root", trace.getId());
        Assert.assertEquals(1, trace.getNodes().size());

        InteractionNode rootConsumerNode = ((InteractionNode) trace.getNodes().get(0));
        Assert.assertThat(rootConsumerNode, instanceOf(Consumer.class));
        Assert.assertEquals(1, rootConsumerNode.getCorrelationIds().size());
        Assert.assertEquals("root", rootConsumerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(2, rootConsumerNode.getNodes().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("descendant", "descendant2")),
                extractCorrelationIds(rootConsumerNode.getNodes()));

        InteractionNode descendantProducerNode = ((InteractionNode) rootConsumerNode.getNodes().get(0));
        Assert.assertThat(descendantProducerNode, instanceOf(Producer.class));
        Assert.assertEquals(1, descendantProducerNode.getCorrelationIds().size());
        Assert.assertEquals("descendant", descendantProducerNode.getCorrelationIds().get(0).getValue());
        Assert.assertTrue(descendantProducerNode.getNodes().isEmpty());

        InteractionNode descendantProducerNode2 = ((InteractionNode) rootConsumerNode.getNodes().get(1));
        Assert.assertThat(descendantProducerNode2, instanceOf(Producer.class));
        Assert.assertEquals(1, descendantProducerNode2.getCorrelationIds().size());
        Assert.assertEquals("descendant2", descendantProducerNode2.getCorrelationIds().get(0).getValue());
        Assert.assertTrue(descendantProducerNode2.getNodes().isEmpty());
    }

    @Test
    public void testGetTraceFragmentClientRoot() throws StoreException, InterruptedException {
        Span rootClientSpan = new Span();
        rootClientSpan.setId("root");
        rootClientSpan.setTraceId("root");
        rootClientSpan.setName("rootClientSpan");
        rootClientSpan.setAnnotations(clientAnnotations());

        Span serverSpan = new Span();
        serverSpan.setId("root");
        serverSpan.setTraceId("root");
        serverSpan.setAnnotations(serverAnnotations());

        Span clientSpanDescendantRoot = new Span();
        serverSpan.setId("root2");
        serverSpan.setParentId("root");
        serverSpan.setTraceId("root");
        serverSpan.setAnnotations(clientAnnotations());

        Span serverSpanOfClientDescendantRoot = new Span();
        serverSpanOfClientDescendantRoot.setId("root2");
        serverSpanOfClientDescendantRoot.setParentId("root");
        serverSpanOfClientDescendantRoot.setTraceId("root");
        serverSpanOfClientDescendantRoot.setAnnotations(serverAnnotations());

        storeAndWait(null, Arrays.asList(
                rootClientSpan,
                serverSpan,
                clientSpanDescendantRoot,
                serverSpanOfClientDescendantRoot),
                SpanUniqueIdGenerator::toUnique);

        Trace trace = spanService.getTraceFragment(null, SpanUniqueIdGenerator.getClientId("root"));
        Assert.assertEquals("root", trace.getId());
        Assert.assertEquals(1, trace.getNodes().size());

        InteractionNode rootProducerNode = (InteractionNode) trace.getNodes().get(0);
        Assert.assertThat(rootProducerNode, instanceOf(Producer.class));
        Assert.assertEquals(1, rootProducerNode.getCorrelationIds().size());
        Assert.assertEquals("root", rootProducerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(0, rootProducerNode.getNodes().size());
    }

    @Test
    public void testGetTraceFragmentComponent() throws StoreException, InterruptedException {
        Span rootServerSpan = new Span();
        rootServerSpan.setId("root");
        rootServerSpan.setTraceId("root");
        rootServerSpan.setAnnotations(serverAnnotations());

        Span componentSpan = new Span();
        componentSpan.setId("component");
        componentSpan.setParentId("root");
        componentSpan.setTraceId("root");
        componentSpan.setName("ejb");

        Span clientComponentSpan = new Span();
        clientComponentSpan.setId("clientComponent");
        clientComponentSpan.setParentId("component");
        clientComponentSpan.setTraceId("root");
        clientComponentSpan.setAnnotations(clientAnnotations());

        Span clientComponentSpan2 = new Span();
        clientComponentSpan2.setId("clientComponent2");
        clientComponentSpan2.setParentId("component");
        clientComponentSpan2.setTraceId("root");
        clientComponentSpan2.setAnnotations(clientAnnotations());

        Span descendant = new Span();
        descendant.setId("descendant");
        descendant.setParentId("root");
        descendant.setTraceId("root");
        descendant.setAnnotations(clientAnnotations());

        Span descendantServer = new Span();
        descendantServer.setId("descendant");
        descendantServer.setParentId("root");
        descendantServer.setTraceId("root");
        descendantServer.setAnnotations(serverAnnotations());

        storeAndWait(null, Arrays.asList(
                rootServerSpan,
                componentSpan,
                clientComponentSpan,
                clientComponentSpan2,
                descendant,
                descendantServer),
                SpanUniqueIdGenerator::toUnique);

        Trace trace = spanService.getTraceFragment(null, "root");
        Assert.assertEquals("root", trace.getId());
        Assert.assertEquals(1, trace.getNodes().size());

        InteractionNode rootConsumerNode = (InteractionNode) trace.getNodes().get(0);
        Assert.assertThat(rootConsumerNode, instanceOf(Consumer.class));
        Assert.assertEquals(1, rootConsumerNode.getCorrelationIds().size());
        Assert.assertEquals("root", rootConsumerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(2, rootConsumerNode.getNodes().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("descendant")),
                extractCorrelationIds(rootConsumerNode.getNodes()));

        InteractionNode componentNode = (InteractionNode) rootConsumerNode.getNodes().get(0);
        Assert.assertThat(componentNode, instanceOf(Component.class));
        Assert.assertEquals(0, componentNode.getCorrelationIds().size());
        Assert.assertEquals(2, componentNode.getNodes().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("clientComponent", "clientComponent2")),
                extractCorrelationIds(componentNode.getNodes()));

        InteractionNode producerNode = (InteractionNode) rootConsumerNode.getNodes().get(1);
        Assert.assertThat(producerNode, instanceOf(Producer.class));
        Assert.assertEquals(1, producerNode.getCorrelationIds().size());
        Assert.assertEquals("descendant", producerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(0, producerNode.getNodes().size());
    }

    @Test
    public void testGetEndToEndTrace() throws StoreException, InterruptedException {
        Span rootServerSpan = new Span();
        rootServerSpan.setId("root");
        rootServerSpan.setTraceId("root");
        rootServerSpan.setAnnotations(serverAnnotations());

        Span clientDescendant = new Span();
        clientDescendant.setId("descendant");
        clientDescendant.setParentId("root");
        clientDescendant.setTraceId("root");
        clientDescendant.setAnnotations(clientAnnotations());

        Span clientDescendant2 = new Span();
        clientDescendant2.setId("descendant2");
        clientDescendant2.setParentId("root");
        clientDescendant2.setTraceId("root");
        clientDescendant2.setAnnotations(clientAnnotations());

        Span serverDescendantOfDescendant2 = new Span();
        serverDescendantOfDescendant2.setId("descendant2");
        serverDescendantOfDescendant2.setParentId("root");
        serverDescendantOfDescendant2.setTraceId("root");
        serverDescendantOfDescendant2.setAnnotations(serverAnnotations());

        Span serverDescendantOfDescendant = new Span();
        serverDescendantOfDescendant.setId("descendant");
        serverDescendantOfDescendant.setParentId("root");
        serverDescendantOfDescendant.setTraceId("root");
        serverDescendantOfDescendant.setAnnotations(serverAnnotations());

        storeAndWait(null, Arrays.asList(rootServerSpan,
                clientDescendant,
                clientDescendant2,
                serverDescendantOfDescendant,
                serverDescendantOfDescendant2),
                SpanUniqueIdGenerator::toUnique);

        Trace trace = spanService.getTrace(null, "root");
        Assert.assertEquals("root", trace.getId());
        Assert.assertEquals(1, trace.getNodes().size());

        InteractionNode rootConsumerNode = ((InteractionNode) trace.getNodes().get(0));
        Assert.assertThat(rootConsumerNode, instanceOf(Consumer.class));
        Assert.assertEquals(1, rootConsumerNode.getCorrelationIds().size());
        Assert.assertEquals("root", rootConsumerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(2, rootConsumerNode.getNodes().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("descendant", "descendant2")),
                extractCorrelationIds(rootConsumerNode.getNodes()));

        InteractionNode descendantProducerNode = ((InteractionNode) rootConsumerNode.getNodes().get(0));
        Assert.assertThat(descendantProducerNode, instanceOf(Producer.class));
        Assert.assertEquals(1, descendantProducerNode.getCorrelationIds().size());
        Assert.assertEquals("descendant", descendantProducerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(1, descendantProducerNode.getNodes().size());

        InteractionNode descendantConsumerNode = ((InteractionNode) descendantProducerNode.getNodes().get(0));
        Assert.assertThat(descendantConsumerNode, instanceOf(Consumer.class));
        Assert.assertEquals(1, descendantConsumerNode.getCorrelationIds().size());
        Assert.assertEquals("descendant", descendantConsumerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(0, descendantConsumerNode.getNodes().size());

        InteractionNode descendantProducerNode2 = ((InteractionNode) rootConsumerNode.getNodes().get(1));
        Assert.assertThat(descendantProducerNode2, instanceOf(Producer.class));
        Assert.assertEquals(1, descendantProducerNode2.getCorrelationIds().size());
        Assert.assertEquals("descendant2", descendantProducerNode2.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(1, descendantProducerNode2.getNodes().size());

        InteractionNode descendant2ConsumerNode = ((InteractionNode) descendantProducerNode2.getNodes().get(0));
        Assert.assertThat(descendant2ConsumerNode, instanceOf(Consumer.class));
        Assert.assertEquals(1, descendant2ConsumerNode.getCorrelationIds().size());
        Assert.assertEquals("descendant2", descendant2ConsumerNode.getCorrelationIds().get(0).getValue());
        Assert.assertEquals(0, descendant2ConsumerNode.getNodes().size());
    }

    private Set<String> extractCorrelationIds(List<Node> nodes) {
        return new HashSet<>(nodes.stream()
                .filter(node -> node.getType() != NodeType.Component)
                .map(node -> node.getCorrelationIds().get(0).getValue())
                .collect(Collectors.toList()));
    }

    private void storeAndWait(String tenant, List<Span> spans) throws StoreException, InterruptedException {
        spanService.storeSpan(tenant, spans);
    }

    private void storeAndWait(String tenant, List<Span> spans, Function<Span, String> idSupplier) throws StoreException,
            InterruptedException {
        spanService.storeSpan(tenant, spans, idSupplier);
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
