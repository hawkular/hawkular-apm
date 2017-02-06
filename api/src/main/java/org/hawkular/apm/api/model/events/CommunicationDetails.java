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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.Property;

/**
 * This class represents communication details derived from two correlated trace
 * fragments.
 *
 * @author gbrown
 */
public class CommunicationDetails implements Serializable, ApmEvent {

    private static final long serialVersionUID = 1L;

    private String id;

    private String linkId;

    private String transaction;

    private String source;

    private String target;

    private boolean multiConsumer = false;

    private boolean internal = false;

    /**
     * Timestamp in microseconds
     */
    private long timestamp = 0;

    /**
     * Network latency in microseconds
     */
    private long latency = 0;

    /**
     * Consumer duration in microseconds
     */
    private long consumerDuration = 0;

    /**
     * Producer duration in microseconds
     */
    private long producerDuration = 0;

    /**
     * Timestamp offset in microseconds
     */
    private long timestampOffset = 0;

    private String traceId;

    private String sourceFragmentId;

    private String sourceHostName;

    private String sourceHostAddress;

    private String targetFragmentId;

    private String targetHostName;

    private String targetHostAddress;

    private long targetFragmentDuration;

    private Set<Property> properties = new HashSet<Property>();

    private List<Outbound> outbound = new ArrayList<Outbound>();

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
     * @return the linkId
     */
    public String getLinkId() {
        return linkId;
    }

