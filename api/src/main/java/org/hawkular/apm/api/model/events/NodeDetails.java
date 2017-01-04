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
package org.hawkular.apm.api.model.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.NodeType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents node details derived from a trace
 * fragment.
 *
 * @author gbrown
 */
public class NodeDetails implements ApmEvent {

    @JsonInclude
    private String id;

    @JsonInclude
    private String traceId;

    @JsonInclude
    private String fragmentId;

    /**
     * This property indicates whether this is the initial node within an application
     * or invoked service. This enables the node details associated with handling a
     * service request (or initial top level application component) to be identified.
     */
    @JsonInclude(Include.NON_DEFAULT)
    private boolean initial = false;

    @JsonInclude
    private String transaction;

    @JsonInclude
    private NodeType type;

    @JsonInclude
    private String uri;

    /**
     * Timestamp in microseconds
     */
    @JsonInclude
    private long timestamp = 0;

    /**
     * Elapsed time in microseconds
     */
    @JsonInclude
    private long elapsed = 0;

    /**
     * Actual time in microseconds
     */
    @JsonInclude
    private long actual = 0;

    @JsonInclude(Include.NON_NULL)
    private String componentType;

    @JsonInclude(Include.NON_NULL)
    private String operation;

    @JsonInclude(Include.NON_NULL)
    private String hostName;

    @JsonInclude(Include.NON_NULL)
    private String hostAddress;

    @JsonInclude(Include.NON_EMPTY)
    private Set<Property> properties = new HashSet<Property>();

    @JsonInclude(Include.NON_EMPTY)
    private List<CorrelationIdentifier> correlationIds = new ArrayList<CorrelationIdentifier>();

    /**
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the traceId
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * @param traceId the traceId to set
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * @return the fragmentId
     */
    public String getFragmentId() {
        return fragmentId;
    }

    /**
     * @param fragmentId the fragmentId to set
     */
    public void setFragmentId(String fragmentId) {
        this.fragmentId = fragmentId;
    }

    /**
     * @return the initial
     */
    public boolean isInitial() {
        return initial;
    }

    /**
     * @param initial the initial to set
     */
    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    /**
     * @return the transaction
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * @param transaction the transaction to set
     */
    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    /**
     * @return the type
     */
    public NodeType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(NodeType type) {
        this.type = type;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the elapsed
     */
    public long getElapsed() {
        return elapsed;
    }

    /**
     * This method sets the elapsed time
     *
     * @param elapsed the elapsed time in microseconds
     */
    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    /**
     * @return the actual time in microseconds
     */
    public long getActual() {
        return actual;
    }

    /**
     * This method sets the actual duration
     *
     * @param actual the actual duration in microseconds
     */
    public void setActual(long actual) {
        this.actual = actual;
    }

    /**
     * @return the componentType
     */
    public String getComponentType() {
        return componentType;
    }

    /**
     * @param componentType the componentType to set
     */
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the hostName to set
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Get host address (e.g. ipv4).
     *
     * @return the host address
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * Set host address (e.g. ipv4).
     *
     * @param hostAddress the host address
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * @return the properties
     */
    public Set<Property> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Set<Property> properties) {
        this.properties = properties;
    }

    /**
     * This method determines whether there is atleast one
     * property with the supplied name.
     *
     * @param name The property name
     * @return Whether a property of the supplied name is defined
     */
    public boolean hasProperty(String name) {
        for (Property property : this.properties) {
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
        for (Property property : this.properties) {
            if (property.getName().equals(name)) {
                ret.add(property);
            }
        }
        return ret;
    }

    /**
     * @return the correlationIds
     */
    public List<CorrelationIdentifier> getCorrelationIds() {
        return correlationIds;
    }

    /**
     * @param correlationIds the correlationIds to set
     */
    public void setCorrelationIds(List<CorrelationIdentifier> correlationIds) {
        this.correlationIds = correlationIds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (actual ^ (actual >>> 32));
        result = prime * result + ((componentType == null) ? 0 : componentType.hashCode());
        result = prime * result + ((correlationIds == null) ? 0 : correlationIds.hashCode());
        result = prime * result + (int) (elapsed ^ (elapsed >>> 32));
        result = prime * result + ((fragmentId == null) ? 0 : fragmentId.hashCode());
        result = prime * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (initial ? 1231 : 1237);
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((traceId == null) ? 0 : traceId.hashCode());
        result = prime * result + ((transaction == null) ? 0 : transaction.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
        NodeDetails other = (NodeDetails) obj;
        if (actual != other.actual)
            return false;
        if (componentType == null) {
            if (other.componentType != null)
                return false;
        } else if (!componentType.equals(other.componentType))
            return false;
        if (correlationIds == null) {
            if (other.correlationIds != null)
                return false;
        } else if (!correlationIds.equals(other.correlationIds))
            return false;
        if (elapsed != other.elapsed)
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
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (initial != other.initial)
            return false;
        if (operation == null) {
            if (other.operation != null)
                return false;
        } else if (!operation.equals(other.operation))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (traceId == null) {
            if (other.traceId != null)
                return false;
        } else if (!traceId.equals(other.traceId))
            return false;
        if (transaction == null) {
            if (other.transaction != null)
                return false;
        } else if (!transaction.equals(other.transaction))
            return false;
        if (type != other.type)
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "NodeDetails [id=" + id + ", traceId=" + traceId + ", fragmentId=" + fragmentId + ", initial=" + initial
                + ", transaction=" + transaction + ", type=" + type + ", uri=" + uri + ", timestamp=" + timestamp
                + ", elapsed=" + elapsed + ", actual=" + actual + ", componentType=" + componentType + ", operation="
                + operation + ", hostName=" + hostName + ", hostAddress=" + hostAddress + ", properties=" + properties
                + ", correlationIds=" + correlationIds + "]";
    }

}
