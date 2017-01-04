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

import org.hawkular.apm.api.model.config.ReportingLevel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a transaction configuration.
 *
 * @author gbrown
 */
public class TransactionConfig {

    @JsonInclude
    private ReportingLevel level = ReportingLevel.All;

    @JsonInclude(Include.NON_NULL)
    private String description;

    @JsonInclude
    private Filter filter;

    @JsonInclude
    private List<Processor> processors = new ArrayList<Processor>();

    /**
     * Last updated in microseconds
     */
    @JsonInclude(Include.NON_NULL)
    private long lastUpdated;

    @JsonInclude(Include.NON_DEFAULT)
    private boolean deleted = false;

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

    /**
     * @return the filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * @return the processors
     */
    public List<Processor> getProcessors() {
        return processors;
    }

    /**
     * @param processors the processors to set
     */
    public void setProcessors(List<Processor> processors) {
        this.processors = processors;
    }

    /**
     * @return the lastUpdated
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * @param lastUpdated the lastUpdated to set
     */
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * @return the deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param deleted the deleted to set
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "BusinessTxnConfig [level=" + level + ", description=" + description + ", filter=" + filter
                + ", processors=" + processors + ", lastUpdated=" + lastUpdated + ", deleted=" + deleted + "]";
    }

}