    /**
     * @param linkId the linkId to set
     */
    public void setLinkId(String linkId) {
        this.linkId = linkId;
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
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * @return the multiConsumer
     */
    public boolean isMultiConsumer() {
        return multiConsumer;
    }

    /**
     * @param multiConsumer the multiConsumer to set
     */
    public void setMultiConsumer(boolean multiConsumer) {
        this.multiConsumer = multiConsumer;
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
     * This method returns the latency, between a producer and a consumer.
     *
     * @return the latency in microseconds
     */
    public long getLatency() {
        return latency;
    }

    /**
     * This method sets the latency, between a producer and a consumer.
     *
     * @param latency the latency in microseconds
     */
    public void setLatency(long latency) {
        this.latency = latency;
    }

    /**
     * @return the consumer duration in microseconds
     */
    public long getConsumerDuration() {
        return consumerDuration;
    }

    /**
     * @param consumerDuration the consumer duration in microseconds
     */
    public void setTargetDuration(long consumerDuration) {
        this.consumerDuration = consumerDuration;
    }

    /**
     * @return the producer duration in microseconds
     */
    public long getProducerDuration() {
        return producerDuration;
    }

    /**
     * @param producerDuration the producer duration in microseconds
     */
    public void setSourceDuration(long producerDuration) {
        this.producerDuration = producerDuration;
    }

    /**
     * This method returns the timestamp offset, between the producer node and the consumer node.
     *
     * @return the timestamp offset in microseconds
     */
    public long getTimestampOffset() {
        return timestampOffset;
    }

    /**
     * This method sets the timestamp offset, between the producer node and the consumer node.
     *
     * @param timestampOffset the timestamp offset in microseconds
     */
    public void setTimestampOffset(long timestampOffset) {
        this.timestampOffset = timestampOffset;
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
     * @return the targetFragmentDuration
     */
    public long getTargetFragmentDuration() {
        return targetFragmentDuration;
    }

    /**
     * @param targetFragmentDuration the targetFragmentDuration to set
     */
    public void setTargetFragmentDuration(long targetFragmentDuration) {
        this.targetFragmentDuration = targetFragmentDuration;
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
     * @return the outbound
     */
    public List<Outbound> getOutbound() {
        return outbound;
    }

    /**
     * @param outbound the outbound to set
     */
    public void setOutbound(List<Outbound> outbound) {
        this.outbound = outbound;
    }

    @Override
    public String toString() {
        return "CommunicationDetails [id=" + id + ", linkId=" + linkId + ", transaction=" + transaction
                + ", source=" + source + ", target=" + target + ", multiConsumer=" + multiConsumer + ", internal="
                + internal + ", timestamp=" + timestamp + ", latency=" + latency + ", consumerDuration="
                + consumerDuration + ", producerDuration=" + producerDuration + ", timestampOffset=" + timestampOffset
                + ", traceId=" + traceId + ", sourceFragmentId=" + sourceFragmentId + ", sourceHostName="
                + sourceHostName + ", sourceHostAddress=" + sourceHostAddress + ", targetFragmentId="
                + targetFragmentId + ", targetHostName=" + targetHostName + ", targetHostAddress=" + targetHostAddress
                + ", targetFragmentDuration=" + targetFragmentDuration + ", properties=" + properties + ", outbound="
                + outbound + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((transaction == null) ? 0 : transaction.hashCode());
        result = prime * result + (int) (consumerDuration ^ (consumerDuration >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (internal ? 1231 : 1237);
        result = prime * result + (int) (latency ^ (latency >>> 32));
        result = prime * result + ((linkId == null) ? 0 : linkId.hashCode());
        result = prime * result + (multiConsumer ? 1231 : 1237);
        result = prime * result + ((outbound == null) ? 0 : outbound.hashCode());
        result = prime * result + (int) (producerDuration ^ (producerDuration >>> 32));
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((sourceFragmentId == null) ? 0 : sourceFragmentId.hashCode());
        result = prime * result + ((sourceHostAddress == null) ? 0 : sourceHostAddress.hashCode());
        result = prime * result + ((sourceHostName == null) ? 0 : sourceHostName.hashCode());
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + (int) (targetFragmentDuration ^ (targetFragmentDuration >>> 32));
        result = prime * result + ((targetFragmentId == null) ? 0 : targetFragmentId.hashCode());
        result = prime * result + ((targetHostAddress == null) ? 0 : targetHostAddress.hashCode());
        result = prime * result + ((targetHostName == null) ? 0 : targetHostName.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + (int) (timestampOffset ^ (timestampOffset >>> 32));
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
        CommunicationDetails other = (CommunicationDetails) obj;
        if (transaction == null) {
            if (other.transaction != null)
                return false;
        } else if (!transaction.equals(other.transaction))
            return false;
        if (consumerDuration != other.consumerDuration)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (internal != other.internal)
            return false;
        if (latency != other.latency)
            return false;
        if (linkId == null) {
            if (other.linkId != null)
                return false;
        } else if (!linkId.equals(other.linkId))
            return false;
        if (multiConsumer != other.multiConsumer)
            return false;
        if (outbound == null) {
            if (other.outbound != null)
                return false;
        } else if (!outbound.equals(other.outbound))
            return false;
        if (producerDuration != other.producerDuration)
            return false;
        if (properties == null) {
            if (other.properties != null)
                return false;
        } else if (!properties.equals(other.properties))
            return false;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
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
        if (target == null) {
            if (other.target != null)
                return false;
        } else if (!target.equals(other.target))
            return false;
        if (targetFragmentDuration != other.targetFragmentDuration)
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
        if (traceId == null) {
            if (other.traceId != null)
                return false;
        } else if (!traceId.equals(other.traceId))
            return false;
        return true;
    }

    /**
     * This class represents the outbound connectivity information associated with
     * the target fragment id. This can be used to build a complete end to end communication
     * map for the trace id.
     *
     * @author gbrown
     */
    public static class Outbound implements Serializable {

        private static final long serialVersionUID = 1L;

        private List<String> linkIds = new ArrayList<String>();
        private boolean multiConsumer = false;
        private long producerOffset = 0;

        /**
         * @return the link ids
         */
        public List<String> getLinkIds() {
            return linkIds;
        }

        /**
         * @param linkIds the linkIds to set
         */
        public void setLinkIds(List<String> linkIds) {
            this.linkIds = linkIds;
        }

        /**
         * @return the multiConsumer
         */
        public boolean isMultiConsumer() {
            return multiConsumer;
        }

        /**
         * @param multiConsumer the multiConsumer to set
         */
        public void setMultiConsumer(boolean multiConsumer) {
            this.multiConsumer = multiConsumer;
        }

        /**
         * @return the producerOffset
         */
        public long getProducerOffset() {
            return producerOffset;
        }

        /**
         * @param producerOffset the producerOffset to set
         */
        public void setProducerOffset(long producerOffset) {
            this.producerOffset = producerOffset;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((linkIds == null) ? 0 : linkIds.hashCode());
            result = prime * result + (multiConsumer ? 1231 : 1237);
            result = prime * result + (int) (producerOffset ^ (producerOffset >>> 32));
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
            Outbound other = (Outbound) obj;
            if (linkIds == null) {
                if (other.linkIds != null) {
                    return false;
                }
            } else if (!linkIds.equals(other.linkIds)) {
                return false;
            }
            if (multiConsumer != other.multiConsumer) {
                return false;
            }
            if (producerOffset != other.producerOffset) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Outbound [linkIds=" + linkIds + ", multiConsumer=" + multiConsumer + ", producerOffset=" + producerOffset
                    + "]";
        }

    }
}
