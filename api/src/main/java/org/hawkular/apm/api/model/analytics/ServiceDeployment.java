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
package org.hawkular.apm.api.model.analytics;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a service deployment, including a list of build stamps.
 *
 * @author Juraci Paixão Kröhling
 */
public class ServiceDeployment {
    @JsonInclude
    private final String name;

    @JsonInclude
    private final List<BuildStamp> buildStamps;

    /**
     * Creates a new Service Deployment, based on the given name and list of build stamps.
     * @param name        the service name. Required.
     * @param buildStamps the list of build stamps. Optional.
     */
    public ServiceDeployment(String name, List<BuildStamp> buildStamps) {
        if (null == name || name.isEmpty()) {
            throw new IllegalStateException("The service name cannot be null nor empty.");
        }

        if (null == buildStamps) {
            buildStamps = Collections.emptyList();
        }

        this.name = name;
        this.buildStamps = buildStamps;
    }

    /**
     * The service name.
     * @return the service name
     */
    public String getName() {
        return name;
    }

    /**
     * A list of build stamps. This list might not be comprehensive and might contain only a limited number of items.
     * @return a list of build stamps. If no build stamps exist for this service name, an empty list is returned.
     */
    public List<BuildStamp> getBuildStamps() {
        return buildStamps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceDeployment that = (ServiceDeployment) o;

        if (!name.equals(that.name)) return false;
        return buildStamps.equals(that.buildStamps);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + buildStamps.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceDeployment{" +
                "name='" + name + '\'' +
                ", buildStamps=" + buildStamps +
                '}';
    }
}
