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
 * Test environment
 *
 * @author Pavol Loffay
 */
public class TestEnvironment {

    private String image;
    private List<String> dockerCompose;
    private boolean pull;
    private long initWaitSeconds;
    private Type type;
    private String apmAddress;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getInitWaitSeconds() {
        return initWaitSeconds;
    }

    public void setInitWaitSeconds(long initWaitSeconds) {
        this.initWaitSeconds = initWaitSeconds;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getDockerCompose() {
        return dockerCompose;
    }

    public void setDockerCompose(List<String> dockerCompose) {
        this.dockerCompose = dockerCompose;
    }

    public String getApmAddress() {
        return apmAddress;
    }

    public void setApmAddress(String apmAddress) {
        this.apmAddress = apmAddress;
    }

    public boolean isPull() {
        return pull;
    }

    public void setPull(boolean pull) {
        this.pull = pull;
    }

    @Override
    public String toString() {
        return "TestEnvironment{" +
                "image='" + image + '\'' +
                ", dockerCompose='" + dockerCompose + '\'' +
                ", pull=" + pull +
                ", initWaitSeconds=" + initWaitSeconds +
                ", type=" + type +
                ", apmAddress='" + apmAddress + '\'' +
                '}';
    }
}
