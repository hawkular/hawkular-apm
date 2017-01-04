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

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.server.api.model.zipkin.Span;

/**
 * @author Pavol Loffay
 */
public class CompletionTimeProcessing {

    /**
     * Root span of a trace
     */
    private Span rootSpan;

    /**
     * The last timestamp represents timestamp of annotation with biggest value
     * (e.g. the last event which happened in trace)
     */
    private Long lastTimestamp;

    /**
     * Trace completion time, if set processing is over
     */
    private CompletionTime completionTime;


    public CompletionTimeProcessing() {}

    public CompletionTimeProcessing(Span rootSpan) {
        this.rootSpan = rootSpan;
    }

    public Span getRootSpan() {
        return rootSpan;
    }

    public void setRootSpan(Span rootSpan) {
        this.rootSpan = rootSpan;
    }

    public Long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(Long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public CompletionTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(CompletionTime completionTime) {
        this.completionTime = completionTime;
    }
}
