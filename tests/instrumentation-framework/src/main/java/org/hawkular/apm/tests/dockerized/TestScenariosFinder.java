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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.tests.dockerized.model.TestScenario;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Finds test scenarios defined in some directory.
 *
 * Test scenarios are defined in separate directories with test prefix.
 * Inside test scenario directory there is test scenario definition file named test.json and other test related
 * files (eg. scripts)
 *
 * @author Pavol Loffay
 */
public class TestScenariosFinder {

    private static final String SCENARIO_DIRECTORY_NAME_PREFIX = "test";
    private static final String SCENARIO_NAME = "test.json";

    private final String directory;
    private ObjectMapper objectMapper;

    /**
     * @param directoryWithScenarios directory with test scenarios
     */
    public TestScenariosFinder(String directoryWithScenarios) {
        this.directory = directoryWithScenarios;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Returns defined test scenarios.
     * @return Defined test scenarios.
     * @throws IOException
     */
    public List<TestScenario> getScenarios() throws IOException {

        File folder = new File(directory);
        File[] scenarioDirectories = folder.listFiles((dir, name) -> name.startsWith(SCENARIO_DIRECTORY_NAME_PREFIX));

        List<TestScenario> testScenarios = new ArrayList<>();

        for (File directory: scenarioDirectories) {
            if (directory.isDirectory()) {
                File scenarioFile = directory.listFiles((dir, name) -> name.equals(SCENARIO_NAME))[0];

                String scenarioJson = readFile(scenarioFile);
                TestScenario testScenario = deserialize(scenarioJson, TestScenario.class);
                testScenario.setScenarioDirectory(directory.getAbsolutePath());

                testScenarios.add(testScenario);
            }
        }

        return testScenarios;
    }

    public <T> T deserialize(String json, Class<T> type) throws IOException {
        JsonParser parser = objectMapper.getFactory().createParser(json);
        return parser.readValueAs(type);
    }

    public String readFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(file.toURI()));
        return new String(bytes);
    }
}
