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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class represents a set of percentiles.
 *
 * @author gbrown
 */
public class Percentiles {

    @JsonInclude
    private Map<Integer,Long> percentiles = new HashMap<Integer,Long>();

    /**
     * @return the percentiles
     */
    public Map<Integer, Long> getPercentiles() {
        return percentiles;
    }

    /**
     * @param percentiles the percentiles to set
     */
    public void setPercentiles(Map<Integer, Long> percentiles) {
        this.percentiles = percentiles;
    }

    /**
     * This method adds a percentile.
     *
     * @param percent The percentage
     * @param value The value
     */
    public void addPercentile(int percent, long value) {
        percentiles.put(percent, value);
    }

}
