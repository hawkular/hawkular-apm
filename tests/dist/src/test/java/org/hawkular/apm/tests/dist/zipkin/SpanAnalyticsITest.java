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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.analytics.service.rest.client.AnalyticsServiceRESTClient;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.model.zipkin.AnnotationType;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.tests.dist.AbstractITest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class SpanAnalyticsITest extends AbstractITest {

    private static AnalyticsServiceRESTClient analyticsService;

    @BeforeClass
    public static void beforeClass() {
        analyticsService = new AnalyticsServiceRESTClient();
        analyticsService.setUsername(HAWKULAR_APM_USERNAME);
        analyticsService.setPassword(HAWKULAR_APM_PASSWORD);
    }

    @Before
    public void beforeTest() {
        analyticsService.clear(null);
    }

    @Test
    public void testCompletionTimeSyncCalls() throws IOException {
        BinaryAnnotation urlRootBinAnn = new BinaryAnnotation();
        urlRootBinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlRootBinAnn.setValue("http://localhost/root");
        urlRootBinAnn.setType(AnnotationType.STRING);

        Span rootSpan = new Span(Arrays.asList(urlRootBinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(1), SpanServiceITest.millisToMicroSeconds(30)));
        rootSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(1L));
        rootSpan.setDuration(SpanServiceITest.calculateDuration(1L, 30L));
        rootSpan.setTraceId(UUID.randomUUID().toString());
        rootSpan.setId(rootSpan.getTraceId());
        rootSpan.setName("get");

        BinaryAnnotation urlServiceABinAnn = new BinaryAnnotation();
        urlServiceABinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlServiceABinAnn.setValue("http://localhost/serviceA");

        Span clientSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.clientAnnotation(SpanServiceITest.millisToMicroSeconds(5),
                        SpanServiceITest.millisToMicroSeconds(15)));
        clientSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(5));
        clientSpan.setDuration(SpanServiceITest.calculateDuration(5L, 15L));
        clientSpan.setTraceId(rootSpan.getTraceId());
        clientSpan.setId(UUID.randomUUID().toString());
        clientSpan.setParentId(rootSpan.getId());
        clientSpan.setName("post");

        Span serverSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(7), SpanServiceITest.millisToMicroSeconds(10)));
        serverSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(7));
        serverSpan.setDuration(SpanServiceITest.calculateDuration(7L, 10L));
        serverSpan.setTraceId(rootSpan.getTraceId());
        serverSpan.setId(clientSpan.getId());
        serverSpan.setParentId(rootSpan.getId());
        serverSpan.setName("post");

        post(Server.Zipkin, "/spans", null, Arrays.asList(serverSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(clientSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(rootSpan));

        Wait.until(() -> {
            List<CompletionTime> traceCompletionTimes = analyticsService.getTraceCompletionTimes(null, requestCriteria());
            return traceCompletionTimes == null || traceCompletionTimes.size() != 1 ? false : true;
        }, 30, TimeUnit.SECONDS);

        List<CompletionTime> traceCompletionTimes = analyticsService.getTraceCompletionTimes(null, requestCriteria());
        Assert.assertEquals(1, traceCompletionTimes.size());

        CompletionTime completionTime = traceCompletionTimes.get(0);
        Assert.assertEquals(rootSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(completionTime.getDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals("/root", completionTime.getUri());
        Assert.assertEquals(rootSpan.getId(), completionTime.getId());
    }

    @Test
    public void testCompletionTimeAsyncCalls() throws IOException, InterruptedException {
        BinaryAnnotation urlRootBinAnn = new BinaryAnnotation();
        urlRootBinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlRootBinAnn.setValue("http://localhost/root");
        urlRootBinAnn.setType(AnnotationType.STRING);

        Span rootSpan = new Span(Arrays.asList(urlRootBinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(1), SpanServiceITest.millisToMicroSeconds(5)));
        rootSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(1L));
        rootSpan.setDuration(SpanServiceITest.calculateDuration(1L, 5L));
        rootSpan.setTraceId(UUID.randomUUID().toString());
        rootSpan.setId(rootSpan.getTraceId());
        rootSpan.setName("get");

        BinaryAnnotation urlServiceABinAnn = new BinaryAnnotation();
        urlServiceABinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlServiceABinAnn.setValue("http://localhost/serviceA");

        Span clientSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.clientAnnotation(SpanServiceITest.millisToMicroSeconds(2),
                        SpanServiceITest.millisToMicroSeconds(4)));
        clientSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(2));
        clientSpan.setDuration(SpanServiceITest.calculateDuration(2L, 4L));
        clientSpan.setTraceId(rootSpan.getTraceId());
        clientSpan.setId(UUID.randomUUID().toString());
        clientSpan.setParentId(rootSpan.getId());
        clientSpan.setName("post");

        Span serverSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(3), SpanServiceITest.millisToMicroSeconds(10)));
        serverSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(3));
        serverSpan.setDuration(SpanServiceITest.calculateDuration(3L, 10L));
        serverSpan.setTraceId(rootSpan.getTraceId());
        serverSpan.setId(clientSpan.getId());
        serverSpan.setParentId(rootSpan.getId());
        serverSpan.setName("post");

        post(Server.Zipkin, "/spans", null, Arrays.asList(rootSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(clientSpan));
        Thread.sleep(2000);
        post(Server.Zipkin, "/spans", null, Arrays.asList(serverSpan));

        Wait.until(() -> {
            List<CompletionTime> traceCompletionTimes = analyticsService.getTraceCompletionTimes(null, requestCriteria());
            return traceCompletionTimes == null || traceCompletionTimes.size() != 1 ? false : true;
        }, 30, TimeUnit.SECONDS);

        List<CompletionTime> traceCompletionTimes = analyticsService.getTraceCompletionTimes(null, requestCriteria());
        Assert.assertEquals(1, traceCompletionTimes.size());

        CompletionTime completionTime = traceCompletionTimes.get(0);
        Assert.assertEquals(9000,
                TimeUnit.MICROSECONDS.convert(completionTime.getDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals("/root", completionTime.getUri());
        Assert.assertEquals(rootSpan.getId(), completionTime.getId());
    }

    @Test
    public void testCommunicationSummaryStatistics() throws IOException {
        BinaryAnnotation urlRootBinAnn = new BinaryAnnotation();
        urlRootBinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlRootBinAnn.setValue("http://localhost/root");
        urlRootBinAnn.setType(AnnotationType.STRING);

        Span rootSpan = new Span(Arrays.asList(urlRootBinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(1), SpanServiceITest.millisToMicroSeconds(30)));
        rootSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(1L));
        rootSpan.setDuration(SpanServiceITest.calculateDuration(1L, 30L));
        rootSpan.setTraceId(UUID.randomUUID().toString());
        rootSpan.setId(rootSpan.getTraceId());
        rootSpan.setName("get");

        BinaryAnnotation urlServiceABinAnn = new BinaryAnnotation();
        urlServiceABinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlServiceABinAnn.setValue("http://localhost/serviceA");

        Span clientSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.clientAnnotation(SpanServiceITest.millisToMicroSeconds(5),
                        SpanServiceITest.millisToMicroSeconds(15)));
        clientSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(5));
        clientSpan.setDuration(SpanServiceITest.calculateDuration(5L, 15L));
        clientSpan.setTraceId(rootSpan.getTraceId());
        clientSpan.setId(UUID.randomUUID().toString());
        clientSpan.setParentId(rootSpan.getId());
        clientSpan.setName("post");

        Span serverSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(7), SpanServiceITest.millisToMicroSeconds(10)));
        serverSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(7));
        serverSpan.setDuration(SpanServiceITest.calculateDuration(7L, 10L));
        serverSpan.setTraceId(rootSpan.getTraceId());
        serverSpan.setId(clientSpan.getId());
        serverSpan.setParentId(rootSpan.getId());
        serverSpan.setName("post");

        post(Server.Zipkin, "/spans", null, Arrays.asList(rootSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(clientSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(serverSpan));

        Wait.until(() -> {
            Collection<CommunicationSummaryStatistics> communicationSummaryStatistics =
                    analyticsService.getCommunicationSummaryStatistics(null, requestCriteria(), false);
            return communicationSummaryStatistics == null || communicationSummaryStatistics.size() != 2 ? false : true;
        }, 30, TimeUnit.SECONDS);

        List<CommunicationSummaryStatistics> communicationSummaryStatisticsList = new ArrayList<>(
                analyticsService.getCommunicationSummaryStatistics(null, requestCriteria(), false));
        Assert.assertEquals(2, communicationSummaryStatisticsList.size());

        CommunicationSummaryStatistics summaryStatistics = communicationSummaryStatisticsList.get(0);
        Assert.assertEquals(EndpointUtil.encodeEndpoint("/root", "GET"), summaryStatistics.getId());
        Assert.assertEquals(1, summaryStatistics.getCount());
        Assert.assertEquals(rootSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(summaryStatistics.getMinimumDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals(rootSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(summaryStatistics.getMaximumDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals(rootSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(summaryStatistics.getAverageDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals(1, summaryStatistics.getOutbound().size());
        Assert.assertTrue(summaryStatistics.getOutbound().containsKey(EndpointUtil.encodeEndpoint("/serviceA", "POST")));

        summaryStatistics = communicationSummaryStatisticsList.get(1);
        Assert.assertEquals(EndpointUtil.encodeEndpoint("/serviceA", "POST"), summaryStatistics.getId());
        Assert.assertEquals(1, summaryStatistics.getCount());
        Assert.assertEquals(serverSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(summaryStatistics.getMinimumDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals(serverSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(summaryStatistics.getMaximumDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals(serverSpan.getDuration(),
                TimeUnit.MICROSECONDS.convert(summaryStatistics.getAverageDuration(), TimeUnit.MILLISECONDS));
        Assert.assertEquals(0, summaryStatistics.getOutbound().size());
    }

    @Test
    public void testNodeSummaryStatistics() throws IOException {
        BinaryAnnotation urlRootBinAnn = new BinaryAnnotation();
        urlRootBinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlRootBinAnn.setValue("http://localhost/root");
        urlRootBinAnn.setType(AnnotationType.STRING);

        Span rootSpan = new Span(Arrays.asList(urlRootBinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(1), SpanServiceITest.millisToMicroSeconds(30)));
        rootSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(1L));
        rootSpan.setDuration(SpanServiceITest.calculateDuration(1L, 30L));
        rootSpan.setTraceId(UUID.randomUUID().toString());
        rootSpan.setId(rootSpan.getTraceId());
        rootSpan.setName("get");

        BinaryAnnotation urlServiceABinAnn = new BinaryAnnotation();
        urlServiceABinAnn.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        urlServiceABinAnn.setValue("http://localhost/serviceA");

        Span clientSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.clientAnnotation(SpanServiceITest.millisToMicroSeconds(5),
                        SpanServiceITest.millisToMicroSeconds(15)));
        clientSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(5));
        clientSpan.setDuration(SpanServiceITest.calculateDuration(5L, 15L));
        clientSpan.setTraceId(rootSpan.getTraceId());
        clientSpan.setId(UUID.randomUUID().toString());
        clientSpan.setParentId(rootSpan.getId());
        clientSpan.setName("post");

        Span serverSpan = new Span(Arrays.asList(urlServiceABinAnn),
                SpanServiceITest.serverAnnotations(
                        SpanServiceITest.millisToMicroSeconds(7), SpanServiceITest.millisToMicroSeconds(10)));
        serverSpan.setTimestamp(SpanServiceITest.millisToMicroSeconds(7));
        serverSpan.setDuration(SpanServiceITest.calculateDuration(7L, 10L));
        serverSpan.setTraceId(rootSpan.getTraceId());
        serverSpan.setId(clientSpan.getId());
        serverSpan.setParentId(rootSpan.getId());
        serverSpan.setName("post");

        post(Server.Zipkin, "/spans", null, Arrays.asList(rootSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(clientSpan));
        post(Server.Zipkin, "/spans", null, Arrays.asList(serverSpan));

        Wait.until(() -> {
            Collection<NodeSummaryStatistics> nodeSummaryStatistics =
                    analyticsService.getNodeSummaryStatistics(null, requestCriteria());
            return nodeSummaryStatistics == null || nodeSummaryStatistics.size() != 3 ? false : true;
        }, 30, TimeUnit.SECONDS);

        List<NodeSummaryStatistics> nodeSummaryStatisticsList = new ArrayList<>(
                analyticsService.getNodeSummaryStatistics(null, requestCriteria()));
        Assert.assertEquals(3, nodeSummaryStatisticsList.size());

        NodeSummaryStatistics nodeSummaryStatistics = nodeSummaryStatisticsList.get(0);
        Assert.assertEquals("/root", nodeSummaryStatistics.getUri());
        Assert.assertEquals("consumer", nodeSummaryStatistics.getComponentType());

        nodeSummaryStatistics = nodeSummaryStatisticsList.get(1);
        Assert.assertEquals("/serviceA", nodeSummaryStatistics.getUri());
        Assert.assertEquals("consumer", nodeSummaryStatistics.getComponentType());

        nodeSummaryStatistics = nodeSummaryStatisticsList.get(2);
        Assert.assertEquals("/serviceA", nodeSummaryStatistics.getUri());
        Assert.assertEquals("producer", nodeSummaryStatistics.getComponentType());
    }

    private Criteria requestCriteria() {
        return new Criteria().setStartTime(1).setEndTime(System.currentTimeMillis());
    }
}
