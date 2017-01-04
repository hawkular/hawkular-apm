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
    private String traceId;

    @JsonInclude
    private String fragmentId;

    /**
     * Timestamp in microseconds
     */
    @JsonInclude
    private long timestamp;

    @JsonInclude(Include.NON_EMPTY)
    private String transaction;

    @JsonInclude(Include.NON_EMPTY)
    private String hostName;

    @JsonInclude(Include.NON_EMPTY)
    private String hostAddress;

    @JsonInclude
    private List<Node> nodes = new ArrayList<Node>();

    public Trace() {
    }

    /**
     * @return the traceId
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * This method sets the trace id. When the fragment and trace id
     * are the same value, it means the fragment is the initial fragment
     * for the trace instance.
     *
     * @param traceId the traceId to set
     * @return The trace
     */
    public Trace setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * @return the fragmentId
     */
    public String getFragmentId() {
        return fragmentId;
    }

    /**
     * This method sets the fragment id. When the fragment and trace id
     * are the same value, it means the fragment is the initial fragment
     * for the trace instance.
     *
     * @param fragmentId the fragmentId to set
     * @return The trace
     */
    public Trace setFragmentId(String fragmentId) {
        this.fragmentId = fragmentId;
        return this;
    }

    /**
     * @return the timestamp in microseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp in microseconds
     * @return The trace
     */
    public Trace setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * @return the transaction name
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * @param name the transaction name to set
     * @return The trace
     */
    public Trace setTransaction(String name) {
        this.transaction = name;
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
        return getTraceId().equals(getFragmentId());
    }

    /**
     * This method returns the end time of the trace fragment.
     *
     * @return The end time (in microseconds)
     */
    public long endTime() {
        return getTimestamp() + calculateDuration();
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

            return endTime - getNodes().get(0).getTimestamp();
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
        result = prime * result + ((transaction == null) ? 0 : transaction.hashCode());
        result = prime * result + ((fragmentId == null) ? 0 : fragmentId.hashCode());
        result = prime * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((traceId == null) ? 0 : traceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Trace other = (Trace) obj;
        if (transaction == null) {
            if (other.transaction != null)
                return false;
        } else if (!transaction.equals(other.transaction))
            return false;
        if (fragmentId == null) {
            if (other.fragmentId != null)
                return false;
        } else if (!fragmentId.equals(other.fragmentId))
            return false;
        if (hostAddress == null) {
            if (other.hostAddress != null)
                return false;
        } else if (!hostAddress.equals(other.hostAddress))
            return false;
        if (hostName == null) {
            if (other.hostName != null)
                return false;
        } else if (!hostName.equals(other.hostName))
            return false;
        if (nodes == null) {
            if (other.nodes != null)
                return false;
        } else if (!nodes.equals(other.nodes))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (traceId == null) {
            if (other.traceId != null)
                return false;
        } else if (!traceId.equals(other.traceId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Trace [traceId=" + traceId + ", fragmentId=" + fragmentId + ", timestamp=" + timestamp
                + ", transaction=" + transaction + ", hostName=" + hostName + ", hostAddress="
                + hostAddress + ", nodes=" + nodes + "]";
    }

}
