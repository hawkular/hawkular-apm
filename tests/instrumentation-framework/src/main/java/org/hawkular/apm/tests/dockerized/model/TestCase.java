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

/**
 * Class representing test case.
 *
 * @author Pavol Loffay
 */
public class TestCase {

    private String description;
    private boolean skip;
    /**
     * Script which is run to execute some actions within example application inside docker container.
     * This action can be shell, python... It only depends on what is installed on the host OS.
     */
    private String action;
    /**
     * Service from docker-compose in which the action will be invoked
     */
    private String scriptServiceName;
    private TestCaseVerify verify;
    /**
     * Time in seconds to wait after executing {@link TestCase#action}
     */
    private long afterActionWaitSeconds;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public TestCaseVerify getVerify() {
        return verify;
    }

    public void setVerify(TestCaseVerify verify) {
        this.verify = verify;
    }

    public long getAfterActionWaitSeconds() {
        return afterActionWaitSeconds;
    }

    public void setAfterActionWaitSeconds(long afterActionWaitSeconds) {
        this.afterActionWaitSeconds = afterActionWaitSeconds;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String getScriptServiceName() {
        return scriptServiceName;
    }

    public void setScriptServiceName(String scriptServiceName) {
        this.scriptServiceName = scriptServiceName;
    }

    @Override
    public String toString() {
        return "TestCase{" +
                "description='" + description + '\'' +
                ", skip=" + skip +
                ", action='" + action + '\'' +
                ", scriptServiceName='" + scriptServiceName + '\'' +
                ", verify=" + verify +
                ", afterActionWaitSeconds=" + afterActionWaitSeconds +
                '}';
    }
}
