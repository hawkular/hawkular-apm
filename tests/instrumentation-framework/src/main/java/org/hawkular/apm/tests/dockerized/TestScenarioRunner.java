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

package org.hawkular.apm.tests.dockerized;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.hawkular.apm.tests.common.ApmMockServer;
import org.hawkular.apm.tests.dockerized.environment.DockerComposeExecutor;
import org.hawkular.apm.tests.dockerized.environment.DockerImageExecutor;
import org.hawkular.apm.tests.dockerized.environment.TestEnvironmentExecutor;
import org.hawkular.apm.tests.dockerized.exception.TestFailException;
import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;
import org.hawkular.apm.tests.dockerized.model.TestCase;
import org.hawkular.apm.tests.dockerized.model.TestScenario;
import org.hawkular.apm.tests.dockerized.model.Type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class for running test scenario.
 *
 * @author Pavol Loffay
 */
public class TestScenarioRunner {
    private static final Logger log = Logger.getLogger(TestScenarioRunner.class.getName());

    private final ObjectMapper objectMapper;
    private int apmServerPort;


    /**
     * @param apmServerPort - port at which Hawkular APM server will be started
     */
    public TestScenarioRunner(int apmServerPort) {
        this.apmServerPort = apmServerPort;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Run a test scenario
     *
     * Life cycle of the test case:
     * 1. Create network (must for docker-compose, optional for single docker image)
     * 2. Start apm server
     * 3. Start environment (which includes start of a test app)
     * 4. Wait for app start
     * 5. Run action in the app which is executed inside docker container.
     * 6. Wait some time that the action is processed and some data propagated to apm server
     * 7. Verify results.
     * 8. Shut down test environment and apm server.
     *
     * @param testScenario
     * @return number of successful test cases
     */
    public int run(TestScenario testScenario) {
        if (testScenario.getEnvironment().getDockerCompose() != null &&
                testScenario.getEnvironment().getImage() != null) {
            throw new IllegalArgumentException("Ambiguous environment: defined docker image and docker-compose" +
                    ", but we expect only one of them to be defined!");
        }

        log.info(String.format("========================= Starting test scenario: %s", testScenario));
        int successfulTestCases = 0;

        for (TestCase test: testScenario.getTests()) {
            if (test.isSkip()) {
                continue;
            }

            TestEnvironmentExecutor testEnvironmentExecutor = createTestEnvironmentExecutor(testScenario);

            try {
                runTestCase(testScenario, test, testEnvironmentExecutor);
                successfulTestCases++;
            } catch (TestFailException ex) {
                log.severe(String.format("Test case failed: %s\n%s", ex.toString(), ex.getMessage()));
                ex.printStackTrace();
            } finally {
                testEnvironmentExecutor.close();
            }
        }

        log.info(String.format("========================= Closing test scenario : %s", testScenario));
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

        log.info(String.format("Executing test case: %s", testCase));

        List<String> environmentId = null;
        ApmMockServer apmServer = new ApmMockServer();
        apmServer.setHost("0.0.0.0");
        apmServer.setPort(apmServerPort);
        // disable shut down timer
        apmServer.setShutdownTimer(60*60*1000);
        try {

            /**
             * Create network if necessary
             */
            if (testScenario.getEnvironment().getApmAddress() != null) {
                testEnvironmentExecutor.createNetwork();
            }

            /**
             * Start APM server
             */
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
            Collection<JsonPathVerify> jsonPathVerifies = verifyResults(testScenario, testCase, apmServer);
            if (!jsonPathVerifies.isEmpty()) {
                throw new TestFailException(testCase, jsonPathVerifies);
            }
        } catch (InterruptedException ex) {
            log.severe("Interruption exception");
            log.severe(ex.toString());
            throw new TestFailException(testCase, ex);
        } finally {
            if (environmentId != null) {
                testEnvironmentExecutor.stopAndRemove(environmentId);
            }

            if (apmServer != null) {
                apmServer.shutdown();
            }
        }
    }

    private List<JsonPathVerify> verifyResults(TestScenario testScenario, TestCase testCase, ApmMockServer apmServer) {

        Collection<?> objects = getCapturedData(testScenario.getEnvironment().getType(), apmServer);

        String json;
        try {
            json = serialize(objects);
        } catch (IOException ex) {
            log.severe(String.format("Failed to serialize traces: %s", objects));
            throw new RuntimeException("Failed to serialize traces = " + objects, ex);
        }

        log.info(String.format("Captured objects:\n%s", json));

        List<JsonPathVerify> failedJsonPathVerify = new ArrayList<>();

        for (JsonPathVerify jsonPathVerify: testCase.getVerify().getJsonPath()) {
            if (!JsonPathVerifier.verify(json, jsonPathVerify)) {
                failedJsonPathVerify.add(jsonPathVerify);
            }
        }

        return failedJsonPathVerify;
    }

    private Collection<?> getCapturedData(Type type, ApmMockServer apmMockServer) {
        Collection<?> objects = null;

        switch (type) {
            case APM:
            case APMAGENT:
            case APMOTAGENT:
                objects = apmMockServer.getTraces();
                break;
            case ZIPKIN:
                objects = apmMockServer.getSpans();
                break;
        }

        return objects;
    }

    private TestEnvironmentExecutor createTestEnvironmentExecutor(TestScenario testScenario) {

        TestEnvironmentExecutor testEnvironmentExecutor;
        if (testScenario.getEnvironment().getImage() != null) {
            testEnvironmentExecutor = DockerImageExecutor.getInstance(testScenario.getScenarioDirectory(),
                    testScenario.getEnvironment().getApmAddress());
        } else {
            testEnvironmentExecutor = DockerComposeExecutor.getInstance(testScenario.getScenarioDirectory(),
                    testScenario.getEnvironment().getApmAddress());
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
