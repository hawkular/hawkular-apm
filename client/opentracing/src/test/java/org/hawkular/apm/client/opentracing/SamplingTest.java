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

package org.hawkular.apm.client.opentracing;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.client.api.sampler.Sampler;
import org.junit.Assert;
import org.junit.Test;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
public class SamplingTest {

    @Test
    public void testNeverSample() {
        APMTracerTest.TestTraceRecorder traceRecorder = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(traceRecorder, Sampler.NEVER_SAMPLE);

        Span span = tracer.buildSpan("foo")
                .start();

        span.finish();
        Assert.assertEquals(0, traceRecorder.getTraces().size());
    }

    @Test
    public void testNeverSampleExtractedSampleAllContext() {
        APMTracerTest.TestTraceRecorder traceRecorder = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(traceRecorder, Sampler.NEVER_SAMPLE);

        Span span = tracer.buildSpan("foo")
                .asChildOf(extractedTraceState(tracer, ReportingLevel.All))
                .start();

        span.finish();
        Assert.assertEquals(1, traceRecorder.getTraces().size());
    }

    @Test
    public void testAlwaysSample() {
        APMTracerTest.TestTraceRecorder traceRecorder = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(traceRecorder, Sampler.ALWAYS_SAMPLE);

        Span span = tracer.buildSpan("foo")
                .start();

        span.finish();
        Assert.assertEquals(1, traceRecorder.getTraces().size());
    }

    @Test
    public void testAlwaysSampleExtractedSampleNoneContext() {
        APMTracerTest.TestTraceRecorder traceRecorder = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(traceRecorder, Sampler.ALWAYS_SAMPLE);

        Span span = tracer.buildSpan("foo")
                .asChildOf(extractedTraceState(tracer, ReportingLevel.None))
                .start();

        span.finish();
        Assert.assertEquals(0, traceRecorder.getTraces().size());
    }

    @Test
    public void testSamplingPriorityChangedToZero() {
        APMTracerTest.TestTraceRecorder traceRecorder = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(traceRecorder, Sampler.ALWAYS_SAMPLE);

        Span rootSpan = tracer.buildSpan("foo")
                .asChildOf(extractedTraceState(tracer, ReportingLevel.All))
                .start();

        Span descendant = tracer.buildSpan("foo")
                .asChildOf(rootSpan)
                .start();

        Map<String, String> carrier = new HashMap<>();
        tracer.inject(descendant.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(carrier));
        Assert.assertEquals(ReportingLevel.All.name(), carrier.get(Constants.HAWKULAR_APM_LEVEL));

        Span descendantZeroSampling = tracer.buildSpan("foo")
                .asChildOf(rootSpan)
                .start();

        descendantZeroSampling.setTag(Tags.SAMPLING_PRIORITY.getKey(), 0);
        carrier.clear();
        tracer.inject(descendantZeroSampling.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(carrier));
        Assert.assertEquals(ReportingLevel.None.name(), carrier.get(Constants.HAWKULAR_APM_LEVEL));

        descendantZeroSampling.finish();
        descendant.finish();
        rootSpan.finish();

        Assert.assertEquals(1, traceRecorder.getTraces().size());
        traceRecorder.clear();

        Span descendantDescendantZeroSampling = tracer.buildSpan("foo")
                .addReference(References.FOLLOWS_FROM, descendantZeroSampling.context())
                .start();

        carrier.clear();
        tracer.inject(descendantDescendantZeroSampling.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(carrier));
        Assert.assertEquals(ReportingLevel.None.name(), carrier.get(Constants.HAWKULAR_APM_LEVEL));

        descendantDescendantZeroSampling.finish();
        Assert.assertEquals(0, traceRecorder.getTraces().size());
    }

    @Test
    public void testSamplingPriorityChangedToOne() {
        APMTracerTest.TestTraceRecorder traceRecorder = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(traceRecorder, Sampler.ALWAYS_SAMPLE);

        Span rootSpan = tracer.buildSpan("foo")
                .asChildOf(extractedTraceState(tracer, ReportingLevel.None))
                .start();

        Span descendant = tracer.buildSpan("foo")
                .asChildOf(rootSpan)
                .start();

        Map<String, String> carrier = new HashMap<>();
        tracer.inject(descendant.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(carrier));
        Assert.assertEquals(ReportingLevel.None.name(), carrier.get(Constants.HAWKULAR_APM_LEVEL));

        Span descendantOneSampling = tracer.buildSpan("foo")
                .asChildOf(rootSpan)
                .start();

        descendantOneSampling.setTag(Tags.SAMPLING_PRIORITY.getKey(), 1);
        carrier.clear();
        tracer.inject(descendantOneSampling.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(carrier));
        Assert.assertEquals(ReportingLevel.All.name(), carrier.get(Constants.HAWKULAR_APM_LEVEL));

        descendantOneSampling.finish();
        descendant.finish();
        rootSpan.finish();

        Span descendantDescendantOneSampling = tracer.buildSpan("foo")
                .addReference(References.FOLLOWS_FROM, descendantOneSampling.context())
                .start();

        carrier.clear();
        tracer.inject(descendantDescendantOneSampling.context(), Format.Builtin.TEXT_MAP, new TextMapInjectAdapter(carrier));
        Assert.assertEquals(ReportingLevel.All.name(), carrier.get(Constants.HAWKULAR_APM_LEVEL));

        descendantDescendantOneSampling.finish();
        Assert.assertEquals(2, traceRecorder.getTraces().size());
    }

    private SpanContext extractedTraceState(Tracer tracer, ReportingLevel reportingLevel) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HAWKULAR_APM_TRACEID, "foo");
        headers.put(Constants.HAWKULAR_APM_ID, "foo");
        headers.put(Constants.HAWKULAR_APM_LEVEL, reportingLevel.toString());

        return tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(headers));
    }
}
