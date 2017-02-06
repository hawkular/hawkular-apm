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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public List<String> run(TestEnvironment testEnvironment) {
        if (testEnvironment.isPull()) {
            List<String> cmd = composeCommandWithFiles(testEnvironment.getDockerCompose());
            cmd.add("pull");
            runShellCommand(cmd);
        }

        /**
         * Note that if image is not in local cache it will be download from the hub
         */
        List<String> cmd = composeCommandWithFiles(testEnvironment.getDockerCompose());
        cmd.addAll(Arrays.asList("up", "-d"));
        runShellCommand(cmd);

        return testEnvironment.getDockerCompose();
    }

    @Override
    public void stopAndRemove(List<String> ids) {
        List<String> cmd = composeCommandWithFiles(ids);
        cmd.addAll(Arrays.asList("down", "--rmi", "all"));
        try {
            runShellCommand(cmd);
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
    public void execScript(List<String> id, String serviceName, String script) {
        List<String> cmd = composeCommandWithFiles(id);
        cmd.addAll(Arrays.asList("exec", serviceName, "bash", "-c", scriptExecCommand(script)));
        runShellCommand(cmd);
    }

    private void runShellCommand(List<String> commands) {

        log.info(String.format("Executing command on host OS: `%s`", commands));

        try {
            Process process = Runtime.getRuntime().exec(commands.toArray(new String[0]));

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
                log.severe(String.format("`%s` did not return 0", commands));
                log.severe("-------- stderr ");
                log.severe(new BufferedReader(new InputStreamReader(process.getErrorStream())).lines().collect(Collectors.joining("\n")));
                log.severe("-------- /stderr ");
                throw new EnvironmentException(commands + " did not return 0, actual = " +
                        (process != null ? process.exitValue(): ""));
            }

        } catch (IOException | InterruptedException ex) {
            log.severe(String.format("Could not run: %s", commands));
            throw new EnvironmentException("Could not run: " + commands, ex);
        }

        log.info(String.format("Command `%s`, successfully executed", commands));
    }

    private List<String> composeCommandWithFiles(List<String> composeFiles) {
        List<String> composeCmdWithArgs = new ArrayList<>(composeFiles.size()*2 + 1);
        composeCmdWithArgs.add("docker-compose");

        for (String composeFile: composeFiles) {
            composeCmdWithArgs.add("-f");
            composeCmdWithArgs.add(scenarioDirectory + File.separator + composeFile);
        }
        return composeCmdWithArgs;
    }
}
