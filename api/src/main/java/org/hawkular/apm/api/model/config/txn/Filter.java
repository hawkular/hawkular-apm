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
package org.hawkular.apm.api.model.config.txn;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class represents a filter.
 *
 * @author gbrown
 */
public class Filter {

    @JsonInclude
    private List<String> inclusions = new ArrayList<String>();

    @JsonInclude
    private List<String> exclusions = new ArrayList<String>();

    /**
     * @return the inclusions
     */
    public List<String> getInclusions() {
        return inclusions;
    }

    /**
     * @param inclusions the inclusions to set
     */
    public void setInclusions(List<String> inclusions) {
        this.inclusions = inclusions;
    }

    /**
     * @return the exclusions
     */
    public List<String> getExclusions() {
        return exclusions;
    }

    /**
     * @param exclusions the exclusions to set
     */
    public void setExclusions(List<String> exclusions) {
        this.exclusions = exclusions;
    }

    @Override
    public String toString() {
        return "Filter [inclusions=" + inclusions + ", exclusions=" + exclusions + "]";
    }

}
