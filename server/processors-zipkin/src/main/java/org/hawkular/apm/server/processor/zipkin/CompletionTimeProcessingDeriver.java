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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.jboss.logging.Logger;

/**
 * Processing deriver of completion time of trace reported by zipkin instrumentation.
 *
 * @author Pavol Loffay
 */
public class CompletionTimeProcessingDeriver extends AbstractProcessor<CompletionTimeProcessing, CompletionTimeProcessing>{

    private static final Logger log = Logger.getLogger(CompletionTimeProcessingDeriver.class);

    private final SpanCache spanCache;


    public CompletionTimeProcessingDeriver(SpanCache spanCache) {
        super(ProcessorType.OneToOne);
        this.spanCache = spanCache;
    }


    @Override
    public CompletionTimeProcessing processOneToOne(String tenantId, CompletionTimeProcessing completionTimeProcessing)
            throws RetryAttemptException{

        // end processing if the completion time has been calculated
        if (completionTimeProcessing.getCompletionTime() != null) {
            return null;
        }

        Span rootSpan = completionTimeProcessing.getRootSpan();

        Set<Span> trace = spanCache.getTrace(tenantId, rootSpan.getTraceId());
        if (trace == null) {
            throw new RetryAttemptException("trace not present in the cache");
        }

        Annotation lastAnnotation = extractLastAnnotation(trace);
        if (lastAnnotation == null) {
            throw new RetryAttemptException("No annotation present, cannot calculate completion time - trying retry");
        }

        /**
         * TODO HWKAPM-348
         *
         * When there is new information recorded in trace (annotation with bigger timestamp)
         * then wait for a fixed amount of time {@link #getDeliveryDelay(List)} to calculate
         * trace completion time for all reported spans (async span can be recorded any time after root
         * span has been recorded). This approach does not deal with long run async spans,
         * because it waits only for a fixed amount of time.
         */
        if (completionTimeProcessing.getLastTimestamp() == null ||
                completionTimeProcessing.getLastTimestamp() < lastAnnotation.getTimestamp()) {

            completionTimeProcessing.setLastTimestamp(lastAnnotation.getTimestamp());
            return completionTimeProcessing;
        }

        CompletionTime completionTime = CompletionTimeUtil.spanToCompletionTime(spanCache, rootSpan);
        if (completionTime == null) {
            log.warnf("NO URL, span = %s", rootSpan);
            return null;
        }

        if (completionTime.getUri() == null) {
            if (rootSpan.serverSpan() &&
                    spanCache.get(null, SpanUniqueIdGenerator.getClientId(rootSpan.getId())) == null) {
                throw new RetryAttemptException("URL is null, span id = " + rootSpan.getId());
            }
        }

        completionTime.setProperties(extractProperties(trace));
        completionTime.setDuration(getTraceDuration(rootSpan, lastAnnotation.getTimestamp()));

        completionTimeProcessing.setCompletionTime(completionTime);
        return completionTimeProcessing;
    }

    @Override
    public long getRetryDelay(List<CompletionTimeProcessing> completionTimeProcessings, int retryCount) {
        // TODO HWKAPM-348
        return 5000;
    }

    @Override
    public long getDeliveryDelay(List<CompletionTimeProcessing> completionTimeProcessings) {
        // TODO HWKAPM-348
        return 5000;
    }

    private Long getTraceDuration(Span rootSpan, long lastAnnotationTimestamp) {
        /**
         * first estimate is last timestamp - first timestamp (of root span)
         */
        long estimatedDuration = lastAnnotationTimestamp - rootSpan.getTimestamp();

        /**
         * first estimate is only used if is bigger than root span duration. Instrumentation is
         * responsible for setting the duration so if it is bigger number use original duration of a root span.
         */
        if (estimatedDuration < rootSpan.getDuration()) {
            estimatedDuration = rootSpan.getDuration();
        }

        return estimatedDuration;
    }

    private Annotation extractLastAnnotation(Set<Span> spans) {
        Annotation lastAnnotation = null;

        for (Span span: spans) {
            for (Annotation annotation: span.getAnnotations()) {
                if (lastAnnotation == null ||
                        lastAnnotation.getTimestamp() < annotation.getTimestamp()) {
                    lastAnnotation = annotation;
                }
            }
        }

        return lastAnnotation;
    }

    private Set<Property> extractProperties(Set<Span> spans) {
        Set<Property> properties = new HashSet<>();

        for (Span span: spans) {
            properties.addAll(span.binaryAnnotationMapping().getProperties());

        }

        return properties;
    }
}
