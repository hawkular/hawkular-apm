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
package org.hawkular.btm.api.model.btxn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents the top level business transaction instance
 * description. This model can be used to represent either a fragment
 * of a business transaction instance, captured from one of multiple
 * resources executing the transaction, or a complete end to end correlated
 * description of the flow.
 *
 * @author gbrown
 *
 */
public class BusinessTransaction {

    @JsonInclude
    private String id;

    @JsonInclude(Include.NON_EMPTY)
    private String name;

    @JsonInclude
    private List<Node> nodes = new ArrayList<Node>();

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> properties = new HashMap<String, String>();

    public BusinessTransaction() {
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @return The business transaction
     */
    public BusinessTransaction setId(String id) {
        this.id = id;
        return this;
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
     * @return the nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     * @return The business transaction
     */
    public BusinessTransaction setNodes(List<Node> nodes) {
        this.nodes = nodes;
        return this;
    }

    /**
     * This method returns properties that can be used to search for
     * the business transaction.
     *
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     * @return The business transaction
     */
    public BusinessTransaction setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * This method returns the start time of the business
     * transaction.
     *
     * @return The start time, or 0 if no time defined
     */
    public long startTime() {
        long ret = 0;

        if (getNodes().size() > 0) {
            ret = getNodes().get(0).getStartTime();
        }

        return ret;
    }

    /**
     * This method returns the end time of the business
     * transaction.
     *
     * @return The end time, or 0 if no time defined
     */
    public long endTime() {
        long ret = 0;

        for (Node node : getNodes()) {
            long endTime = node.overallEndTime();

            if (endTime > ret) {
                ret = endTime;
            }
        }

        return ret;
    }

    /**
     * This method locates any node within the business transaction that
     * is associated with the supplied correlation id.
     *
     * @param cid The correlation identifier
     * @param baseTime The base time associated with the correlation identifier
     * @return The nodes that were correlated with the supplied correlation identifier
     *                      (and base time if relevant)
     */
    public Set<Node> getCorrelatedNodes(CorrelationIdentifier cid, long baseTime) {
        Set<Node> ret = new HashSet<Node>();

        for (Node n : getNodes()) {
            n.findCorrelatedNodes(cid, baseTime, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BusinessTransaction other = (BusinessTransaction) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nodes == null) {
            if (other.nodes != null)
                return false;
        } else if (!nodes.equals(other.nodes))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BusinessTransaction [id=" + id + ", name=" + name + ", nodes=" + nodes + ", properties=" + properties
                + "]";
    }

}
