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
package org.hawkular.apm.api.model.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private String businessTransaction;

    @JsonInclude
    private NodeType type;

    @JsonInclude
    private String uri;

    @JsonInclude
    private long timestamp = 0;

    @JsonInclude
    private long elapsed = 0;

    @JsonInclude
    private long actual = 0;

    @JsonInclude(Include.NON_NULL)
    private String componentType;

    @JsonInclude(Include.NON_NULL)
    private String operation;

    @JsonInclude(Include.NON_NULL)
    private String fault;

    @JsonInclude(Include.NON_NULL)
    private String hostName;

    @JsonInclude(Include.NON_NULL)
    private String hostAddress;

    @JsonInclude(Include.NON_NULL)
    private String principal;

    @JsonInclude(Include.NON_EMPTY)
    private Set<Property> properties = new HashSet<Property>();

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> details = new HashMap<String, String>();

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
     * @return the businessTransaction
     */
    public String getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * @param businessTransaction the businessTransaction to set
     */
    public void setBusinessTransaction(String businessTransaction) {
        this.businessTransaction = businessTransaction;
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
     * This method sets the elapsed duration, in nanoseconds.
     *
     * @param elapsed the elapsed to set
     */
    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    /**
     * @return the actual
     */
    public long getActual() {
        return actual;
    }

    /**
     * This method sets the actual duration, in nanoseconds.
     *
     * @param actual the actual to set
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
     * @return the fault
     */
    public String getFault() {
        return fault;
    }

    /**
     * @param fault the fault to set
     */
    public void setFault(String fault) {
        this.fault = fault;
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
     * @return the principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * @param principal the principal to set
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
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
     * @return the details
     */
    public Map<String, String> getDetails() {
        return details;
    }

    /**
     * @param details the details to set
     */
    public void setDetails(Map<String, String> details) {
        this.details = details;
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
        result = prime * result + ((businessTransaction == null) ? 0 : businessTransaction.hashCode());
        result = prime * result + ((componentType == null) ? 0 : componentType.hashCode());
        result = prime * result + ((correlationIds == null) ? 0 : correlationIds.hashCode());
        result = prime * result + ((details == null) ? 0 : details.hashCode());
        result = prime * result + (int) (elapsed ^ (elapsed >>> 32));
        result = prime * result + ((fault == null) ? 0 : fault.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((principal == null) ? 0 : principal.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
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
        if (businessTransaction == null) {
            if (other.businessTransaction != null)
                return false;
        } else if (!businessTransaction.equals(other.businessTransaction))
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
        if (details == null) {
            if (other.details != null)
                return false;
        } else if (!details.equals(other.details))
            return false;
        if (elapsed != other.elapsed)
            return false;
        if (fault == null) {
            if (other.fault != null)
                return false;
        } else if (!fault.equals(other.fault))
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
        if (operation == null) {
            if (other.operation != null)
                return false;
        } else if (!operation.equals(other.operation))
            return false;
        if (principal == null) {
            if (other.principal != null)
                return false;
        } else if (!principal.equals(other.principal))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (timestamp != other.timestamp)
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
        return "NodeDetails [id=" + id + ", businessTransaction=" + businessTransaction + ", type=" + type + ", uri="
                + uri + ", timestamp=" + timestamp + ", elapsed=" + elapsed + ", actual=" + actual + ", componentType="
                + componentType + ", operation=" + operation + ", fault=" + fault + ", hostName=" + hostName
                + ", principal=" + principal + ", properties=" + properties + ", details=" + details
                + ", correlationIds=" + correlationIds + "]";
    }

}
