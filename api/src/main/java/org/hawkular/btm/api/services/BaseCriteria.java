/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This class represents the base query criteria.
 *
 * @author gbrown
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = BusinessTransactionCriteria.class, name="BusinessTransaction"),
    @Type(value = CompletionTimeCriteria.class, name="CompletionTime"),
    @Type(value = NodeCriteria.class, name="Node") })
@ApiModel(subTypes = { BusinessTransactionCriteria.class, CompletionTimeCriteria.class,
        NodeCriteria.class}, discriminator = "type")
public abstract class BaseCriteria {

    private final Logger log = Logger.getLogger(BaseCriteria.class.getName());

    private long startTime = 0L;
    private long endTime = 0L;
    private String businessTransaction;
    private Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();
    private String hostName;

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
    public BaseCriteria setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * This method calculates the start time to use. If
     * the configured value is 0, then it will default to
     * an hour before the end time. If negative, then the
     * value will be deducted from the end time.
     *
     * @return The calculated start time
     */
    public long calculateStartTime() {
        if (startTime == 0) {
            // Set to 1 hour before end time
            return calculateEndTime() - 3600000;
        } else if (startTime < 0) {
            return calculateEndTime() + startTime;
        }
        return startTime;
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
    public BaseCriteria setEndTime(long endTime) {
        this.endTime = endTime;
        return this;
    }

    /**
     * This method returns an end time based on the configured
     * value. If end time is less or equal to 0, then its value
     * will be deducted from the current time.
     *
     * @return The calculated end time
     */
    public long calculateEndTime() {
        if (endTime == 0) {
            return System.currentTimeMillis();
        } else if (endTime < 0) {
            return System.currentTimeMillis() - endTime;
        }
        return endTime;
    }

    /**
     * @return the business transaction
     */
    public String getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * If a null name is used, then it will match any transaction whether it has
     * a name or not. If the supplied name is an empty string, then it will match
     * only transactions that don't have a name. If a name is specified, then
     * only transactions with that business transaction name will be selected.
     *
     * @param name the business transaction name to set
     * @return The criteria
     */
    public BaseCriteria setBusinessTransaction(String name) {
        this.businessTransaction = name;
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
    public BaseCriteria setProperties(Set<PropertyCriteria> properties) {
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
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     * @return The criteria
     */
    public BaseCriteria setHostName(String hostName) {
        this.hostName = hostName;
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
     * This method returns the criteria as a map of name/value pairs.
     * The properties and correlation ids are returned as a single
     * entry with | separators.
     *
     * @return The criteria parameters
     */
    public Map<String, String> parameters() {
        Map<String, String> ret = new HashMap<String, String>();

        if (getBusinessTransaction() != null) {
            ret.put("businessTransaction", getBusinessTransaction());
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

        if (hostName != null) {
            ret.put("hostName", hostName);
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
        return "BaseCriteria [log=" + log + ", startTime=" + startTime + ", endTime=" + endTime
                + ", businessTransaction=" + businessTransaction + ", properties=" + properties + ", hostName="
                + hostName + ", timeout=" + timeout + ", maxResponseSize=" + maxResponseSize + ", toString()="
                + super.toString() + "]";
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
