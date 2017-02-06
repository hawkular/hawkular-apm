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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.hawkular.apm.tests.dockerized.InterfaceIpV4Address;
import org.hawkular.apm.tests.dockerized.exception.EnvironmentException;
import org.hawkular.apm.tests.dockerized.model.TestEnvironment;
import org.hawkular.apm.tests.dockerized.model.Type;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.SELContext;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * @author Pavol Loffay
 */
public class DockerImageExecutor extends AbstractDockerBasedEnvironment {
    private static final Logger log = Logger.getLogger(DockerImageExecutor.class.getName());

    private boolean userDefinedNetwork;


    private DockerImageExecutor(String scenarioDirectory, String apmBindAddress, Boolean userDefinedNetwork) {
        super(scenarioDirectory, apmBindAddress);
        this.userDefinedNetwork = userDefinedNetwork;
    }

    /**
     * @param scenarioDirectory Absolute path of test scenario directory
     * @param apmBindAddress Address of default gateway, if null default docker network will be used (docker0 interface)
     */
    public static DockerImageExecutor getInstance(String scenarioDirectory, String apmBindAddress) {
        boolean userDefinedAddress = apmBindAddress != null;
        if (apmBindAddress == null) {
            List<InetAddress> dockerInterfaceIpAddresses = InterfaceIpV4Address.getIpAddresses("docker0");
            if (dockerInterfaceIpAddresses == null || dockerInterfaceIpAddresses.isEmpty()) {
                throw new EnvironmentException("Could not find any ip address of network interface docker0");
            }
            apmBindAddress = dockerInterfaceIpAddresses.iterator().next().getHostAddress();
        }

        return new DockerImageExecutor(scenarioDirectory, apmBindAddress, userDefinedAddress);
    }

    @Override
    public List<String> run(TestEnvironment testEnvironment) {
        String hostOsMountDir = System.getProperties().getProperty("buildDirectory");


        CreateContainerCmd containerBuilder = dockerClient.createContainerCmd(testEnvironment.getImage())
                .withBinds(new Bind(hostOsMountDir, new Volume(Constants.HAWKULAR_APM_AGENT_DIRECTORY),
                                AccessMode.ro, SELContext.shared),
                    new Bind(scenarioDirectory, new Volume(Constants.HAWKULAR_APM_TEST_DIRECTORY),
                            AccessMode.ro, SELContext.shared))
                .withExtraHosts(Constants.HOST_ADDED_TO_ETC_HOSTS + ":" + apmBindAddress);

        if (userDefinedNetwork) {
            if (network == null) {
                throw new IllegalStateException("Create network before running environment");
            }
            containerBuilder.withNetworkMode(network.getName());
        }

        containerBuilder.withEnv(apmEnvVariables(testEnvironment.getType()));

        if (testEnvironment.isPull()) {
            log.info("Pulling image...");
            dockerClient.pullImageCmd(testEnvironment.getImage()).exec(new PullImageResultCallback()).awaitSuccess();
        }

        CreateContainerResponse containerResponse = containerBuilder.exec();
        log.info(String.format("Starting docker container: %s", containerResponse));

        try {
            dockerClient.startContainerCmd(containerResponse.getId()).exec();
        } catch (DockerException ex) {
            log.severe(String.format("Could not create or start docker container: %s", containerResponse));
            throw new EnvironmentException("Could not create or start docker container.", ex);
        }

        return Arrays.asList(containerResponse.getId());
    }

    /**
     * @param id The container id, will be used only the first one
     * @param serviceName Service name in running environment. Can be null.
     * @param script Script to execute
     */
    @Override
    public void execScript(List<String> id, String serviceName, String script) {
        log.info(String.format("Executing script: %s, in container: %s", script, id));

        String[] commands = null;
        try {
            /**
             * Due to permissions problems with mounted volume script is moved to some local directory in container
             *
             * 1. create new directory in container
             * 2. move there script
             * 3. chmod script
             * 4. execute script
             */
            commands = new String[] {
                "bash", "-c",
                scriptExecCommand(script)
            };

            ExecCreateCmdResponse exec = dockerClient.execCreateCmd(id.get(0))
                    .withCmd(commands)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            dockerClient.execStartCmd(exec.getId())
                    .exec(new ExecStartResultCallback(System.out, System.err))
                    .awaitCompletion();
        } catch (DockerException | InterruptedException ex) {
            log.severe(String.format("Could not execute command: %s", Arrays.toString(commands)));
            throw new EnvironmentException("Could not execute command " + Arrays.toString(commands), ex);
        }
    }

    @Override
    public void stopAndRemove(List<String> ids) {
        for (String container: ids) {
            log.info(String.format("Cleaning environment %s", container));
            try {
                dockerClient.removeContainerCmd(container).withForce(true).exec();
            } catch (DockerException ex) {
                log.severe(String.format("Could not remove container: %s", container));
                throw new EnvironmentException("Could not remove container: " + container, ex);
            }
        }
    }

    private List<String> apmEnvVariables(Type type) {
        Properties properties = new Properties();
        InputStream is = getClass().getClassLoader().getResourceAsStream(type.getPropertyFile());

        if(is == null) {
            throw new EnvironmentException("Could not load env variables property file: " + type.getPropertyFile());
        }

        try {
            properties.load(is);
        } catch (IOException ex) {
            log.severe(String.format("Could not open properties file: %s", type.getPropertyFile()));
            throw new EnvironmentException("Could not load env variables property file: " + type.getPropertyFile(), ex);
        }

        List<String> envVariables = new ArrayList<>();
        for(String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);

            envVariables.add(key + "=" + value);
        }

        return envVariables;
    }
}
