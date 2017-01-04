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
 * @author Pavol Loffay
 */
public enum Type {
    /**
     * Will instrument the application using the Hawkular APM Java agent.
     */
    APMAGENT("apmagent-env-variables.properties"),
    /**
     * Will instrument the application using the Hawkular APM Java opentracing agent.
     */
    APMOTAGENT("apmotagent-env-variables.properties"),
    /**
     * Application will be reporting tracing information in APM format.
     */
    APM("apm-env-variables.properties"),
    /**
     * Application will be reporting tracing information in Zipkin format.
     */
    ZIPKIN("zipkin-env-variables.properties");

    private String propertyFile;

    Type(String propertyFile) {
        this.propertyFile = propertyFile;
    }

    public String getPropertyFile() {
        return propertyFile;
    }
}
