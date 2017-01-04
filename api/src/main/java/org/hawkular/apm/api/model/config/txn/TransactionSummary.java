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

import org.hawkular.apm.api.model.config.ReportingLevel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a transaction summary.
 *
 * @author gbrown
 */
public class TransactionSummary {

    @JsonInclude
    private String name;

    @JsonInclude
    private ReportingLevel level = ReportingLevel.All;

    @JsonInclude(Include.NON_NULL)
    private String description;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the level
     */
    public ReportingLevel getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(ReportingLevel level) {
        this.level = level;
    }

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

    @Override
    public String toString() {
        return "BusinessTxnSummary [name=" + name + ", level=" + level + ", description=" + description + "]";
    }

}
