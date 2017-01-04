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

package org.hawkular.apm.tests.dockerized.model;

import java.util.List;

/**
 * Top level class representing test scenario.
 * Test scenario defines environment (docker image, docker compose) and set of tests.
 *
 * @author Pavol Loffay
 */
public class TestScenario {

    private String name;
    private TestEnvironment environment;
    private List<TestCase> tests;

    private String scenarioDirectory;


    public int enabledTests() {
        int enabledTests = 0;
        for (TestCase testCase: tests) {
            if (!testCase.isSkip()) {
                enabledTests++;
            }
        }

        return enabledTests;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TestEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(TestEnvironment environment) {
        this.environment = environment;
    }

    public List<TestCase> getTests() {
        return tests;
    }

    public void setTests(List<TestCase> tests) {
        this.tests = tests;
    }

    public String getScenarioDirectory() {
        return scenarioDirectory;
    }

    public void setScenarioDirectory(String scenarioDirectory) {
        this.scenarioDirectory = scenarioDirectory;
    }

    @Override
    public String toString() {
        return "TestScenario{" +
                "name='" + name + '\'' +
                ", environment=" + environment +
                ", tests=" + tests +
                ", scenarioDirectory='" + scenarioDirectory + '\'' +
                '}';
    }
}
