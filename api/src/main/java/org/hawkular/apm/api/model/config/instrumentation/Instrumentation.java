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
package org.hawkular.apm.api.model.config.instrumentation;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class describes the instrumentation requirements for a particular environment.
 *
 * @author gbrown
 */
public class Instrumentation {

    @JsonInclude(Include.NON_NULL)
    private String description;

    @JsonInclude(Include.NON_DEFAULT)
    private boolean compile = true;

    @JsonInclude
    private List<InstrumentRule> rules = new ArrayList<InstrumentRule>();

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the compile
     */
    public boolean isCompile() {
        return compile;
    }

    /**
     * @param compile the compile to set
     */
    public void setCompile(boolean compile) {
        this.compile = compile;
    }

    /**
     * @return the rules
     */
    public List<InstrumentRule> getRules() {
        return rules;
    }

    /**
     * @param rules the rules to set
     */
    public void setRules(List<InstrumentRule> rules) {
        this.rules = rules;
    }

}
