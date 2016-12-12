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

package org.hawkular.apm.tests.dockerized.environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hawkular.apm.tests.dockerized.exception.EnvironmentException;
import org.hawkular.apm.tests.dockerized.model.TestEnvironment;

/**
 * @author Pavol Loffay
 */
public class DockerComposeExecutor extends AbstractDockerBasedEnvironment {
    private static final Logger log = Logger.getLogger(DockerComposeExecutor.class.getName());


    private DockerComposeExecutor(String scenarioDirectory, String apmBindAddress) {
        super(scenarioDirectory, apmBindAddress);
        if (apmBindAddress == null) {
            throw new NullPointerException("Bind address for APM should be specified.");
        }
    }

    /**
     * @param scenarioDirectory Absolute path of test scenario directory
     * @param apmBindAddress Address of default gateway (host OS where is running APM server). This network
     *                       is also used as default in docker-compose
     */
    public static DockerComposeExecutor getInstance(String scenarioDirectory, String apmBindAddress) {
        return new DockerComposeExecutor(scenarioDirectory, apmBindAddress);
    }

    @Override
    public String run(TestEnvironment testEnvironment) {

        if (testEnvironment.isPull()) {
            runShellCommand(new String[] {
                    "docker-compose", "-f", scenarioDirectory + File.separator + testEnvironment.getDockerCompose(),
                    "pull",
            });
        }

        /**
         * Note that if image is not in local cache it will be download from the hub
         */
       runShellCommand(new String[] {
               "docker-compose", "-f", scenarioDirectory + File.separator + testEnvironment.getDockerCompose(), "up",
               "-d"
        });

        return testEnvironment.getDockerCompose();
    }

    @Override
    public void stopAndRemove(String dockerCompose) {

        String[] command;
        try {
            command = new String[]{
                    "docker-compose", "-f", scenarioDirectory + File.separator + dockerCompose, "down", "--rmi", "local"
            };
            runShellCommand(command);
        } catch (EnvironmentException ex) {
            log.severe(String.format("docker-compose down failed %s", ex.getMessage()));
            ex.printStackTrace();
        }
    }

    /**
     * @param id Id of the environment, Concretely docker-compose file.
     * @param serviceName Service name in running environment. Service defined in docker-compose.
     * @param script Script to execute
     */
    @Override
    public void execScript(String id, String serviceName, String script) {

        String[] command = new String[] {
                "docker-compose", "-f", scenarioDirectory + File.separator + id, "exec", serviceName, "bash", "-c",
                scriptExecCommand(script),
        };

        runShellCommand(command);
    }

    private void runShellCommand(String[] commands) {

        log.info(String.format("Executing command on host OS: `%s`", Arrays.toString(commands)));

        try {
            Process process = Runtime.getRuntime().exec(commands);

            /**
             * Output
             */
            InputStream stdin = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            log.info("<OUTPUT>");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            log.info("</OUTPUT>");

            int exitVal = process.waitFor();
            log.info(String.format("Process exit value: %d", exitVal));

            if (process == null || exitVal != 0) {
                log.severe(String.format("`%s` did not return 0", Arrays.toString(commands)));
                log.severe("-------- stderr ");
                log.severe(new BufferedReader(new InputStreamReader(process.getErrorStream())).lines().collect(Collectors.joining("\n")));
                log.severe("-------- /stderr ");
                throw new EnvironmentException(Arrays.toString(commands) + " did not return 0, actual = " +
                        (process != null ? process.exitValue(): ""));
            }

        } catch (IOException | InterruptedException ex) {
            System.out.println(String.format("Could not run: %s", Arrays.toString(commands)));
            throw new EnvironmentException("Could not run: " + Arrays.toString(commands), ex);
        }

        log.info(String.format("Command `%s`, successfully executed", Arrays.toString(commands)));
    }
}
