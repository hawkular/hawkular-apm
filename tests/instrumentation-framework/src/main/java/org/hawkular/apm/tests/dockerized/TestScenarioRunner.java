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
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.dockerized.exception.EnvironmentException;
import org.hawkular.apm.tests.dockerized.exception.TestFailException;
import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;
import org.hawkular.apm.tests.dockerized.model.TestCase;
import org.hawkular.apm.tests.dockerized.model.TestScenario;
import org.hawkular.apm.tests.server.ApmMockServer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.exceptions.DockerCertificateException;

/**
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
    private final ApmMockServer apmServer;
    private final ObjectMapper objectMapper;


    /**
     * @param apmVersion - version of the Hawkular APM (usually current version)
     * @param apmServerPort - port at which Hawkular APM server will be started
     * @throws DockerCertificateException
     */
    public TestScenarioRunner(String apmVersion, int apmServerPort) throws DockerCertificateException {
        this.apmVersion = apmVersion;
        this.apmServerPort = apmServerPort;

        Set<String> dockerInterfaceIpAddresses = InterfaceIpAddress.getIpAddresses("docker0");
        if (dockerInterfaceIpAddresses == null || dockerInterfaceIpAddresses.isEmpty()) {
            throw new EnvironmentException("Could not find any ip address of network interface docker0");
        }
        String hostIpAddress = dockerInterfaceIpAddresses.iterator().next();
        this.apmServer = new ApmMockServer();
        this.apmServer.setPort(apmServerPort);
        this.apmServer.setHost(hostIpAddress);
        this.apmServer.setShutdownTimer(60*60*1000);

        this.objectMapper = new ObjectMapper();
    }

    /**
     * Run test scenario
     *
     * @param testScenario
     * @return number of successful test cases
     */
    public int run(TestScenario testScenario) {

        System.out.println("Starting test scenario: " + testScenario);
        int successfulTestCases = 0;

        for (TestCase test: testScenario.getTests()) {
            if (test.isSkip()) {
                continue;
            }

            TestEnvironmentExecutor testEnvironmentExecutor = testEnvironmentExecutor(testScenario);

            try {
                runSingleTest(testScenario, test, testEnvironmentExecutor);
                successfulTestCases++;
            } catch (TestFailException ex) {
                System.out.println("Test case failed: " + ex.getTestCase());
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }

        return successfulTestCases;
    }

    private void runSingleTest(TestScenario testScenario, TestCase testCase,
                               TestEnvironmentExecutor testEnvironmentExecutor) throws TestFailException {

        System.out.println("Executing test: " + testCase);

        String environmentId = null;
        try {
            /**
             * Start APM server
             */
            apmServer.setTraces(new ArrayList<>());
            apmServer.run();

            /**
             * Run a container and wait
             */
            environmentId = testEnvironmentExecutor.run(testScenario.getEnvironment(), testCase.getScript());
            Thread.sleep(testScenario.getEnvironment().getInitWaitSeconds()*1000);

            /**
             * Execute test script and wait
             */
            DockerImageExecutor dockerImageExecutor = (DockerImageExecutor) testEnvironmentExecutor;
            dockerImageExecutor.exec(environmentId, testCase.getScript());
//            Integer returnValue = executeTestScript(testScenario, testCase);
//            if (returnValue == null || !returnValue.equals(0)) {
//                throw new TestFailException(testCase, "Script exit value != 0, -> " + returnValue);
//            }
            Thread.sleep(testCase.getAfterScriptWaitSeconds()*1000);

            /**
             * verify results
             */
            verifyResults(testCase);
        } catch (InterruptedException ex) {
            throw new TestFailException(testCase, ex);
        } finally {
            apmServer.shutdown();

            if (environmentId != null) {
                testEnvironmentExecutor.clean(environmentId);
            }
        }
    }

    private void verifyResults(TestCase testCase) throws TestFailException {

        List<Trace> traces = apmServer.getTraces();
        System.out.println("Captured traces:\n" + traces);

        String tracesJson;
        try {
            tracesJson = serialize(traces);
        } catch (IOException e) {
            throw new TestFailException(testCase, "Failed to serialize traces", e);
        }

        for (JsonPathVerify jsonPathVerify: testCase.getVerify().getJsonPath()) {
            if (!JsonPathVerifier.verify(tracesJson, jsonPathVerify)) {
                throw new TestFailException(testCase, "Failed to verify: " + jsonPathVerify.toString());
            }
        }
    }

    private TestEnvironmentExecutor testEnvironmentExecutor(TestScenario testScenario) {

        TestEnvironmentExecutor testEnvironmentExecutor;
        if (testScenario.getEnvironment().getImage() != null) {
            testEnvironmentExecutor = new DockerImageExecutor(apmVersion, apmServerPort,
                    testScenario.getScenarioDirectory());
        } else {
            testEnvironmentExecutor = new DockerComposeEnvironmentExecutor();
        }

        return testEnvironmentExecutor;
    }

    /**
     * Execute test script
     */
    private Integer executeTestScript(TestScenario testScenario, TestCase testCase) throws TestFailException {

        Process process = null;
        try {
            process = Runtime.getRuntime()
                    .exec("chmod +x " + testScenario.getScenarioDirectory() + "/" + testCase.getScript());
            if (process == null || process.waitFor() != 0) {
                return null;
            }

            process = Runtime.getRuntime().exec(testScenario.getScenarioDirectory() + "/" + testCase.getScript());
            if (process == null || process.waitFor() != 0) {
                return null;
            }

        } catch (InterruptedException | IOException ex) {
            ex.printStackTrace();
            throw new TestFailException(testCase, "Failed to execute test script", ex);
        }

        return process != null ? process.exitValue() : null;
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
