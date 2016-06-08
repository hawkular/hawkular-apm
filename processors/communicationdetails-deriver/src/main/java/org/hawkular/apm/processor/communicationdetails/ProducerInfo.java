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
package org.hawkular.apm.processor.communicationdetails;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import org.hawkular.apm.api.model.Property;

/**
 * This class represents information cached about a producer, to enable it to be
 * correlated to a consumer.
 *
 * @author gbrown
 */
public class ProducerInfo implements Externalizable {

    private String sourceUri;

    private String sourceOperation;

    private long timestamp = 0;

    private long duration = 0;

    private String fragmentId;

    private String hostName;

    private String hostAddress;

    private boolean multipleConsumers = false;

    private Set<Property> properties = new HashSet<Property>();

    /**
     * @return the sourceUri
     */
    public String getSourceUri() {
        return sourceUri;
    }

    /**
     * @param sourceUri the sourceUri to set
     */
    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    /**
     * @return the sourceOperation
     */
    public String getSourceOperation() {
        return sourceOperation;
    }

    /**
     * @param sourceOperation the sourceOperation to set
     */
    public void setSourceOperation(String sourceOperation) {
        this.sourceOperation = sourceOperation;
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
     * @return the hostAddress
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param hostAddress the hostAddress to set
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * @return the multipleConsumers
     */
    public boolean isMultipleConsumers() {
        return multipleConsumers;
    }

    /**
     * @param multipleConsumers the multipleConsumers to set
     */
    public void setMultipleConsumers(boolean multipleConsumers) {
        this.multipleConsumers = multipleConsumers;
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

    /* (non-Javadoc)
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput ois) throws IOException, ClassNotFoundException {
        ois.readInt(); // Read version

        sourceUri = ois.readUTF();
        sourceOperation = ois.readUTF();
        timestamp = ois.readLong();
        duration = ois.readLong();
        fragmentId = ois.readUTF();
        hostName = ois.readUTF();
        hostAddress = ois.readUTF();
        multipleConsumers = ois.readBoolean();

        int size = ois.readInt();
        for (int i = 0; i < size; i++) {
            properties.add((Property) ois.readObject());
        }
    }

    /* (non-Javadoc)
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput oos) throws IOException {
        oos.writeInt(1); // Write version

        oos.writeUTF(sourceUri);
        oos.writeUTF(sourceOperation);
        oos.writeLong(timestamp);
        oos.writeLong(duration);
        oos.writeUTF(fragmentId);
        oos.writeUTF(hostName);
        oos.writeUTF(hostAddress);
        oos.writeBoolean(multipleConsumers);

        oos.writeInt(properties.size());
        for (Property property : properties) {
            oos.writeObject(property);
        }
    }

}
