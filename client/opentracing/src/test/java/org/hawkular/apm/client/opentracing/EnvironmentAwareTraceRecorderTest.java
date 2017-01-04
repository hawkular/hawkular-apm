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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author gbrown
 */
public class EnvironmentAwareTraceRecorderTest {

    @Test
    public void testDeploymentMetaDataIncluded() {
        assertEquals(new HashSet<>(Arrays.asList(new Property(Constants.PROP_SERVICE_NAME, "myService"),
                new Property(Constants.PROP_BUILD_STAMP, "myBuildStamp"))),
                getTraceRootNodeProperties(new DeploymentMetaData("myService", "myBuildStamp")));
    }

    @Test
    public void testDeploymentMetaDataNotSet() {
        assertTrue(getTraceRootNodeProperties(new DeploymentMetaData(null, null)).isEmpty());
    }

    protected Set<Property> getTraceRootNodeProperties(DeploymentMetaData dmd) {
        TraceRecorder mock = Mockito.mock(TraceRecorder.class);
        EnvironmentAwareTraceRecorder eatr = new EnvironmentAwareTraceRecorder(mock, dmd);

        Trace trace = new Trace();
        Component component = new Component();
        trace.getNodes().add(component);

        eatr.record(trace);

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);
        Mockito.verify(mock).record(traceCaptor.capture());

        Trace result = traceCaptor.getValue();
        Component resultcomp = (Component) result.getNodes().get(0);

        return resultcomp.getProperties();
    }
}
