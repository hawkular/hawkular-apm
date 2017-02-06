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

package org.hawkular.apm.tests.dockerized.environment;

import java.util.List;
import java.util.UUID;

import org.hawkular.apm.tests.dockerized.model.TestEnvironment;

/**
 * @author Pavol Loffay
 */
public interface TestEnvironmentExecutor {

    /**
     * Run test environment
     * @param testEnvironment test environment
     * @return
     */
    List<String> run(TestEnvironment testEnvironment);

    /**
     * Stop and remove environment
     *
     * @param ids environment ids (containers, docker-compose.yml files)
     */
    void stopAndRemove(List<String> ids);

    /**
     * Frees all resources for accessing/creating environment
     */
    void close();

    /**
     * Execute script in running environment
     *
     * @param id Id of the environment.
     * @param serviceName Service name in running environment.
     * @param script Script to execute
     */
    void execScript(List<String> id, String serviceName, String script);

    /**
     * Creates network for the environment.
     */
    void createNetwork();

    /**
     * Prepares command which will be executed inside docker container. This should be used for executing
     * action script inside docker container. Due to permissions problems with mounted volumes script is
     * moved to another directory.
     * @param script The script name.
     * @return command.
     */
    default String scriptExecCommand(String script) {
        String sNewAbsolutePath = "/opt/hawkular-apm-test-local" + UUID.randomUUID();

        return  "mkdir " + sNewAbsolutePath + " && " +
                "cp " + Constants.HAWKULAR_APM_TEST_DIRECTORY + "/" + script + " " + sNewAbsolutePath + " && " +
                "chmod +x " + sNewAbsolutePath + "/" + script  + " && " +
                sNewAbsolutePath + "/" + script;
    }

    final class Constants {

        protected static final String HOST_ADDED_TO_ETC_HOSTS = "hawkular-apm";

        /**
         * Directory inside environment with agent jar
         */
        public static final String HAWKULAR_APM_AGENT_DIRECTORY;
        /**
         * Directory inside test environment where are located test scenario files (e.g. action script)
         * This directory is mounted to docker container, therefore there could be permission issues in host OS.
         */
        public static final String HAWKULAR_APM_TEST_DIRECTORY;

        static {
            String property = System.getProperties().getProperty("hawkular-apm.test.environment.agent.dir");
            HAWKULAR_APM_AGENT_DIRECTORY = property != null ? property : "/opt/hawkular-apm-agent";

            property = System.getProperties().getProperty("hawkular-apm.test.environment.test.dir");
            HAWKULAR_APM_TEST_DIRECTORY = property != null ? property : "/opt/hawkular-apm-test";
        }
    }
}
