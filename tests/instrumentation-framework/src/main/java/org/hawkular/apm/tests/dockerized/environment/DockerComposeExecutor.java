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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.hawkular.apm.tests.dockerized.exception.EnvironmentException;
import org.hawkular.apm.tests.dockerized.model.TestEnvironment;

/**
 * @author Pavol Loffay
 */
public class DockerComposeExecutor implements TestEnvironmentExecutor {
    private static final String HAWKULAR_APM_NETWORK = "hawkular-apm";

    private final String apmBindAddress;


    /**
     * @param apmBindAddress Address of default gateway (host OS where is running APM server). This network
     *                       is also used as default in docker-compose
     */
    public DockerComposeExecutor(String apmBindAddress) {
        this.apmBindAddress = apmBindAddress;
    }

    /**
     * Create network which is used as default in docker-compose.yml
     * This should be run before {@link DockerComposeExecutor#run(TestEnvironment)}
     */
    public void createNetwork() {
        String[] command = new String[] {
                "docker", "network", "create", "--driver=bridge", "--subnet=" + apmBindAddress + "/24",
                "--gateway=" + apmBindAddress, HAWKULAR_APM_NETWORK,
        };

        runShellCommand(command);
    }

    @Override
    public String run(TestEnvironment testEnvironment) {
        String[] command = new String[] {
            "docker-compose", "-f", testEnvironment.getDockerCompose(), "up", "-d"
        };

        runShellCommand(command);

        return testEnvironment.getDockerCompose();
    }

    @Override
    public void clean(String dockerComposeDirectory) {

        String[] command;
        try {
            command = new String[]{
                    "docker-compose", "-f", dockerComposeDirectory, "down"
            };
            runShellCommand(command);
        } catch (EnvironmentException ex) {
            System.out.println(ex);
        }

        command = new String[] {
            "docker", "network", "rm", HAWKULAR_APM_NETWORK
        };
        runShellCommand(command);
    }

    /**
     *
     * @param id Id of the environment, Concretely path to docker-compose file.
     * @param serviceName Service name in running environment. Service defined in docker-compose.
     * @param script Script to execute
     */
    @Override
    public void execScript(String id, String serviceName, String script) {

        String scriptDockerLocalDir = "/opt/hawkular-apm-test-local";

        String[] command = new String[] {
                "docker-compose", "-f", id, "exec", serviceName, "bash", "-c",
                "mkdir " + scriptDockerLocalDir + " && " +
                "cp /opt/hawkular-apm-test/" + script + " " + scriptDockerLocalDir + " && " +
                "chmod +x " + scriptDockerLocalDir + "/" + script + " && " +
                scriptDockerLocalDir + "/" + script
        };

        runShellCommand(command);
    }

    @Override
    public void close() {
    }

    private void runShellCommand(String[] commands) {

        System.out.println("Executing command on host OS: `" + Arrays.toString(commands) + "`");

        try {
            Process process = Runtime.getRuntime().exec(commands);

            /**
             * Output
             */
            InputStream stdin = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            System.out.println("<OUTPUT>");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("</OUTPUT>");

            int exitVal = process.waitFor();
            System.out.println("Process exit value: " + exitVal);

            if (process == null || exitVal != 0) {
                throw new EnvironmentException(Arrays.toString(commands) + " did not return 0, actual = " +
                        (process != null ? process.exitValue(): ""));
            }

        } catch (IOException | InterruptedException ex) {
            throw new EnvironmentException("Could not run: " + Arrays.toString(commands), ex);
        }

        System.out.println("Command `" + Arrays.toString(commands) + "`, successfully executed");
    }
}
