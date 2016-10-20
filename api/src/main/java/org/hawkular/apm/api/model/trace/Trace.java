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
package org.hawkular.apm.api.model.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.Property;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents the top level trace instance
 * description. This model can be used to represent either a fragment
 * of a trace instance, captured from one of multiple
 * resources executing the transaction, or a complete end to end correlated
 * description of the flow.
 *
 * @author gbrown
 *
 */
public class Trace {

    // NOTE: If any new fields are added to the Trace class, then the TraceServiceElasticsearch
    // class will need to be updated to include the field in the custom serializer/deserializer
    // implementations. These custom implementations are required to promote the node 'Property'
    // objects to the top level, to enable them to be queried.

    @JsonInclude
    private String id;

    /**
     * Start time in microseconds
     */
    @JsonInclude
    private long startTime;

    @JsonInclude(Include.NON_EMPTY)
    private String businessTransaction;

    @JsonInclude(Include.NON_EMPTY)
    private String principal;

    @JsonInclude(Include.NON_EMPTY)
    private String hostName;

    @JsonInclude(Include.NON_EMPTY)
    private String hostAddress;

    @JsonInclude
    private List<Node> nodes = new ArrayList<Node>();

    public Trace() {
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @return The trace
     */
    public Trace setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return the startTime in microseconds
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime in microseconds
     * @return The trace
     */
    public Trace setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * @return the business transaction name
     */
    public String getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * @param name the business transaction name to set
     * @return The trace
     */
    public Trace setBusinessTransaction(String name) {
        this.businessTransaction = name;
        return this;
    }

    /**
     * @return the principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * @param principal the principal to set
     * @return The trace
     */
    public Trace setPrincipal(String principal) {
        this.principal = principal;
        return this;
    }

    /**
     * @return the host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the host name to set
     * @return The trace
     */
    public Trace setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    /**
     * @return the host address
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param hostAddress the host address to set
     * @return The trace
     */
    public Trace setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
        return this;
    }

    /**
     * @return the nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     * @return The trace
     */
    public Trace setNodes(List<Node> nodes) {
        this.nodes = nodes;
        return this;
    }

    /**
     * This method returns all properties contained in the node hierarchy
     * that can be used to search for the trace.
     *
     * @return Aggregated list of all the properties in the node hierarchy
     */
    public Set<Property> allProperties() {
        Set<Property> properties = new HashSet<Property>();
        for (Node n : nodes) {
            n.includeProperties(properties);
        }
        return Collections.unmodifiableSet(properties);
    }

    /**
     * This method determines whether there is atleast one
     * property with the supplied name.
     *
     * @param name The property name
     * @return Whether a property of the supplied name is defined
     */
    public boolean hasProperty(String name) {
        for (Property property : allProperties()) {
            if (property.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method returns the set of properties having the
     * supplied property name.
     *
     * @param name The property name
     * @return The set of properties with the supplied name
     */
    public Set<Property> getProperties(String name) {
        Set<Property> ret = new HashSet<Property>();
        for (Property property : allProperties()) {
            if (property.getName().equals(name)) {
                ret.add(property);
            }
        }
        return ret;
    }

    /**
     * This method determines whether this trace is the initial
     * fragment of an instance.
     *
     * @return Whether this is the initial fragment
     */
    public boolean initialFragment() {
        // Initial fragment, if the first node has no correlation ids
        return !getNodes().isEmpty() && getNodes().get(0).getCorrelationIds().isEmpty();
    }

    /**
     * This method returns the end time of the trace fragment.
     *
     * @return The end time (in microseconds)
     */
    public long endTime() {
        return getStartTime() + calculateDuration();
    }

    /**
     * This method returns the duration of the trace fragment.
     *
     * @return The duration (in microseconds), or 0 if no nodes defined
     */
    public long calculateDuration() {
        if (!nodes.isEmpty()) {
            long endTime = 0;

            for (int i = 0; i < getNodes().size(); i++) {
                Node node = getNodes().get(i);
                long nodeEndTime = node.overallEndTime();

                if (nodeEndTime > endTime) {
                    endTime = nodeEndTime;
                }
            }

            return endTime - getNodes().get(0).getBaseTime();
        }

        return 0L;
    }

    /**
     * This method locates any node within the trace that
     * is associated with the supplied correlation id.
     *
     * @param cid The correlation identifier
     * @return The nodes that were correlated with the supplied correlation identifier
     */
    public Set<Node> getCorrelatedNodes(CorrelationIdentifier cid) {
        Set<Node> ret = new HashSet<Node>();

        for (Node n : getNodes()) {
            n.findCorrelatedNodes(cid, ret);
        }

        return ret;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((businessTransaction == null) ? 0 : businessTransaction.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        result = prime * result + ((principal == null) ? 0 : principal.hashCode());
        result = prime * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Trace other = (Trace) obj;
        if (hostAddress == null) {
            if (other.hostAddress != null) {
                return false;
            }
        } else if (!hostAddress.equals(other.hostAddress)) {
            return false;
        }
        if (hostName == null) {
            if (other.hostName != null) {
                return false;
            }
        } else if (!hostName.equals(other.hostName)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (businessTransaction == null) {
            if (other.businessTransaction != null) {
                return false;
            }
        } else if (!businessTransaction.equals(other.businessTransaction)) {
            return false;
        }
        if (nodes == null) {
            if (other.nodes != null) {
                return false;
            }
        } else if (!nodes.equals(other.nodes)) {
            return false;
        }
        if (principal == null) {
            if (other.principal != null) {
                return false;
            }
        } else if (!principal.equals(other.principal)) {
            return false;
        }
        if (startTime != other.startTime) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Trace [id=" + id + ", startTime=" + startTime
                + ", businessTransaction=" + businessTransaction + ", principal="
                + principal + ", hostName=" + hostName + ", hostAddress=" + hostAddress + ", nodes=" + nodes + "]";
    }

}
