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
package org.hawkular.btm.api.model.btxn;

import java.util.HashSet;
import java.util.Set;

import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;

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
@ApiModel(subTypes = { Consumer.class, Producer.class, Service.class, Component.class }, discriminator = "type")
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

    /**
     * This method adds an interaction correlation id.
     *
     * @param id The id
     */
    public void addInteractionId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Interaction, id));
    }

    /**
     * This method adds a global correlation id.
     *
     * @param id The id
     */
    public void addGlobalId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Global, id));
    }

    /**
     * This method adds a local correlation id.
     *
     * @param id The id
     */
    public void addLocalId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Local, id));
    }

    /**
     * This method calculates the end time of this node based on the
     * start time and duration. An end time will only be returned if
     * the start time has been set.
     *
     * @return The end time, based on start time and duration, or 0 if
     *                  not known
     */
    public long endTime() {
        long ret = 0;

        if (startTime > 0) {
            ret = startTime + duration;
        }

        return ret;
    }

    /**
     * This method determines the overall end time of this node.
     *
     * @return The overall end time
     */
    protected long overallEndTime() {
        return endTime();
    }

    /**
     * This method identifies all of the nodes within a business transaction that
     * are associated with the supplied correlation identifier.
     *
     * @param cid The correlation identifier
     * @param baseTime The base time at which the correlation is being evaluated
     * @param nodes The set of nodes that are associated with the correlation identifier
     */
    protected void findCorrelatedNodes(CorrelationIdentifier cid, long baseTime, Set<Node> nodes) {
        if (isCorrelated(cid, baseTime)) {
            nodes.add(this);
        }
    }

    /**
     * This method determines whether the node is correlated to the supplied
     * identifier.
     *
     * @param cid The correlation id
     * @param baseTime The base time at which the correlation is being evaluated
     * @return Whether the node is correlated to the supplied id
     */
    protected boolean isCorrelated(CorrelationIdentifier cid, long baseTime) {
        for (CorrelationIdentifier id : correlationIds) {
            if (id.match(startTime, cid, baseTime)) {
                return true;
            }
        }
        return false;
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
