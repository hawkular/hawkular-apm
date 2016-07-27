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

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.tests.dockerized.exception.EnvironmentException;
import org.hawkular.apm.tests.dockerized.model.TestEnvironment;
import org.hawkular.apm.tests.dockerized.model.Type;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.HostConfig;

/**
 * @author Pavol Loffay
 */
public class DockerImageExecutor implements TestEnvironmentExecutor {
    private static final String APM_AGENT_DIRECTORY = "/opt/hawkular-apm-agent";
    private static final String TEST_SCRIPT_DIRECTORY = "/opt/hawkular-apm-test";

    private String apmVersion;
    private int apmServerPort;
    private String scenarioDirectory;

    /**
     * Docker config
     */
    private final DockerClient dockerClient;
    private final HostConfig hostConfig;


    public DockerImageExecutor(String apmVersion, int apmServerPort, String scenarioDirectory) {
        this.apmVersion = apmVersion;
        this.apmServerPort = apmServerPort;
        this.scenarioDirectory = scenarioDirectory;

        try {
            this.dockerClient = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException ex) {
            throw new EnvironmentException();
        }

        this.hostConfig = hostConfig();
    }

    @Override
    public String run(TestEnvironment testEnvironment) {
        ContainerConfig.Builder containerConfigBuilder = ContainerConfig.builder()
                .hostConfig(hostConfig)
                // when using --net=host hostname should be null
//                .hostname(null)
                .image(testEnvironment.getImage());

        if (testEnvironment.getType() == Type.APM) {
            containerConfigBuilder.env(instrumentationEnvVariables(APM_AGENT_DIRECTORY));
        }

        ContainerConfig containerConfig = containerConfigBuilder.build();

        String containerId = null;
        try {
            containerId = dockerClient.createContainer(containerConfig).id();
            dockerClient.startContainer(containerId);

        } catch (DockerException | InterruptedException ex) {
            throw new EnvironmentException("Could not create or start docker container.", ex);
        }

        return containerId;
    }

    /**
     * Method executes script in docker container
     *
     * @param id The container id
     * @param script script name, this script should be accessible in containers
     *      {@link DockerImageExecutor#TEST_SCRIPT_DIRECTORY}
     */
    public void execScript(String id, String script) {
        String execCreate = null;
        try {
            String sOrigAbsolutePath = TEST_SCRIPT_DIRECTORY + "/" + script;
            String sNewAbsolutePath = "/opt/hawkular-apm-test-local";

            /**
             * Due to permissions problem with mounted volume script is moved to some local directory in container
             *
             * 1. create new directory in container
             * 2. move there script
             * 3. chmod script
             * 4. execute script
             */
            String[] commands2 = new String[]{"bash", "-c",
                    "mkdir " + sNewAbsolutePath + " && " +
                    "mv " + sOrigAbsolutePath + " " + sNewAbsolutePath + " && " +
                    "chmod +x " + sNewAbsolutePath + "/" + script  + " && " +
                    sNewAbsolutePath + "/" + script};

            execCreate = dockerClient.execCreate(id, commands2,
                    DockerClient.ExecCreateParam.attachStdout(), DockerClient.ExecCreateParam.attachStderr());

            LogStream logStream = dockerClient.execStart(execCreate);
            System.out.println("\n\nTest script:\n" + logStream.readFully());
        } catch (DockerException | InterruptedException ex) {
            throw new EnvironmentException("Could not execute command", ex);
        }
    }

    @Override
    public void clean(String id) {
        try {
            dockerClient.killContainer(id);
            dockerClient.removeContainer(id);
        } catch (DockerException | InterruptedException ex) {
            throw new EnvironmentException("Could not remove container: " + id, ex);
        }
    }

    @Override
    public void close() {
        if (dockerClient != null) {
            dockerClient.close();
        }
    }

    private HostConfig hostConfig() {
        /**
         * Target directory is mounted to docker in order to access agent's jar
         */
        String hostOsMountDir = System.getProperties().getProperty("buildDirectory");

        List<String> dockerInterfaceIpAddresses = InterfaceIpV4Address.getIpAddresses("docker0");
        if (dockerInterfaceIpAddresses == null || dockerInterfaceIpAddresses.isEmpty()) {
            throw new EnvironmentException("Could not find any ip address of network interface docker0");
        }

        String hostIpAddress = dockerInterfaceIpAddresses.iterator().next();

        return HostConfig.builder()
//                .networkMode("host")
                .extraHosts("hawkular-apm:" + hostIpAddress)
                .appendBinds(
                    HostConfig.Bind
                        .from(hostOsMountDir)
                        .to(APM_AGENT_DIRECTORY)
                        .readOnly(true)
                        .build(),
                    HostConfig.Bind
                        .from(scenarioDirectory)
                        .to(TEST_SCRIPT_DIRECTORY)
                        .build())
                .build();
    }

    private List<String> instrumentationEnvVariables(String agentDirectory) {
        List<String> envVariables = new ArrayList<>();

        String agentName = "hawkular-apm-agent.jar";
        String agentLocation = agentDirectory + "/" + agentName;

        envVariables.add("APM_VERSION=" + apmVersion);
        envVariables.add("APM_AGENT=" + agentLocation);

        envVariables.add("HAWKULAR_APM_LOG_LEVEL=FINEST");
        envVariables.add("HAWKULAR_APM_CONFIG=" + agentDirectory + "/apmconfig");

        envVariables.add("HAWKULAR_APM_URI=http://hawkular-apm:" + apmServerPort + "/");
        envVariables.add("HAWKULAR_APM_USERNAME=jdoe");
        envVariables.add("HAWKULAR_APM_PASSWORD=password");

        envVariables.add("JAVA_OPTS=-javaagent:" + agentLocation + "=boot:" + agentLocation +
                " -Djboss.modules.system.pkgs=" +
                "org.jboss.byteman,org.hawkular.apm.instrumenter,org.hawkular.apm.client.api");

        return envVariables;
    }
}
