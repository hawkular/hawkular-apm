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

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;
import org.hawkular.apm.tests.dockerized.model.TestCase;
import org.hawkular.apm.tests.dockerized.model.TestScenario;
import org.hawkular.apm.tests.dockerized.model.Type;
import org.hawkular.apm.tests.server.TestTraceServer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;

/**
 * @author Pavol Loffay
 */
public class TestScenarioRunner {
    private static final String DOCKER_MONT_DIR = "/opt/hawkular-apm-agent";

    /**
     * Version of APM, needed for ENV variable inside docker image
     */
    private final String apmVersion;
    /**
     * Port at which will be listening APM server
     */
    private final int apmServerPort;

    /**
     * Docker config
     */
    private final DockerClient dockerClient;
    private final HostConfig hostConfig;

    /**
     * Mocked Hawkular APM server to collect information captured by agents.
     */
    private final TestTraceServer apmServer;

    private final ObjectMapper objectMapper;


    /**
     * @param apmVersion - version of the Hawkular APM (usually current version)
     * @param apmServerPort - port at which Hawkular APM server will be started
     * @throws DockerCertificateException
     */
    public TestScenarioRunner(String apmVersion, int apmServerPort) throws DockerCertificateException {
        this.apmVersion = apmVersion;
        this.apmServerPort = apmServerPort;

        this.dockerClient = DefaultDockerClient.fromEnv().build();
        this.hostConfig = hostConfig();

        this.apmServer = new TestTraceServer();
        this.apmServer.setPort(apmServerPort);
        this.apmServer.setShutdownTimer(60*60*1000);

        this.objectMapper = new ObjectMapper();
    }

    private HostConfig hostConfig() {
        /**
         * Target directory is mounted to docker in order to access agent's jar
         */
        String hostOsMountDir = System.getProperties().getProperty("buildDirectory");

        return HostConfig.builder()
                .networkMode("host")
                .appendBinds(HostConfig.Bind
                        .from(hostOsMountDir)
                        .to(DOCKER_MONT_DIR)
                        .readOnly(true)
                        .build())
                .build();
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

            if (testScenario.getEnvironment().getImage() != null) {
                try {
                    runSingleImageTest(testScenario, test);
                    successfulTestCases++;
                } catch (TestFailException ex) {
                    System.out.println("Test case failed: " + ex.getTestCase());
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                // TODO docker-compose
            }
        }

        return successfulTestCases;
    }

    public void close() {
        dockerClient.close();
    }

    private void runSingleImageTest(TestScenario testScenario, TestCase testCase) throws TestFailException {

        System.out.println("Executing test: " + testCase);

        ContainerConfig.Builder containerConfigBuilder = ContainerConfig.builder()
                .hostConfig(hostConfig)
                // when using --net=host hostname should be null
                .hostname(null)
                .image(testScenario.getEnvironment().getImage())
                .env(instrumentationEnvVariables(DOCKER_MONT_DIR));

        if (testScenario.getEnvironment().getType() == Type.APM) {
            containerConfigBuilder.env(instrumentationEnvVariables(DOCKER_MONT_DIR));
        }

        ContainerConfig containerConfig = containerConfigBuilder.build();

        String containerId = null;
        try {
            /**
             * Start APM server
             */
            apmServer.setTraces(new ArrayList<>());
            apmServer.run();

            /**
             * Start container and wait
             */
            containerId = dockerClient.createContainer(containerConfig).id();
            dockerClient.startContainer(containerId);
            Thread.sleep(testScenario.getEnvironment().getInitWaitSeconds() * 1000);

            /**
             * Execute test script and wait
             */
            Integer returnValue = executeTestScript(testScenario, testCase);
            if (returnValue == null || !returnValue.equals(0)) {
                throw new TestFailException(testCase, "Script exit value != 0, -> " + returnValue);
            }
            Thread.sleep(testCase.getAfterScriptWaitSeconds() * 1000);

            /**
             * verify results
             */
            verifyResults(testCase);
        } catch (InterruptedException | DockerException ex) {
            throw new TestFailException(testCase, ex);
        } finally {
            apmServer.shutdown();

            if (containerId != null) {
                try {
                    dockerClient.killContainer(containerId);
                    dockerClient.removeContainer(containerId);
                } catch (DockerException | InterruptedException ex) {
                    //ignore
                }
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

    private List<String> instrumentationEnvVariables(String agentDirectory) {
        List<String> envVariables = new ArrayList<>();

        String agentName = "hawkular-apm-agent.jar";
        String agentLocation = agentDirectory + "/" + agentName;

        envVariables.add("APM_VERSION=" + apmVersion);
        envVariables.add("APM_AGENT=" + agentLocation);

        envVariables.add("HAWKULAR_APM_LOG_LEVEL=FINEST");
        envVariables.add("HAWKULAR_APM_CONFIG=" + agentDirectory + "/apmconfig");

        envVariables.add("HAWKULAR_APM_URI=http://localhost:" + apmServerPort + "/");
        envVariables.add("HAWKULAR_APM_USERNAME=jdoe");
        envVariables.add("HAWKULAR_APM_PASSWORD=password");

        envVariables.add("JAVA_OPTS=-javaagent:" + agentLocation + "=boot:" + agentLocation +
                " -Djboss.modules.system.pkgs=" +
                "org.jboss.byteman,org.hawkular.apm.instrumenter,org.hawkular.apm.client.api");

        return envVariables;
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
