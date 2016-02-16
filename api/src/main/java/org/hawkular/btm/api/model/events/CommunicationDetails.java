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
package org.hawkular.btm.api.model.events;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents communication details derived from two correlated business transaction
 * fragments.
 *
 * @author gbrown
 */
public class CommunicationDetails {

    private String id;

    private String businessTransaction;

    private String uri;

    private String originUri;

    private long timestamp = 0;

    private double latency = 0;

    private double consumerDuration = 0;

    private double producerDuration = 0;

    private long timestampOffset = 0;

    private String sourceFragmentId;

    private String sourceHostName;

    private String sourceHostAddress;

    private String targetFragmentId;

    private String targetHostName;

    private String targetHostAddress;

    private Map<String, String> properties = new HashMap<String, String>();

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
     * @return the originUri
     */
    public String getOriginUri() {
        return originUri;
    }

    /**
     * @param originUri the originUri to set
     */
    public void setOriginUri(String originUri) {
        this.originUri = originUri;
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
     * @return the latency
     */
    public double getLatency() {
        return latency;
    }

    /**
     * @param latency the latency to set
     */
    public void setLatency(double latency) {
        this.latency = latency;
    }

    /**
     * @return the consumerDuration
     */
    public double getConsumerDuration() {
        return consumerDuration;
    }

    /**
     * @param consumerDuration the consumerDuration to set
     */
    public void setConsumerDuration(double consumerDuration) {
        this.consumerDuration = consumerDuration;
    }

    /**
     * @return the producerDuration
     */
    public double getProducerDuration() {
        return producerDuration;
    }

    /**
     * @param producerDuration the producerDuration to set
     */
    public void setProducerDuration(double producerDuration) {
        this.producerDuration = producerDuration;
    }

    /**
     * @return the timestampOffset
     */
    public long getTimestampOffset() {
        return timestampOffset;
    }

    /**
     * @param timestampOffset the timestampOffset to set
     */
    public void setTimestampOffset(long timestampOffset) {
        this.timestampOffset = timestampOffset;
    }

    /**
     * @return the sourceFragmentId
     */
    public String getSourceFragmentId() {
        return sourceFragmentId;
    }

    /**
     * @param sourceFragmentId the sourceFragmentId to set
     */
    public void setSourceFragmentId(String sourceFragmentId) {
        this.sourceFragmentId = sourceFragmentId;
    }

    /**
     * @return the sourceHostName
     */
    public String getSourceHostName() {
        return sourceHostName;
    }

    /**
     * @param sourceHostName the sourceHostName to set
     */
    public void setSourceHostName(String sourceHostName) {
        this.sourceHostName = sourceHostName;
    }

    /**
     * @return the sourceHostAddress
     */
    public String getSourceHostAddress() {
        return sourceHostAddress;
    }

    /**
     * @param sourceHostAddress the sourceHostAddress to set
     */
    public void setSourceHostAddress(String sourceHostAddress) {
        this.sourceHostAddress = sourceHostAddress;
    }

    /**
     * @return the targetFragmentId
     */
    public String getTargetFragmentId() {
        return targetFragmentId;
    }

    /**
     * @param targetFragmentId the targetFragmentId to set
     */
    public void setTargetFragmentId(String targetFragmentId) {
        this.targetFragmentId = targetFragmentId;
    }

    /**
     * @return the targetHostName
     */
    public String getTargetHostName() {
        return targetHostName;
    }

    /**
     * @param targetHostName the targetHostName to set
     */
    public void setTargetHostName(String targetHostName) {
        this.targetHostName = targetHostName;
    }

    /**
     * @return the targetHostAddress
     */
    public String getTargetHostAddress() {
        return targetHostAddress;
    }

    /**
     * @param targetHostAddress the targetHostAddress to set
     */
    public void setTargetHostAddress(String targetHostAddress) {
        this.targetHostAddress = targetHostAddress;
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((businessTransaction == null) ? 0 : businessTransaction.hashCode());
        long temp;
        temp = Double.doubleToLongBits(consumerDuration);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        temp = Double.doubleToLongBits(latency);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((originUri == null) ? 0 : originUri.hashCode());
        temp = Double.doubleToLongBits(producerDuration);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((sourceFragmentId == null) ? 0 : sourceFragmentId.hashCode());
        result = prime * result + ((sourceHostAddress == null) ? 0 : sourceHostAddress.hashCode());
        result = prime * result + ((sourceHostName == null) ? 0 : sourceHostName.hashCode());
        result = prime * result + ((targetFragmentId == null) ? 0 : targetFragmentId.hashCode());
        result = prime * result + ((targetHostAddress == null) ? 0 : targetHostAddress.hashCode());
        result = prime * result + ((targetHostName == null) ? 0 : targetHostName.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + (int) (timestampOffset ^ (timestampOffset >>> 32));
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
        CommunicationDetails other = (CommunicationDetails) obj;
        if (businessTransaction == null) {
            if (other.businessTransaction != null)
                return false;
        } else if (!businessTransaction.equals(other.businessTransaction))
            return false;
        if (Double.doubleToLongBits(consumerDuration) != Double.doubleToLongBits(other.consumerDuration))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (Double.doubleToLongBits(latency) != Double.doubleToLongBits(other.latency))
            return false;
        if (originUri == null) {
            if (other.originUri != null)
                return false;
        } else if (!originUri.equals(other.originUri))
            return false;
        if (Double.doubleToLongBits(producerDuration) != Double.doubleToLongBits(other.producerDuration))
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (sourceFragmentId == null) {
            if (other.sourceFragmentId != null)
                return false;
        } else if (!sourceFragmentId.equals(other.sourceFragmentId))
            return false;
        if (sourceHostAddress == null) {
            if (other.sourceHostAddress != null)
                return false;
        } else if (!sourceHostAddress.equals(other.sourceHostAddress))
            return false;
        if (sourceHostName == null) {
            if (other.sourceHostName != null)
                return false;
        } else if (!sourceHostName.equals(other.sourceHostName))
            return false;
        if (targetFragmentId == null) {
            if (other.targetFragmentId != null)
                return false;
        } else if (!targetFragmentId.equals(other.targetFragmentId))
            return false;
        if (targetHostAddress == null) {
            if (other.targetHostAddress != null)
                return false;
        } else if (!targetHostAddress.equals(other.targetHostAddress))
            return false;
        if (targetHostName == null) {
            if (other.targetHostName != null)
                return false;
        } else if (!targetHostName.equals(other.targetHostName))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (timestampOffset != other.timestampOffset)
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
        return "CommunicationDetails [id=" + id + ", businessTransaction=" + businessTransaction + ", uri=" + uri
                + ", originUri=" + originUri + ", timestamp=" + timestamp + ", latency=" + latency
                + ", consumerDuration=" + consumerDuration + ", producerDuration=" + producerDuration
                + ", timestampOffset=" + timestampOffset + ", sourceFragmentId=" + sourceFragmentId
                + ", sourceHostName=" + sourceHostName + ", sourceHostAddress=" + sourceHostAddress
                + ", targetFragmentId=" + targetFragmentId + ", targetHostName=" + targetHostName
                + ", targetHostAddress=" + targetHostAddress + ", properties=" + properties + "]";
    }

}
