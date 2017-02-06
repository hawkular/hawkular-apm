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

import org.hawkular.apm.api.model.config.instrumentation.jvm.JVM;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This class represents an instrumentation rule.
 *
 * @author gbrown
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "ruleType")
@JsonSubTypes({ @Type(value = JVM.class) })
@ApiModel(subTypes = { JVM.class }, discriminator = "ruleType")
public class InstrumentRule {

    @JsonInclude
    private String ruleName;

    @JsonInclude
    private List<String> notes = new ArrayList<String>();

    /**
     * @return the ruleName
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * @param ruleName the ruleName to set
     */
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    /**
     * @return the notes
     */
    public List<String> getNotes() {
        return notes;
    }

    /**
     * @param notes the notes to set
     */
    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

}
