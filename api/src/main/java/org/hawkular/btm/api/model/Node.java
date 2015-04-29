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
package org.hawkular.btm.api.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * This abstract class is the base for all nodes describing a business transaction
 * instance flow.
 *
 * @author gbrown
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = Consumer.class),
    @Type(value = Producer.class),
    @Type(value = Service.class),
    @Type(value = Component.class) })
@ApiModel(subTypes = {Consumer.class, Producer.class, Service.class, Component.class}, discriminator = "type")
public abstract class Node {

    @JsonInclude
    private long startTime = 0;

    @JsonInclude
    private long duration = 0;

    @JsonInclude(Include.NON_EMPTY)
    private Set<CorrelationIdentifier> correlationIds = new HashSet<CorrelationIdentifier>();

    public Node() {
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
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
     * @return the correlationIds
     */
    public Set<CorrelationIdentifier> getCorrelationIds() {
        return correlationIds;
    }

    /**
     * @param correlationIds the correlationIds to set
     */
    public void setCorrelationIds(Set<CorrelationIdentifier> correlationIds) {
        this.correlationIds = correlationIds;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((correlationIds == null) ? 0 : correlationIds.hashCode());
        result = prime * result + (int) (duration ^ (duration >>> 32));
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
        Node other = (Node) obj;
        if (correlationIds == null) {
            if (other.correlationIds != null) {
                return false;
            }
        } else if (!correlationIds.equals(other.correlationIds)) {
            return false;
        }
        if (duration != other.duration) {
            return false;
        }
        if (startTime != other.startTime) {
            return false;
        }
        return true;
    }

}
