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
    private String name;
    private Map<String, String> properties = new HashMap<String, String>();
    private Set<CorrelationIdentifier> correlationIds = new HashSet<CorrelationIdentifier>();

    /**  */
    private static int DEFAULT_RESPONSE_SIZE = 100000;

    /**  */
    private static long DEFAULT_TIMEOUT = 10000L;

    private long timeout = DEFAULT_TIMEOUT;

    private int maxResponseSize = DEFAULT_RESPONSE_SIZE;

    /**
     * @return the startTime, or 0 meaning 1 hours ago
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
     * @return the endTime, or 0 meaning 'current time'
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * If a null name is used, then it will match any transaction whether it has
     * a name or not. If the supplied name is an empty string, then it will match
     * only transactions that don't have a name. If a name is specified, then
     * only transactions with that business transaction name will be selected.
     *
     * @param name the name to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setName(String name) {
        this.name = name;
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
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the maxResponseSize
     */
    public int getMaxResponseSize() {
        return maxResponseSize;
    }

    /**
     * @param maxResponseSize the maxResponseSize to set
     */
    public void setMaxResponseSize(int maxResponseSize) {
        this.maxResponseSize = maxResponseSize;
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
        if (startTime > 0L && btxn.getStartTime() < startTime) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Start time out of range");
            }
            return false;
        }

        if (endTime > 0L && btxn.getStartTime() > endTime) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("End time out of range");
            }
            return false;
        }

        if (name != null) {
            if (name.trim().length() == 0) {
                if (btxn.getName() != null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Name is defined");
                    }
                    return false;
                }
            } else if (!name.equals(btxn.getName())) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Name mismatch, was '"+btxn.getName()+"' required '"+name+"'");
                }
                return false;
            }
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
                Set<Node> nodes = btxn.getCorrelatedNodes(ci);

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
        return "BusinessTransactionCriteria [startTime=" + startTime + ", endTime=" + endTime
                + ", name=" + name + ", properties=" + properties + ", correlationIds=" + correlationIds + "]";
    }

}
