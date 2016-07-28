/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

package org.hawkular.apm.tests.dockerized;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.hawkular.apm.tests.dockerized.environment.DockerComposeExecutor;
import org.hawkular.apm.tests.dockerized.environment.DockerImageExecutor;
import org.hawkular.apm.tests.dockerized.environment.TestEnvironmentExecutor;
import org.hawkular.apm.tests.dockerized.exception.TestFailException;
import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;
import org.hawkular.apm.tests.dockerized.model.TestCase;
import org.hawkular.apm.tests.dockerized.model.TestScenario;
import org.hawkular.apm.tests.dockerized.model.Type;
import org.hawkular.apm.tests.server.ApmMockServer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.exceptions.DockerCertificateException;

/**
 * Class for running test scenario.
 *
 * @author Pavol Loffay
 */
public class TestScenarioRunner {

    /**
     * Version of APM, needed for ENV variable inside docker image
     */
    private final String apmVersion;
    /**
     * Port at which will be listening APM server
     */
    private final int apmServerPort;

    /**
     * Mocked Hawkular APM server to collect information captured by agents.
     */
//    private final ApmMockServer apmServer;
    private final ObjectMapper objectMapper;

    private final ApmMockServer apmServer;

    /**
     * @param apmVersion - version of the Hawkular APM (usually current version)
     * @param apmServerPort - port at which Hawkular APM server will be started
     * @throws DockerCertificateException
     */
    public TestScenarioRunner(String apmVersion, int apmServerPort) throws DockerCertificateException {
        this.apmVersion = apmVersion;
        this.apmServerPort = apmServerPort;

        this.objectMapper = new ObjectMapper();
        apmServer = new ApmMockServer();
        apmServer.setPort(apmServerPort);
        apmServer.setShutdownTimer(60*60*1000);
    }

    /**
     * Run a test scenario
     *
     * @param testScenario
     * @return number of successful test cases
     */
    public int run(TestScenario testScenario) {
        if (testScenario.getEnvironment().getDockerCompose() != null &&
                testScenario.getEnvironment().getImage() != null) {
            throw new IllegalArgumentException("Ambiguous environment: defined docker image and docker-compose");
        }

        System.out.println("Starting test scenario: " + testScenario);
        int successfulTestCases = 0;

        for (TestCase test: testScenario.getTests()) {
            if (test.isSkip()) {
                continue;
            }

            TestEnvironmentExecutor testEnvironmentExecutor = testEnvironmentExecutor(testScenario);

            try {
                runTestCase(testScenario, test, testEnvironmentExecutor);
                successfulTestCases++;
            } catch (TestFailException ex) {
                System.out.println("Test case failed: " + ex.toString());
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

            testEnvironmentExecutor.close();
        }

        return successfulTestCases;
    }

    /**
     * Run single test case
     *
     * @param testScenario Test scenario of the test case
     * @param testCase Test case to run
     * @param testEnvironmentExecutor Initialized environment
     * @throws TestFailException
     */
    private void runTestCase(TestScenario testScenario, TestCase testCase,
                             TestEnvironmentExecutor testEnvironmentExecutor) throws TestFailException {

        System.out.println("Executing test case: " + testCase);

        String environmentId = null;
        try {
            if (testEnvironmentExecutor instanceof DockerComposeExecutor) {
                ((DockerComposeExecutor) testEnvironmentExecutor).createNetwork();
            }

            /**
             * Start APM server
             */
            apmServer.setHost("0.0.0.0");
            apmServer.setTraces(new ArrayList<>());
            apmServer.run();

            /**
             * Run a container and wait
             */
            environmentId = testEnvironmentExecutor.run(testScenario.getEnvironment());
            Thread.sleep(testScenario.getEnvironment().getInitWaitSeconds()*1000);

            /**
             * Execute test script and wait
             */
            testEnvironmentExecutor.execScript(environmentId, testCase.getScriptServiceName(), testCase.getAction());
            Thread.sleep(testCase.getAfterActionWaitSeconds()*1000);

            /**
             * verify results
             */
            verifyResults(testScenario, testCase, apmServer);
        } catch (InterruptedException ex) {
            throw new TestFailException(testCase, ex);
        } finally {
            if (apmServer != null) {
                apmServer.shutdown();
            }

            if (environmentId != null) {
                testEnvironmentExecutor.clean(environmentId);
            }
        }
    }

    private void verifyResults(TestScenario testScenario, TestCase testCase, ApmMockServer apmServer) throws
            TestFailException {

        Collection<?> objects = getCapturedData(testScenario.getEnvironment().getType(), apmServer);

        System.out.println("Captured objects:\n" + objects);

        String json;
        try {
            json = serialize(objects);
        } catch (IOException e) {
            throw new TestFailException(testCase, "Failed to serialize traces", e);
        }

        for (JsonPathVerify jsonPathVerify: testCase.getVerify().getJsonPath()) {
            if (!JsonPathVerifier.verify(json, jsonPathVerify)) {
                throw new TestFailException(testCase, jsonPathVerify);
            }
        }
    }

    private Collection<?> getCapturedData(Type type, ApmMockServer apmMockServer) {
        Collection<?> objects = null;

        switch (type) {
            case APM:
                objects = apmMockServer.getTraces();
                break;
            case ZIPKIN:
                objects = apmMockServer.getSpans();
                break;
        }

        return objects;
    }

    private TestEnvironmentExecutor testEnvironmentExecutor(TestScenario testScenario) {

        TestEnvironmentExecutor testEnvironmentExecutor;
        if (testScenario.getEnvironment().getImage() != null) {
            testEnvironmentExecutor = new DockerImageExecutor(apmVersion, apmServerPort,
                    testScenario.getScenarioDirectory());
        } else {
            testEnvironmentExecutor = new DockerComposeExecutor(testScenario.getEnvironment().getApmAddress());
        }

        return testEnvironmentExecutor;
    }

    private String serialize(Object object) throws IOException {
        StringWriter out = new StringWriter();

        JsonGenerator gen = objectMapper.getFactory().createGenerator(out);
        gen.writeObject(object);

        gen.close();
        out.close();

        return out.toString();
    }
}
