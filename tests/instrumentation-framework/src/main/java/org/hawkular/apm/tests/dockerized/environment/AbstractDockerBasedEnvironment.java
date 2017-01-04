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
public abstract class AbstractDockerBasedEnvironment implements TestEnvironmentExecutor {
    private static final Logger log = Logger.getLogger(AbstractDockerBasedEnvironment.class.getName());

    protected String scenarioDirectory;
    protected String apmBindAddress;
    protected Network network;

    /**
     * Docker config
     */
    protected final DockerClient dockerClient;


    public AbstractDockerBasedEnvironment(String scenarioDirectory, String apmBindAddress) {
        this.scenarioDirectory = scenarioDirectory;
        this.apmBindAddress = apmBindAddress;
        this.dockerClient = DockerClientBuilder.getInstance().build();
    }


    @Override
    public void close() {
        removeNetwork();

        try {
            dockerClient.close();
        } catch (IOException ex) {
            log.severe("Could not close docker client");
            throw new EnvironmentException("Could not close docker client", ex);
        }
    }

    /**
     * Create network which is used as default in docker-compose.yml
     * This should be run before {@link DockerComposeExecutor#run(TestEnvironment)}
     */
    @Override
    public void createNetwork() {
        removeNetwork();

        String apmNetwork = apmBindAddress.substring(0, apmBindAddress.lastIndexOf(".")) + ".0/24";

        log.info(String.format("Creating network %s:", apmNetwork));

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

    private void removeNetwork() {

        String networkToRemove = network != null ? network.getName() : Constants.HOST_ADDED_TO_ETC_HOSTS;

        try {
            log.info(String.format("Removing network: %s", networkToRemove));
            dockerClient.removeNetworkCmd(networkToRemove).exec();
        } catch (DockerException ex) {
            log.severe(String.format("Could not remove network: %s", networkToRemove));
        }
    }
}
