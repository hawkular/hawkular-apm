/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.Node;

/**
 * This class represents the query criteria for retrieving a set of business
 * transaction (fragments).
 *
 * @author gbrown
 */
public class BusinessTransactionCriteria {

    private final Logger log = Logger.getLogger(BusinessTransactionCriteria.class.getName());

    private long startTime = 0L;
    private long endTime = 0L;
    private Map<String, String> properties = new HashMap<String, String>();
    private Set<CorrelationIdentifier> correlationIds = new HashSet<CorrelationIdentifier>();
    private long correlationTime = 0L;

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * @return the endTime
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setEndTime(long endTime) {
        this.endTime = endTime;
        return this;
    }

    /**
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * @return the correlationIds
     */
    public Set<CorrelationIdentifier> getCorrelationIds() {
        return correlationIds;
    }

    /**
     * @param correlationIds the correlationIds to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setCorrelationIds(Set<CorrelationIdentifier> correlationIds) {
        this.correlationIds = correlationIds;
        return this;
    }

    /**
     * @return the correlationTime
     */
    public long getCorrelationTime() {
        return correlationTime;
    }

    /**
     * @param correlationTime the correlationTime to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setCorrelationTime(long correlationTime) {
        this.correlationTime = correlationTime;
        return this;
    }

    /**
     * This method determines whether the supplied business transaction
     * meets the criteria.
     *
     * @param btxn The business transaction
     * @return Whether the business transaction meets the query criteria
     */
    public boolean isValid(BusinessTransaction btxn) {

        // Validate criteria
        if (startTime > 0L && btxn.startTime() < startTime) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Start time out of range");
            }
            return false;
        }

        if (endTime > 0L && btxn.endTime() > endTime) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("End time out of range");
            }
            return false;
        }

        if (!properties.isEmpty()) {
            for (String key : properties.keySet()) {
                String value = properties.get(key);
                String result = btxn.getProperties().get(key);
                if (result == null || !value.equals(result)) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Property '" + key + "' had value '" + result
                                + "', expected '" + value + "'");
                    }
                    return false;
                }
            }
        }

        if (!correlationIds.isEmpty()) {
            for (CorrelationIdentifier ci : correlationIds) {
                Set<Node> nodes = btxn.getCorrelatedNodes(ci,
                        (correlationTime == 0L ? System.currentTimeMillis() : correlationTime));

                if (!nodes.isEmpty()) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BusinessTransactionCriteria [startTime=" + startTime + ", endTime=" + endTime + ", properties="
                + properties + ", correlationIds=" + correlationIds + ", correlationTime=" + correlationTime + "]";
    }
}
