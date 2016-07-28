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
import java.util.logging.Logger;

import org.hawkular.apm.tests.dockerized.exception.EnvironmentException;
import org.hawkular.apm.tests.dockerized.model.TestEnvironment;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DockerClientBuilder;

/**
 * @author Pavol Loffay
 */
public class DockerComposeExecutor implements TestEnvironmentExecutor {
    private static final Logger log = Logger.getLogger(DockerComposeExecutor.class.getName());

    private final String apmBindAddress;

    private DockerClient dockerClient;
    private Network network;

    /**
     * @param apmBindAddress Address of default gateway (host OS where is running APM server). This network
     *                       is also used as default in docker-compose
     */
    public DockerComposeExecutor(String apmBindAddress) {
        this.apmBindAddress = apmBindAddress;
        this.dockerClient = DockerClientBuilder.getInstance().build();
    }

    /**
     * Create network which is used as default in docker-compose.yml
     * This should be run before {@link DockerComposeExecutor#run(TestEnvironment)}
     */
    public void createNetwork() {
        String apmNetwork = apmBindAddress.substring(0, apmBindAddress.lastIndexOf(".")) + ".0/24";
        Network.Ipam ipam = new Network.Ipam()
                .withConfig(new Network.Ipam.Config()
                    .withSubnet(apmNetwork)
                    .withGateway(apmBindAddress));

        CreateNetworkResponse createNetworkResponse = dockerClient.createNetworkCmd()
                .withName(Constants.HOST_ADDED_TO_ETC_HOSTS)
                .withIpam(ipam)
                .exec();

        try {
            network = dockerClient.inspectNetworkCmd().withNetworkId(createNetworkResponse.getId()).exec();
        } catch (DockerException ex) {
            log.severe(String.format("Could not create network: %s", createNetworkResponse));
            throw new EnvironmentException("Could not create network: " + createNetworkResponse, ex);
        }
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
            log.severe(String.format("docker-compose down failed %s", ex.getMessage()));
            ex.printStackTrace();
        }

        if (network != null) {
            try {
                dockerClient.removeNetworkCmd(network.getId()).exec();
            } catch (DockerException ex) {
                log.severe(String.format("Could not remove network: %s", network));
                throw new EnvironmentException("Could not remove network: " + network, ex);
            }
        }
    }

    /**
     * @param id Id of the environment, Concretely path to docker-compose file.
     * @param serviceName Service name in running environment. Service defined in docker-compose.
     * @param script Script to execute
     */
    @Override
    public void execScript(String id, String serviceName, String script) {

        String[] command = new String[] {
                "docker-compose", "-f", id, "exec", serviceName, "bash", "-c",
                scriptExecCommand(script),
        };

        runShellCommand(command);
    }

    @Override
    public void close() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException ex) {
                log.severe("Could not close docker client");
                throw new EnvironmentException("Could not close docker client", ex);
            }
        }
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
