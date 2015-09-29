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
package org.hawkular.btm.api.model.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.NodeType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a response time.
 *
 * @author gbrown
 */
public class ResponseTime {

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
    private long duration = 0;

    @JsonInclude(Include.NON_NULL)
    private String fault;

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> properties = new HashMap<String, String>();

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> details = new HashMap<String, String>();

    @JsonInclude(Include.NON_EMPTY)
    private List<CorrelationIdentifier> correlationIds = new ArrayList<CorrelationIdentifier>();

    /**
     * @return the id
     */
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
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(long duration) {
        this.duration = duration;
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
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((businessTransaction == null) ? 0 : businessTransaction.hashCode());
        result = prime * result + ((correlationIds == null) ? 0 : correlationIds.hashCode());
        result = prime * result + ((details == null) ? 0 : details.hashCode());
        result = prime * result + (int) (duration ^ (duration >>> 32));
        result = prime * result + ((fault == null) ? 0 : fault.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
        ResponseTime other = (ResponseTime) obj;
        if (businessTransaction == null) {
            if (other.businessTransaction != null)
                return false;
        } else if (!businessTransaction.equals(other.businessTransaction))
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
        if (duration != other.duration)
            return false;
        if (fault == null) {
            if (other.fault != null)
                return false;
        } else if (!fault.equals(other.fault))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ResponseTime [id=" + id + ", businessTransaction=" + businessTransaction + ", type=" + type + ", uri="
                + uri + ", timestamp=" + timestamp + ", duration=" + duration + ", fault=" + fault + ", properties="
                + properties + ", details=" + details + ", correlationIds=" + correlationIds + "]";
    }

}
