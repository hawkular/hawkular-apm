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

import java.util.Optional;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.sampler.Sampler;
import org.junit.Before;
import org.junit.Test;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * @author Juraci Paixão Kröhling
 */
public class DeploymentMetaDataTest {

    @Before
    public void cleanupPossibleConflictingEnvVars() {
        System.getProperties().remove(PropertyUtil.HAWKULAR_APM_SERVICE_NAME);
        System.getProperties().remove(PropertyUtil.HAWKULAR_APM_BUILDSTAMP);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE);
        DeploymentMetaData.reloadServiceName();
    }

    @Test
    public void convertToServiceName_usualCase() {
        assertEquals("hawkular-apm", DeploymentMetaData.getServiceFromBuildName("hawkular-apm-1"));
    }

    @Test
    public void convertToServiceName_reallyLargeBuildNumber() {
        assertEquals("hawkular-apm", DeploymentMetaData.getServiceFromBuildName("hawkular-apm-111111111111111111111111111111111111111111"));
    }

    @Test
    public void convertToServiceName_withoutNumber() {
        // this looks wrong, but we have to decide what to do here: in *all* currently known situations, the build name is a composition
        // of {service_name}-{build_number}, so, we won't try to be too smart here... This test is here to document this conscious decision
        assertEquals("hawkular", DeploymentMetaData.getServiceFromBuildName("hawkular-apm"));
    }

    @Test
    public void convertToServiceName_empty() {
        assertEquals("", DeploymentMetaData.getServiceFromBuildName(""));
    }

    @Test
    public void convertToServiceName_nil() {
        assertEquals(null, DeploymentMetaData.getServiceFromBuildName(null));
    }


    @Test
    public void retrieveServiceName_noExplicitPropertyOutsideOpenShift() {
        // this is probably the most common case: there's no service name and we can't guess one, so, we just return null.
        assertEquals(null, DeploymentMetaData.getServiceNameFromEnv());
        assertEquals(null, DeploymentMetaData.getBuildStampFromEnv());
    }

    @Test
    public void retrieveServiceName_explicitPropertyOutsideOpenShift() {
        System.setProperty(PropertyUtil.HAWKULAR_APM_SERVICE_NAME, "foobar");
        assertEquals("foobar", DeploymentMetaData.getServiceNameFromEnv());
        assertEquals(null, DeploymentMetaData.getBuildStampFromEnv());
        System.getProperties().remove(PropertyUtil.HAWKULAR_APM_SERVICE_NAME);
    }

    @Test
    public void retrieveServiceName_emptyExplicitPropertyOutsideOpenShift() {
        System.setProperty(PropertyUtil.HAWKULAR_APM_SERVICE_NAME, "");
        assertEquals(null, DeploymentMetaData.getServiceNameFromEnv());
        assertEquals(null, DeploymentMetaData.getBuildStampFromEnv());
        System.getProperties().remove(PropertyUtil.HAWKULAR_APM_SERVICE_NAME);
    }

    @Test
    public void retrieveServiceName_noExplicitPropertyInsideOpenShift() {
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAME, "hawkular-apm-1");
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE, "myproject");
        assertEquals("myproject.hawkular-apm", DeploymentMetaData.getServiceNameFromEnv());
        assertEquals("myproject.hawkular-apm-1", DeploymentMetaData.getBuildStampFromEnv());
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE);
    }

    @Test
    public void retrieveServiceName_explicitPropertyInsideOpenShift() {
        System.setProperty(PropertyUtil.HAWKULAR_APM_SERVICE_NAME, "opentracing-provider");
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAME, "hawkular-apm-1");
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE, "myproject");
        assertEquals("opentracing-provider", DeploymentMetaData.getServiceNameFromEnv());
        assertEquals("myproject.hawkular-apm-1", DeploymentMetaData.getBuildStampFromEnv());
        System.getProperties().remove(PropertyUtil.HAWKULAR_APM_SERVICE_NAME);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE);
    }

    @Test
    public void retrieveServiceName_noExplicitPropertyInsideOpenShiftMissingNamespace() {
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAME, "hawkular-apm-1");
        assertEquals("hawkular-apm", DeploymentMetaData.getServiceNameFromEnv());
        assertEquals("hawkular-apm-1", DeploymentMetaData.getBuildStampFromEnv());
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
    }

    @Test
    public void retrieveBuildStamp_overrideOpenShiftProperty() {
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAME, "hawkular-apm-1");
        System.setProperty(PropertyUtil.HAWKULAR_APM_BUILDSTAMP, "hawkular-apm-2");
        assertEquals("hawkular-apm-2", DeploymentMetaData.getBuildStampFromEnv());
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
        System.getProperties().remove(PropertyUtil.HAWKULAR_APM_BUILDSTAMP);
    }

    @Test
    public void testServiceNameIsAddedToRootNode() {
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAME, "vertx-opentracing-1");
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE, "myproject");
        DeploymentMetaData.reloadServiceName();

        APMTracerTest.TestTraceRecorder reporter = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(reporter, Sampler.ALWAYS_SAMPLE);

        Span span = tracer.buildSpan("NoReferences")
                .start();
        span.finish();

        assertEquals(1, reporter.getTraces().size());

        Trace trace = reporter.getTraces().get(0);

        // is this always true? will the trace always have only one node? why is it a collection then?
        Node rootNode = trace.getNodes().get(0);
        assertTrue(rootNode.hasProperty(Constants.PROP_SERVICE_NAME));

        Optional<Property> serviceNameProperty = rootNode.getProperties(Constants.PROP_SERVICE_NAME).stream().findFirst();
        assertTrue(serviceNameProperty.isPresent());
        assertEquals("myproject.vertx-opentracing", serviceNameProperty.get().getValue());

        Optional<Property> buildStampProperty = rootNode.getProperties(Constants.PROP_BUILD_STAMP).stream().findFirst();
        assertTrue(buildStampProperty.isPresent());
        assertEquals("myproject.vertx-opentracing-1", buildStampProperty.get().getValue());

        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE);

    }

    @Test
    public void testCustomServiceNameIsAddedToRootNode() {
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAME, "vertx-opentracing-1");
        System.setProperty(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE, "myproject");
        DeploymentMetaData.reloadServiceName();

        APMTracerTest.TestTraceRecorder reporter = new APMTracerTest.TestTraceRecorder();
        Tracer tracer = new APMTracer(reporter, Sampler.ALWAYS_SAMPLE, new DeploymentMetaData("mycustomapp", "mycustomapp-1"));

        Span span = tracer.buildSpan("NoReferences")
                .start();
        span.finish();

        assertEquals(1, reporter.getTraces().size());

        Trace trace = reporter.getTraces().get(0);

        // is this always true? will the trace always have only one node? why is it a collection then?
        Node rootNode = trace.getNodes().get(0);
        assertTrue(rootNode.hasProperty(Constants.PROP_SERVICE_NAME));

        Optional<Property> serviceNameProperty = rootNode.getProperties(Constants.PROP_SERVICE_NAME).stream().findFirst();
        assertTrue(serviceNameProperty.isPresent());
        assertEquals("mycustomapp", serviceNameProperty.get().getValue());

        Optional<Property> buildStampProperty = rootNode.getProperties(Constants.PROP_BUILD_STAMP).stream().findFirst();
        assertTrue(buildStampProperty.isPresent());
        assertEquals("mycustomapp-1", buildStampProperty.get().getValue());

        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAME);
        System.getProperties().remove(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE);

    }
}
