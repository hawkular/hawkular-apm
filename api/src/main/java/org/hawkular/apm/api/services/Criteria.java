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
package org.hawkular.apm.api.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;

/**
 * This class represents the base query criteria.
 *
 * @author gbrown
 */
public class Criteria {

    private static final Logger log = Logger.getLogger(Criteria.class.getName());

    /**
     * Start time in milliseconds
     */
    private long startTime = 0L;
    /**
     * End time in milliseconds
     */
    private long endTime = 0L;
    private String transaction;
    private Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();
    private Set<CorrelationIdentifier> correlationIds = new HashSet<CorrelationIdentifier>();
    private String hostName;
    private long upperBound;
    private long lowerBound;
    private String uri;
    private String operation;
    private long timeout = 10000;
    private int maxResponseSize = 100000;

    /**
     * Default constructor.
     */
    public Criteria() {
    }

    /**
     * Copy constructor.
     *
     * @param criteria The criteria to copy
     */
    public Criteria(Criteria criteria) {
        if (null != criteria) {
            this.startTime = criteria.startTime;
            this.endTime = criteria.endTime;
            this.transaction = criteria.transaction;
            this.hostName = criteria.hostName;
            this.upperBound = criteria.upperBound;
            this.lowerBound = criteria.lowerBound;
            this.uri = criteria.uri;
            this.operation = criteria.operation;

            criteria.properties.forEach(pc -> this.properties.add(new PropertyCriteria(pc)));
            criteria.correlationIds.forEach(cid -> this.correlationIds.add(new CorrelationIdentifier(cid)));
        }
    }

    /**
     * @return the startTime in milliseconds, or 0 meaning 1 hours ago
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime in milliseconds
     * @return The criteria
     */
    public Criteria setStartTime(long startTime) {
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
     * @return the endTime in milliseconds, or 0 meaning 'current time'
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime the endTime in milliseconds
     * @return The criteria
     */
    public Criteria setEndTime(long endTime) {
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
     * @return the transaction
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * If a null name is used, then it will match any transaction whether it has
     * a name or not. If the supplied name is an empty string, then it will match
     * only transactions that don't have a name. If a name is specified, then
     * only transactions with that transaction name will be selected.
     *
     * @param name the transaction name to set
     * @return The criteria
     */
    public Criteria setTransaction(String name) {
        this.transaction = name;
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
    public Criteria setProperties(Set<PropertyCriteria> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * This method adds a new property criteria.
     *
     * @param name The property name
     * @param value The property value
     * @param operator The property operator
     * @return The criteria
     */
    public Criteria addProperty(String name, String value, Operator operator) {
        properties.add(new PropertyCriteria(name, value, operator));
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
    public Criteria setCorrelationIds(Set<CorrelationIdentifier> correlationIds) {
        this.correlationIds = correlationIds;
        return this;
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
    public Criteria setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    /**
     * @return the upperBound
     */
    public long getUpperBound() {
        return upperBound;
    }

    /**
     * @param upperBound the upperBound to set
     */
    public void setUpperBound(long upperBound) {
        this.upperBound = upperBound;
    }

    /**
     * @return the lowerBound
     */
    public long getLowerBound() {
        return lowerBound;
    }

    /**
     * @param lowerBound the lowerBound to set
     */
    public void setLowerBound(long lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     * @return The criteria
     */
    public Criteria setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     * @return The criteria
     */
    public Criteria setOperation(String operation) {
        this.operation = operation;
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

        if (getTransaction() != null) {
            ret.put("transaction", getTransaction());
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

        // Only relevant for trace fragments
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

        if (uri != null) {
            ret.put("uri", uri);
        }

        if (operation != null) {
            ret.put("operation", operation);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Criteria parameters [" + ret + "]");
        }

        return ret;
    }

    /**
     * This method determines if the specified criteria are relevant to all fragments within
     * an end to end transaction.
     *
     * @return Whether the criteria would apply to all fragments in a transaction
     */
    public boolean transactionWide() {
        return !(!properties.isEmpty() || !correlationIds.isEmpty() || hostName != null
                || uri != null || operation != null);
    }

    /**
     * This method returns the transaction wide version of the current criteria.
     *
     * @return The transaction wide version
     */
    public Criteria deriveTransactionWide() {
        Criteria ret = new Criteria();
        ret.setStartTime(startTime);
        ret.setEndTime(endTime);
        ret.setProperties(getProperties().stream().filter(p -> p.getName().equals(Constants.PROP_PRINCIPAL))
                .collect(Collectors.toSet()));
        ret.setTransaction(transaction);
        return ret;
    }

    @Override
    public String toString() {
        return "Criteria [startTime=" + startTime + ", endTime=" + endTime + ", transaction="
                + transaction + ", properties=" + properties + ", correlationIds=" + correlationIds
                + ", hostName=" + hostName + ", upperBound=" + upperBound + ", lowerBound=" + lowerBound + ", uri="
                + uri + ", operation=" + operation + ", timeout=" + timeout + ", maxResponseSize=" + maxResponseSize
                + "]";
    }

    /**
     * The enum for the comparison operators. The operators are specific
     * to the property type (e.g. Text, Number)
     */
    public static enum Operator {

        /* Text value - matching property/fault operator */
        HAS,

        /* Text value - no matching property/fault operator */
        HASNOT,

        /* Number value - property equality operator */
        EQ,

        /* Number value - property inequality operator */
        NE,

        /* Number value - property greater-than operator */
        GT,

        /* Number value - property greater-than-or-equal operator */
        GTE,

        /* Number value - property less-than operator */
        LT,

        /* Number value - property less-than-or-equal operator */
        LTE

    }

    /**
     * This class represents the property criteria.
     */
    public static class PropertyCriteria {

        private String name;
        private String value;

        private Operator operator = Operator.HAS;

        /**
         * This is the default constructor.
         */
        public PropertyCriteria() {
        }

        /**
         * The copy constructor.
         *
         * @param pc The property criteria
         */
        public PropertyCriteria(PropertyCriteria pc) {
            this.name = pc.name;
            this.value = pc.value;
            this.operator = pc.operator;
        }

        /**
         * This constructor initialises the fields.
         *
         * @param name The name
         * @param value The value
         * @param operator The comparison operator
         */
        public PropertyCriteria(String name, String value, Operator operator) {
            this.name = name;
            this.value = value;
            this.setOperator(operator);
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
         * @return the operator
         */
        public Operator getOperator() {
            return operator;
        }

        /**
         * @param operator the operator to set
         */
        public void setOperator(Operator operator) {
            if (operator == null) {
                operator = Operator.HAS;
            }
            this.operator = operator;
        }

        /**
         * This method returns an encoded form for the
         * property criteria.
         *
         * @return The encoded form
         */
        public String encoded() {
            StringBuilder buf = new StringBuilder();
            buf.append(getName());
            buf.append('|');
            buf.append(getValue());
            if (getOperator() != Operator.HAS) {
                buf.append('|');
                buf.append(getOperator());
            }
            return buf.toString();
        }

        @Override
        public String toString() {
            return "PropertyCriteria [name=" + name + ", value=" + value + ", operator=" + operator + "]";
        }

    }

}
