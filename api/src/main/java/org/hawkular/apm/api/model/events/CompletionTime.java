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

import java.util.HashSet;
import java.util.Set;

import org.hawkular.apm.api.model.Property;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a completion time.
 *
 * @author gbrown
 */
public class CompletionTime implements ApmEvent {

    @JsonInclude
    private String id;

    @JsonInclude
    private String uri;

    @JsonInclude
    private String operation;

    @JsonInclude
    private String endpointType;

    @JsonInclude
    private String transaction;

    /**
     * Timestamp in microseconds
     */
    @JsonInclude
    private long timestamp = 0;

    /**
     * Duration in microseconds
     */
    @JsonInclude
    private long duration = 0;

    @JsonInclude(Include.NON_NULL)
    private String hostName;

    @JsonInclude(Include.NON_NULL)
    private String hostAddress;

    @JsonInclude(Include.NON_EMPTY)
    private Set<Property> properties = new HashSet<Property>();

    @JsonInclude
    private boolean internal = false;

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
     * @return the endpointType
     */
    public String getEndpointType() {
        return endpointType;
    }

    /**
     * @param endpointType the endpointType to set
     */
    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
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
     * @return the timestamp in microseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp in microseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the duration in microseconds
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @param duration the duration in microseconds
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * This method sets the hostname, where the completion time is associated
     * with a fragment.
     *
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
     * @return the internal
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * @param internal the internal to set
     */
    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    @Override
    public String toString() {
        return "CompletionTime [id=" + id + ", uri=" + uri + ", operation=" + operation + ", endpointType="
                + endpointType + ", transaction=" + transaction + ", timestamp=" + timestamp
                + ", duration=" + duration + ", hostName=" + hostName + ", hostAddress=" + hostAddress
                + ", properties=" + properties + ", internal=" + internal + "]";
    }

}
