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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.Node;

import io.swagger.annotations.ApiModel;

/**
 * This class represents the query criteria for retrieving a set of business
 * transaction (fragments).
 *
 * @author gbrown
 */
@ApiModel(parent = BaseCriteria.class)
public class BusinessTransactionCriteria extends BaseCriteria {

    private final Logger log = Logger.getLogger(BusinessTransactionCriteria.class.getName());

    private Set<CorrelationIdentifier> correlationIds = new HashSet<CorrelationIdentifier>();

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
     * This method determines whether the supplied business transaction
     * meets the criteria.
     *
     * @param btxn The business transaction
     * @return Whether the business transaction meets the query criteria
     */
    public boolean isValid(BusinessTransaction btxn) {

        // Validate criteria
        if (getStartTime() > 0L && btxn.getStartTime() < getStartTime()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Start time out of range");
            }
            return false;
        }

        if (getEndTime() > 0L && btxn.getStartTime() > getEndTime()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("End time out of range");
            }
            return false;
        }

        if (getBusinessTransaction() != null) {
            if (getBusinessTransaction().trim().length() == 0) {
                if (btxn.getName() != null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Business transaction name is defined");
                    }
                    return false;
                }
            } else if (!getBusinessTransaction().equals(btxn.getName())) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Business transaction name mismatch, was '" + btxn.getName()
                            + "' required '" + getBusinessTransaction() + "'");
                }
                return false;
            }
        }

        if (!getProperties().isEmpty()) {
            for (PropertyCriteria property : getProperties()) {
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
    @Override
    public Map<String, String> parameters() {
        Map<String, String> ret = super.parameters();

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
            log.finest("BusinessTransactionCriteria parameters [" + ret + "]");
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BusinessTransactionCriteria [correlationIds=" + correlationIds + ", toString()=" + super.toString()
                + "]";
    }

}
