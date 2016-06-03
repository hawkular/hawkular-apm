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
package org.hawkular.apm.server.cassandra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.FaultCriteria;
import org.hawkular.apm.api.services.Criteria.PropertyCriteria;

/**
 * @author gbrown
 */
public class CassandraServiceUtil {

    /**  */
    protected static final String SEPARATOR = ":";

    /**
     * This method converts a map of properties to a list of tags.
     *
     * @param map The map
     * @return The list of tags
     */
    public static List<String> toTagList(Map<String, String> map) {
        List<String> ret = new ArrayList<String>();

        for (String key : map.keySet()) {
            String value = map.get(key);
            ret.add(key + SEPARATOR + value);
        }

        return ret;
    }

    /**
     * This method converts a list of correlation ids to a list of
     * tags.
     *
     * @param cids The list of correlation ids
     * @return The list of tags
     */
    public static List<String> toTagList(List<CorrelationIdentifier> cids) {
        List<String> ret = new ArrayList<String>();

        if (cids != null) {
            for (int i = 0; i < cids.size(); i++) {
                CorrelationIdentifier cid = cids.get(i);
                ret.add(cid.getScope().name() + SEPARATOR + cid.getValue());
            }
        }

        return ret;
    }

    /**
     * This method builds a where clause for the supplied criteria.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The where clause
     */
    public static String whereClause(String tenantId, Criteria criteria) {
        StringBuilder ret = new StringBuilder();

        ret.append(" WHERE tenantId = '");
        ret.append(tenant(tenantId));
        ret.append("'");

        ret.append(" AND ");
        ret.append("datetime >= ");
        ret.append(criteria.calculateStartTime());

        if (criteria.getEndTime() != 0) {
            ret.append(" AND ");
            ret.append("datetime < ");
            ret.append(criteria.calculateEndTime());
        }

        if (criteria.getBusinessTransaction() != null) {
            ret.append(" AND ");
            ret.append("businessTransaction = '");
            ret.append(criteria.getBusinessTransaction());
            ret.append("'");
        }

        if (criteria.getHostName() != null
                && criteria.getHostName().trim().length() > 0) {
            ret.append(" AND ");
            ret.append("hostName = '");
            ret.append(criteria.getHostName());
            ret.append("'");
        }

        for (PropertyCriteria pc : criteria.getProperties()) {

            // TODO: Currently ignoring 'excluded' properties - but
            // need to find a way to handle - either using Stratio or
            // client side filtering

            if (!pc.isExcluded()) {
                ret.append(" AND ");
                ret.append("properties CONTAINS '");
                ret.append(propertyEncoding(pc));
                ret.append('\'');
            }
        }

        for (CorrelationIdentifier ci : criteria.getCorrelationIds()) {
            ret.append(" AND ");
            ret.append("correlationIds CONTAINS '");
            ret.append(propertyEncoding(ci));
            ret.append('\'');
        }

        for (org.hawkular.apm.api.services.Criteria.FaultCriteria fc : criteria.getFaults()) {
            // TODO: Currently ignoring 'excluded' faults - but
            // need to find a way to handle - either using Stratio or
            // client side filtering

            if (!fc.isExcluded()) {
                ret.append(" AND ");
                ret.append("fault = '");
                ret.append(fc.getValue());
                ret.append('\'');
            }
        }

        // NOTE: Upper and lower bounds cannot be added to query

        if (ret.length() > 0) {
            return ret.toString();
        }

        return null;
    }

    /**
     * This method returns the encoding of the property name and value.
     *
     * @param pc The property criteria
     * @return The encoded form
     */
    public static String propertyEncoding(PropertyCriteria pc) {
        return pc.getName() + ":" + pc.getValue();
    }

    /**
     * This method returns the encoding of the correlation identifier.
     *
     * @param ci The correlation identifier
     * @return The encoded form
     */
    public static String propertyEncoding(CorrelationIdentifier ci) {
        return ci.getScope().name() + ":" + ci.getValue();
    }

    /**
     * This method returns the tenant to use in the Cassandra db.
     *
     * @param id The id
     * @return The id, or 'default' if id is null
     */
    public static String tenant(String id) {
        if (id == null) {
            return "default";
        }
        return id;
    }

    /**
     * This method converts a null value into an empty string.
     *
     * @param value The value
     * @return The value, or empty string if value is null
     */
    public static String emptyStringForNull(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    /**
     * This method determines if the supplied details should be excluded based
     * on the criteria.
     *
     * @param properties The properties
     * @param fault The fault
     * @param criteria The criteria
     * @return Whether the information should be excluded
     */
    public static boolean exclude(Map<String, String> properties, String fault, Criteria criteria) {
        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded() && properties.containsKey(pc.getName())
                        && properties.get(pc.getName()).equals(pc.getValue())) {
                    return true;
                }
            }
        }

        if (!criteria.getFaults().isEmpty()) {
            for (FaultCriteria fc : criteria.getFaults()) {
                if (fc.isExcluded() && fault != null && fault.equals(fc.getValue())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This method identifies whether the supplied criteria has exclusions.
     *
     * @param criteria The criteria
     * @return Whether it has exclusions
     */
    public static boolean hasExclusions(Criteria criteria) {
        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded()) {
                    return true;
                }
            }
        }

        if (!criteria.getFaults().isEmpty()) {
            for (FaultCriteria fc : criteria.getFaults()) {
                if (fc.isExcluded()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This method calculates the array position of a particular time, based on a start time
     * and interval.
     *
     * @param startTime
     * @param interval
     * @param time
     * @return The position
     */
    public static int getPosition(long startTime, long interval, long time) {
        return (int) ((time - startTime) / interval);
    }

    /**
     * This method calculates the base time associated with the index slot.
     *
     * @param startTime The start time
     * @param interval The interval
     * @param index The index
     * @return The base time
     */
    public static long getBaseTimestamp(long startTime, long interval, int index) {
        return (((int) (startTime / interval)) * interval) + (interval * index);
    }

}
