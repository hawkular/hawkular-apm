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

import javax.inject.Singleton;

import org.hawkular.apm.client.api.recorder.BatchTraceRecorder;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.client.api.sampler.Sampler;

import io.opentracing.impl.AbstractAPMTracer;

/**
 * The opentracing compatible Tracer implementation for Hawkular APM.
 *
 * @author gbrown
 */
@Singleton
public class APMTracer extends AbstractAPMTracer {

    public APMTracer() {
        this(new BatchTraceRecorder(), Sampler.ALWAYS_SAMPLE, DeploymentMetaData.getInstance());
    }

    public APMTracer(TraceRecorder recorder) {
        this(recorder, Sampler.ALWAYS_SAMPLE, DeploymentMetaData.getInstance());
    }

    public APMTracer(TraceRecorder recorder, Sampler sampler) {
        this(recorder, sampler, DeploymentMetaData.getInstance());
    }

    public APMTracer(TraceRecorder recorder, Sampler sampler, DeploymentMetaData deploymentMetaData) {
        super(new EnvironmentAwareTraceRecorder(recorder, deploymentMetaData), sampler);
    }
}
