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

import org.hawkular.apm.api.model.config.ReportingLevel;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class represents information related to a transaction associated with
 * trace fragments.
 *
 * @author gbrown
 */
public class TransactionInfo {

    @JsonInclude
    private String name;

    @JsonInclude
    private long count;

    @JsonInclude
    private ReportingLevel level = ReportingLevel.All;

    @JsonInclude
    private boolean staticConfig = false;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @return The transaction info
     */
    public TransactionInfo setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * @param count the count to set
     * @return The transaction info
     */
    public TransactionInfo setCount(long count) {
        this.count = count;
        return this;
    }

    /**
     * @return the level
     */
    public ReportingLevel getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     * @return The transaction info
     */
    public TransactionInfo setLevel(ReportingLevel level) {
        this.level = level;
        return this;
    }

    /**
     * @return the staticConfig
     */
    public boolean isStaticConfig() {
        return staticConfig;
    }

    /**
     * @param staticConfig the staticConfig to set
     * @return The transaction info
     */
    public TransactionInfo setStaticConfig(boolean staticConfig) {
        this.staticConfig = staticConfig;
        return this;
    }

    @Override
    public String toString() {
        return "TransactionInfo [name=" + name + ", count=" + count + ", level=" + level + ", staticConfig="
                + staticConfig + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (count ^ (count >>> 32));
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (staticConfig ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TransactionInfo other = (TransactionInfo) obj;
        if (count != other.count)
            return false;
        if (level != other.level)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (staticConfig != other.staticConfig)
            return false;
        return true;
    }

}
