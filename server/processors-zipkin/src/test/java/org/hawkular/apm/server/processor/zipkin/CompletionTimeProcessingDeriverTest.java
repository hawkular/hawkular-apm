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

import java.util.Arrays;
import java.util.HashSet;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Pavol Loffay
 */
public class CompletionTimeProcessingDeriverTest {

    @Test
    public void testSpanTraceCompletionTime() throws RetryAttemptException {
        SpanCache spanCacheMock = Mockito.mock(SpanCache.class);
        CompletionTimeProcessingDeriver completionTimeProcessingDeriver = new CompletionTimeProcessingDeriver(spanCacheMock);

        Annotation sr = new Annotation();
        sr.setValue("sr");
        sr.setTimestamp(0);
        Annotation ss = new Annotation();
        ss.setTimestamp(1000);
        ss.setValue("ss");
        BinaryAnnotation httpAddress = new BinaryAnnotation();
        httpAddress.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        httpAddress.setValue("http://localhost");
        Span rootSpan = new Span(Arrays.asList(httpAddress), Arrays.asList(sr, ss));
        rootSpan.setId("trace");
        rootSpan.setTraceId("trace");
        rootSpan.setTimestamp(0L);
        rootSpan.setDuration(ss.getTimestamp() - sr.getTimestamp());
        Mockito.when(spanCacheMock.getTrace(null, "trace")).thenReturn(new HashSet<>(Arrays.asList(rootSpan)));

        CompletionTimeProcessing completionTimeProcessing = new CompletionTimeProcessing(rootSpan);

        completionTimeProcessing =  completionTimeProcessingDeriver.processOneToOne(null, completionTimeProcessing);
        Assert.assertNull(completionTimeProcessing.getCompletionTime());

        completionTimeProcessing = completionTimeProcessingDeriver.processOneToOne(null, completionTimeProcessing);
        Assert.assertEquals(rootSpan.getDuration().longValue(), completionTimeProcessing.getCompletionTime().getDuration());

        completionTimeProcessing = completionTimeProcessingDeriver.processOneToOne(null, completionTimeProcessing);
        Assert.assertNull(completionTimeProcessing);

        sr = new Annotation();
        sr.setTimestamp(1500);
        ss = new Annotation();
        ss.setTimestamp(2000);
        Span descendant = new Span(null, Arrays.asList(sr, ss));
        descendant.setId("descendant");
        descendant.setTraceId("trace");
        descendant.setTimestamp(1500L);
        Mockito.when(spanCacheMock.getTrace(null, "trace"))
                .thenReturn(new HashSet<>(Arrays.asList(rootSpan, descendant)));

        completionTimeProcessing = new CompletionTimeProcessing(rootSpan);
        completionTimeProcessing = completionTimeProcessingDeriver.processOneToOne(null, completionTimeProcessing);
        Assert.assertNull(completionTimeProcessing.getCompletionTime());

        completionTimeProcessing = completionTimeProcessingDeriver.processOneToOne(null, completionTimeProcessing);
        Assert.assertEquals(2000, completionTimeProcessing.getCompletionTime().getDuration());

        completionTimeProcessing = completionTimeProcessingDeriver.processOneToOne(null, completionTimeProcessing);
        Assert.assertNull(completionTimeProcessing);

        Mockito.verify(spanCacheMock, Mockito.times(4)).getTrace(null, "trace");
    }
}
