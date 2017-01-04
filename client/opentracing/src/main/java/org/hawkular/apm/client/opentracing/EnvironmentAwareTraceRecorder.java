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

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.client.api.recorder.TraceRecorder;

/**
 * @author Juraci Paixão Kröhling
 */
public class EnvironmentAwareTraceRecorder implements TraceRecorder {
    private TraceRecorder backingTraceRecorder;
    private DeploymentMetaData deploymentMetaData;

    public EnvironmentAwareTraceRecorder(TraceRecorder backingTraceRecorder, DeploymentMetaData deploymentMetaData) {
        this.deploymentMetaData = deploymentMetaData;
        this.backingTraceRecorder = backingTraceRecorder;
    }

    public void record(Trace trace) {
        if (trace.getNodes() != null && trace.getNodes().size() > 0) {
            Node rootNode = trace.getNodes().get(0);
            if (this.deploymentMetaData != null) {
                if (this.deploymentMetaData.getServiceName() != null
                        && !rootNode.hasProperty(Constants.PROP_SERVICE_NAME)) {
                    rootNode.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, this.deploymentMetaData.getServiceName()));
                }
                if (this.deploymentMetaData.getBuildStamp() != null
                        && !rootNode.hasProperty(Constants.PROP_BUILD_STAMP)) {
                    rootNode.getProperties().add(new Property(Constants.PROP_BUILD_STAMP, this.deploymentMetaData.getBuildStamp()));
                }
            }
        }

        backingTraceRecorder.record(trace);
    }

    public DeploymentMetaData getDeploymentMetaData() {
        return deploymentMetaData;
    }
}
