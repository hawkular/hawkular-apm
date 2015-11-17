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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the query criteria for retrieving a set of completion times.
 *
 * @author gbrown
 */
public class CompletionTimeCriteria extends BaseCriteria {

    private final Logger log = Logger.getLogger(CompletionTimeCriteria.class.getName());

    private long upperBound;
    private long lowerBound;
    private Set<FaultCriteria> faults = new HashSet<FaultCriteria>();

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
     * @return the faults
     */
    public Set<FaultCriteria> getFaults() {
        return faults;
    }

    /**
     * @param fault the fault to set
     * @return The criteria
     */
    public CompletionTimeCriteria setFaults(Set<FaultCriteria> faults) {
        this.faults = faults;
        return this;
    }

    /**
     * This method returns the criteria as a map of name/value pairs.
     *
     * @return The criteria parameters
     */
    @Override
    public Map<String, String> parameters() {
        Map<String, String> ret = super.parameters();

        if (!getProperties().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (FaultCriteria pc : getFaults()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(pc.encoded());
            }

            ret.put("faults", buf.toString());
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("CompletionTimeCriteria parameters [" + ret + "]");
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CompletionTimeCriteria [upperBound=" + upperBound + ", lowerBound=" + lowerBound + ", faults="
                + faults + ", toString()=" + super.toString() + "]";
    }

    /**
     * This class represents the fault criteria.
     */
    public static class FaultCriteria {

        private String value;

        private boolean excluded = false;

        /**
         * This is the default constructor.
         */
        public FaultCriteria() {
        }

        /**
         * This constructor initialises the fields.
         *
         * @param value The value
         * @param excluded Whether excluded
         */
        public FaultCriteria(String value, boolean excluded) {
            this.value = value;
            this.excluded = excluded;
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
            buf.append(getValue());
            return buf.toString();
        }
    }
}
