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
    private Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();
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
    public Set<PropertyCriteria> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     * @return The criteria
     */
    public BusinessTransactionCriteria setProperties(Set<PropertyCriteria> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * This method adds a new property criteria.
     *
     * @param name The property name
     * @param value The property value
     * @param excluded Whether the specific property name/value should be excluded
     */
    public void addProperty(String name, String value, boolean excluded) {
        properties.add(new PropertyCriteria(name, value, excluded));
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
                    log.finest("Name mismatch, was '" + btxn.getName() + "' required '" + name + "'");
                }
                return false;
            }
        }

        if (!properties.isEmpty()) {
            for (PropertyCriteria property : properties) {
                String value = btxn.getProperties().get(property.getName());
                if (value == null) {
                    if (!property.isExcluded()) {
                        log.finest("Property '" + property.getName() + "' not found");
                        return false;
                    }
                } else if (property.getValue().equals(value) == property.isExcluded()) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Property match failed: criteria=" + property
                                + " txn property value=" + value);
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

    /**
     * This method returns the criteria as a map of name/value pairs.
     * The properties and correlation ids are returned as a single
     * entry with | separators.
     *
     * @return The criteria parameters
     */
    public Map<String, String> parameters() {
        Map<String, String> ret = new HashMap<String, String>();

        if (getName() != null) {
            ret.put("name", getName());
        }

        if (getStartTime() > 0) {
            ret.put("startTime", "" + getStartTime());
        }

        if (getEndTime() > 0) {
            ret.put("endTime", "" + getEndTime());
        }

        if (!getProperties().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (PropertyCriteria pc : getProperties()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(pc.encoded());
            }

            ret.put("properties", buf.toString());
        }

        if (!getCorrelationIds().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (CorrelationIdentifier cid : getCorrelationIds()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(cid.getScope().name());
                buf.append('|');
                buf.append(cid.getValue());
            }

            ret.put("correlations", buf.toString());
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Criteria parameters [" + ret + "]");
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BusinessTransactionCriteria [startTime=" + startTime + ", endTime=" + endTime
                + ", name=" + name + ", properties=" + properties + ", correlationIds=" + correlationIds + "]";
    }

    /**
     * This class represents the property criteria.
     */
    public static class PropertyCriteria {

        private String name;
        private String value;

        private boolean excluded = false;

        /**
         * This is the default constructor.
         */
        public PropertyCriteria() {
        }

        /**
         * This constructor initialises the fields.
         *
         * @param name The name
         * @param value The value
         * @param excluded Whether excluded
         */
        public PropertyCriteria(String name, String value, boolean excluded) {
            this.name = name;
            this.value = value;
            this.excluded = excluded;
        }

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
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * @return the excluded
         */
        public boolean isExcluded() {
            return excluded;
        }

        /**
         * @param excluded the excluded to set
         */
        public void setExcluded(boolean excluded) {
            this.excluded = excluded;
        }

        /**
         * This method returns an encoded form for the
         * property criteria.
         *
         * @return The encoded form
         */
        public String encoded() {
            StringBuilder buf = new StringBuilder();
            if (isExcluded()) {
                buf.append('-');
            }
            buf.append(getName());
            buf.append('|');
            buf.append(getValue());
            return buf.toString();
        }
    }
}
