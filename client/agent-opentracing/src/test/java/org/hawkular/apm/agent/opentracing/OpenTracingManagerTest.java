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
package org.hawkular.apm.agent.opentracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.opentracing.APMTracer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import io.opentracing.Span;

/**
 * @author gbrown
 */
public class OpenTracingManagerTest {

    private static final String OP1 = "OP1";
    private static final String OP2 = "OP2";

    private static final TestTraceRecorder recorder = new TestTraceRecorder();

    @BeforeClass
    public static void initClass() {
        System.setProperty(PropertyUtil.HAWKULAR_APM_AGENT_STATE_EXPIRY_INTERVAL, "2000");

        APMTracer tracer = (APMTracer) OpenTracingTracer.getSingleton();
        tracer.setTraceRecorder(recorder);
    }

    @Before
    public void clearTraces() {
        OpenTracingManager.reset();
        recorder.clear();
    }

    @Test
    public void testStartFinishSingleSpan() {
        OpenTracingManager otm = new OpenTracingManager(null);

        assertFalse(otm.hasSpan());

        otm.startSpan(otm.getTracer().buildSpan(OP1));

        // Check if has span
        assertTrue(otm.hasSpan());

        otm.finishSpan();

        assertFalse(otm.hasSpan());

        assertEquals(1, recorder.getTraces().size());
        assertEquals(1, recorder.getTraces().get(0).getNodes().size());
        assertEquals(OP1, recorder.getTraces().get(0).getNodes().get(0).getOperation());
        assertEquals(0, ((ContainerNode) recorder.getTraces().get(0).getNodes().get(0)).getNodes().size());
    }

    @Test
    public void testStartFinishMultiSpan() {
        OpenTracingManager otm = new OpenTracingManager(null);

        otm.startSpan(otm.getTracer().buildSpan(OP1));
        otm.startSpan(otm.getTracer().buildSpan(OP2));
        otm.finishSpan();

        assertEquals(0, recorder.getTraces().size());

        otm.finishSpan();

        assertEquals(1, recorder.getTraces().size());
        assertEquals(1, recorder.getTraces().get(0).getNodes().size());
        assertEquals(OP1, recorder.getTraces().get(0).getNodes().get(0).getOperation());
        assertEquals(1, ((ContainerNode) recorder.getTraces().get(0).getNodes().get(0)).getNodes().size());
        assertEquals(OP2,
                ((ContainerNode) recorder.getTraces().get(0).getNodes().get(0)).getNodes().get(0).getOperation());
    }

    @Test
    public void testSuspendResume() {
        OpenTracingManager otm = new OpenTracingManager(null);

        assertFalse(otm.hasSpan());

        otm.startSpan(otm.getTracer().buildSpan(OP1));

        // Check if has span
        assertTrue(otm.hasSpan());

        otm.suspend("1");

        // Check span no longer associated with thread
        assertFalse(otm.hasSpan());

        otm.resume("1");

        // Check if has span
        assertTrue(otm.hasSpan());

        otm.finishSpan();

        assertFalse(otm.hasSpan());

        assertEquals(1, recorder.getTraces().size());
    }

    @Test
    public void testSuspendedSpanExpire() throws InterruptedException {
        OpenTracingManager otm = new OpenTracingManager(null);

        assertFalse(otm.hasSpan());

        otm.startSpan(otm.getTracer().buildSpan(OP1));

        otm.suspend("1");

        // Check span no longer associated with thread
        assertFalse(otm.hasSpan());

        synchronized (this) {
            wait(4000);
        }

        otm.resume("1");

        // Check that span hasn't been resumed
        assertFalse(otm.hasSpan());
    }

    @Test
    public void testPathInclude() {
        OpenTracingManager otm = new OpenTracingManager(null);

        assertTrue(otm.includePath("/path/to/anything"));
        assertTrue(otm.includePath("/path.to/anything"));
        assertTrue(otm.includePath("/path.to/anything.jsp"));
        assertTrue(otm.includePath("anything"));

        assertFalse(otm.includePath("/hawkular/apm/anything"));
        assertFalse(otm.includePath("/path.to/anything.xjsp"));
        assertFalse(otm.includePath("myimage.png"));
        assertFalse(otm.includePath("/myimage.png"));
        assertFalse(otm.includePath("/location/myimage.png"));
    }

    @Test
    public void testPathIncludeWhitelist() {
        OpenTracingManager.fileExtensionWhitelist.add("foo");

        OpenTracingManager otm = new OpenTracingManager(null);

        assertTrue(otm.includePath("/quux/test.foo"));
        assertFalse(otm.includePath("/my/image.bar"));

        OpenTracingManager.fileExtensionWhitelist.clear();
    }

    @Test
    public void testCurrentSpanId() {
        OpenTracingManager.TraceState ts = new OpenTracingManager.TraceState();
        ts.pushSpan(Mockito.mock(Span.class), "1");
        assertEquals("1", ts.peekId());
    }

    @Test
    public void testHasSpanForId() {
        OpenTracingManager.TraceState ts = new OpenTracingManager.TraceState();
        Span span = Mockito.mock(Span.class);
        ts.pushSpan(span, "1");
        ts.popSpan();
        assertNull(ts.peekId());
        assertEquals(span, ts.getSpanForId("1"));
    }

}
