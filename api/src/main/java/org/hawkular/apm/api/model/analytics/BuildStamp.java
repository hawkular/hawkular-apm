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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a specific build stamp, with a basic list of statistics.
 * @author Juraci Paixão Kröhling
 */
public class BuildStamp {
    @JsonInclude
    private final String value;

    /**
     * Creates a new BuildStamp based on its value, such as "foo-1".
     *
     * @param value      the build stamp value, such as "foo-1". Required.
     */
    public BuildStamp(String value) {
        if (null == value || value.isEmpty()) {
            throw new IllegalStateException("The build stamp value cannot be null nor empty.");
        }

        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildStamp that = (BuildStamp) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override public String toString() {
        return "BuildStamp{" +
                "value='" + value + '\'' +
                '}';
    }
}
