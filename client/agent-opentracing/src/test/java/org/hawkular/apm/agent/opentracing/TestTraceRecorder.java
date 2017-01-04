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

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.client.api.recorder.TraceRecorder;

/**
 * @author gbrown
 */
public class TestTraceRecorder implements TraceRecorder {

    private List<Trace> traces = new ArrayList<>();

    @Override
    public void record(Trace trace) {
        traces.add(trace);
    }

    public List<Trace> getTraces() {
        return traces;
    }

    public void clear() {
        traces.clear();
    }
}
