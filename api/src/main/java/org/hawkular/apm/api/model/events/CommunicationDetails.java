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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * This class represents communication details derived from two correlated trace
 * fragments.
 *
 * @author gbrown
 */
@Indexed
public class CommunicationDetails implements Externalizable {

    @Field
    private String id;

    private String businessTransaction;

    private String source;

    private String target;

    private boolean multiConsumer = false;

    private boolean internal = false;

    private long timestamp = 0;

    private long latency = 0;

    private long consumerDuration = 0;

    private long producerDuration = 0;

    private long timestampOffset = 0;

    private String sourceFragmentId;

    private String sourceHostName;

    private String sourceHostAddress;

    private String targetFragmentId;

    private String targetHostName;

    private String targetHostAddress;

    private long targetFragmentDuration;

    private String principal;

    private Map<String, String> properties = new HashMap<String, String>();

    private List<Outbound> outbound = new ArrayList<Outbound>();

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
     * This method returns the latency, between a producer and
     * consumer, in milliseconds.
     *
     * @return the latency
     */
    public long getLatency() {
        return latency;
    }

    /**
     * This method sets the latency, between a producer and
     * consumer, in milliseconds.
     *
     * @param latency the latency to set
     */
    public void setLatency(long latency) {
        this.latency = latency;
    }

    /**
     * @return the consumerDuration
     */
    public long getConsumerDuration() {
        return consumerDuration;
    }

    /**
     * @param consumerDuration the consumerDuration to set
     */
    public void setConsumerDuration(long consumerDuration) {
        this.consumerDuration = consumerDuration;
    }

    /**
     * @return the producerDuration
     */
    public long getProducerDuration() {
        return producerDuration;
    }

    /**
     * @param producerDuration the producerDuration to set
     */
    public void setProducerDuration(long producerDuration) {
        this.producerDuration = producerDuration;
    }

    /**
     * This method returns the timestamp offset, between the producer node
     * and the consumer node, in milliseconds.
     *
     * @return the timestampOffset
     */
    public long getTimestampOffset() {
        return timestampOffset;
    }

    /**
     * This method sets the timestamp offset, between the producer node
     * and the consumer node, in milliseconds.
     *
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CommunicationDetails [id=" + id + ", businessTransaction=" + businessTransaction + ", source="
                + source + ", target=" + target + ", multiConsumer=" + multiConsumer + ", internal=" + internal
                + ", timestamp=" + timestamp + ", latency=" + latency + ", consumerDuration=" + consumerDuration
                + ", producerDuration=" + producerDuration + ", timestampOffset=" + timestampOffset
                + ", sourceFragmentId=" + sourceFragmentId + ", sourceHostName=" + sourceHostName
                + ", sourceHostAddress=" + sourceHostAddress + ", targetFragmentId=" + targetFragmentId
                + ", targetHostName=" + targetHostName + ", targetHostAddress=" + targetHostAddress
                + ", targetFragmentDuration=" + targetFragmentDuration + ", principal=" + principal + ", properties="
                + properties + ", outbound=" + outbound + "]";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((businessTransaction == null) ? 0 : businessTransaction.hashCode());
        result = prime * result + (int) (consumerDuration ^ (consumerDuration >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (internal ? 1231 : 1237);
        result = prime * result + (int) (latency ^ (latency >>> 32));
        result = prime * result + (multiConsumer ? 1231 : 1237);
        result = prime * result + ((outbound == null) ? 0 : outbound.hashCode());
        result = prime * result + ((principal == null) ? 0 : principal.hashCode());
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
        if (multiConsumer != other.multiConsumer)
            return false;
        if (outbound == null) {
            if (other.outbound != null)
                return false;
        } else if (!outbound.equals(other.outbound))
            return false;
        if (principal == null) {
            if (other.principal != null)
                return false;
        } else if (!principal.equals(other.principal))
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
        return true;
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput ois) throws IOException, ClassNotFoundException {
        ois.readInt(); // Version

        id = ois.readUTF();
        businessTransaction = ois.readUTF();
        source = ois.readUTF();
        target = ois.readUTF();
        multiConsumer = ois.readBoolean();
        internal = ois.readBoolean();
        timestamp = ois.readLong();
        latency = ois.readLong();
        consumerDuration = ois.readLong();
        producerDuration = ois.readLong();
        timestampOffset = ois.readLong();
        sourceFragmentId = ois.readUTF();
        sourceHostName = ois.readUTF();
        sourceHostAddress = ois.readUTF();
        targetFragmentId = ois.readUTF();
        targetHostName = ois.readUTF();
        targetHostAddress = ois.readUTF();
        targetFragmentDuration = ois.readLong();
        principal = ois.readUTF();
        properties = (Map<String, String>) ois.readObject();    // TODO: Serialise properly

        int size = ois.readInt();
        for (int i = 0; i < size; i++) {
            outbound.add((Outbound) ois.readObject());
        }
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput oos) throws IOException {
        oos.writeInt(1); // Version

        oos.writeUTF(id);
        oos.writeUTF(businessTransaction);
        oos.writeUTF(source);
        oos.writeUTF(target);
        oos.writeBoolean(multiConsumer);
        oos.writeBoolean(internal);
        oos.writeLong(timestamp);
        oos.writeLong(latency);
        oos.writeLong(consumerDuration);
        oos.writeLong(producerDuration);
        oos.writeLong(timestampOffset);
        oos.writeUTF(sourceFragmentId);
        oos.writeUTF(sourceHostName);
        oos.writeUTF(sourceHostAddress);
        oos.writeUTF(targetFragmentId);
        oos.writeUTF(targetHostName);
        oos.writeUTF(targetHostAddress);
        oos.writeLong(targetFragmentDuration);
        oos.writeUTF(principal);
        oos.writeObject(properties);    // TODO: Serialise properly

        oos.writeInt(outbound.size());
        for (int i = 0; i < outbound.size(); i++) {
            oos.writeObject(outbound.get(i));
        }
    }

    /**
     * This class represents the outbound connectivity information associated with
     * the target fragment id. This can be used to build a complete end to end communication
     * map for the trace id.
     *
     * @author gbrown
     */
    public static class Outbound implements Externalizable {

        private List<String> ids = new ArrayList<String>();
        private boolean multiConsumer = false;
        private long producerOffset = 0;

        /**
         * @return the ids
         */
        public List<String> getIds() {
            return ids;
        }

        /**
         * @param ids the ids to set
         */
        public void setIds(List<String> ids) {
            this.ids = ids;
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

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((ids == null) ? 0 : ids.hashCode());
            result = prime * result + (multiConsumer ? 1231 : 1237);
            result = prime * result + (int) (producerOffset ^ (producerOffset >>> 32));
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
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
            if (ids == null) {
                if (other.ids != null) {
                    return false;
                }
            } else if (!ids.equals(other.ids)) {
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

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Outbound [ids=" + ids + ", multiConsumer=" + multiConsumer + ", producerOffset=" + producerOffset
                    + "]";
        }

        /* (non-Javadoc)
         * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
         */
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            in.readInt(); // Version

            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                ids.add(in.readUTF());
            }

            multiConsumer = in.readBoolean();
            producerOffset = in.readLong();
        }

        /* (non-Javadoc)
         * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
         */
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(1); // Version

            out.writeInt(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                out.writeUTF(ids.get(i));
            }

            out.writeBoolean(multiConsumer);
            out.writeLong(producerOffset);
        }

    }
}
