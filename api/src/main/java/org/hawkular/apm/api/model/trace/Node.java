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
package org.hawkular.apm.api.model.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This abstract class is the base for all nodes describing a trace
 * instance flow.
 *
 * @author gbrown
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = ContainerNode.class) })
@ApiModel(subTypes = { ContainerNode.class }, discriminator = "type")
public abstract class Node {

    @JsonInclude
    private NodeType type;

    @JsonInclude
    private String uri;

    @JsonInclude
    private String operation;

    @JsonInclude
    private long baseTime = 0;

    @JsonInclude
    private long duration = 0;

    @JsonInclude(Include.NON_NULL)
    private String fault;

    @JsonInclude(Include.NON_NULL)
    private String faultDescription;

    @JsonInclude(Include.NON_EMPTY)
    private Set<Property> properties = new HashSet<Property>();

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> details = new HashMap<String, String>();

    @JsonInclude(Include.NON_EMPTY)
    private List<CorrelationIdentifier> correlationIds = new ArrayList<CorrelationIdentifier>();

    @JsonInclude(Include.NON_EMPTY)
    private List<Issue> issues = new ArrayList<Issue>();

    public Node(NodeType type) {
        this.type = type;
    }

    public Node(NodeType type, String uri) {
        this(type);
        this.uri = uri;
    }

    /**
     * This method indicates whether this is a container based node.
     *
     * @return Whether the node is a container
     */
    public boolean containerNode() {
        return false;
    }

    /**
     * This method indicates whether this is an interaction based node.
     *
     * @return Whether the node is interaction based
     */
    public boolean interactionNode() {
        return false;
    }

    /**
     * @return the type
     */
    public NodeType getType() {
        return type;
    }

    /**
     * @param type the type to set
     * @return The node
     */
    public Node setType(NodeType type) {
        this.type = type;
        return this;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     * @return The node
     */
    public Node setUri(String uri) {
        this.uri = uri;
        return this;
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
     * @return the baseTime (in nanoseconds)
     */
    public long getBaseTime() {
        return baseTime;
    }

    /**
     * @param baseTime the baseTime (in nanoseconds) to set
     * @return The node
     */
    public Node setBaseTime(long baseTime) {
        this.baseTime = baseTime;
        return this;
    }

    /**
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     * @return The node
     */
    public Node setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    /**
     * @return the fault
     */
    public String getFault() {
        return fault;
    }

    /**
     * @param fault the fault to set
     * @return The node
     */
    public Node setFault(String fault) {
        this.fault = fault;
        return this;
    }

    /**
     * @return the faultDescription
     */
    public String getFaultDescription() {
        return faultDescription;
    }

    /**
     * @param faultDescription the faultDescription to set
     * @return The node
     */
    public Node setFaultDescription(String faultDescription) {
        this.faultDescription = faultDescription;
        return this;
    }

    /**
     * @return the properties
     */
    public Set<Property> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     * @return The node
     */
    public Node setProperties(Set<Property> properties) {
        if (details == null) {
            throw new IllegalArgumentException("Null value not permitted");
        }

        this.properties = properties;
        return this;
    }

    /**
     * This method returns the specific details about the node.
     *
     * @return the details
     */
    public Map<String, String> getDetails() {
        return details;
    }

    /**
     * @param details the details to set
     * @return The node
     */
    public Node setDetails(Map<String, String> details) {
        if (details == null) {
            throw new IllegalArgumentException("Null value not permitted");
        }

        this.details = details;
        return this;
    }

    /**
     * This method determines whether there is atleast one
     * property with the supplied name.
     *
     * @param name The property name
     * @return Whether a property of the supplied name is defined
     */
    public boolean hasProperty(String name) {
        return this.properties.stream().filter(p -> p.getName().equals(name)).findFirst().isPresent();
    }

    /**
     * This method returns the set of properties having the
     * supplied property name.
     *
     * @param name The property name
     * @return The set of properties with the supplied name
     */
    public Set<Property> getProperties(String name) {
        return this.properties.stream().filter(property -> property.getName().equals(name)).collect(Collectors.toSet());
    }

    /**
     * This method adds the properties for this node to the
     * supplied set.
     *
     * @param allProperties The aggregated set of properties
     */
    protected void includeProperties(Set<Property> allProperties) {
        allProperties.addAll(this.properties);
    }

    /**
     * @return the correlationIds
     */
    public List<CorrelationIdentifier> getCorrelationIds() {
        return correlationIds;
    }

    /**
     * @param correlationIds the correlationIds to set
     * @return The node
     */
    public Node setCorrelationIds(List<CorrelationIdentifier> correlationIds) {
        this.correlationIds = correlationIds;
        return this;
    }

    /**
     * This method adds an interaction scoped correlation id.
     *
     * @param id The id
     * @return The node
     */
    public Node addInteractionCorrelationId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Interaction, id));
        return this;
    }

    /**
     * This method adds an association scoped correlation id.
     *
     * @param id The id
     * @return The node
     */
    public Node addAssociationCorrelationId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Association, id));
        return this;
    }

    /**
     * This method adds a fragment scoped correlation id.
     *
     * @param id The id
     * @return The node
     */
    public Node addFragmentCorrelationId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Fragment, id));
        return this;
    }

    /**
     * This method adds a node scoped correlation id.
     *
     * @param id The id
     * @return The node
     */
    public Node addNodeCorrelationId(String id) {
        this.correlationIds.add(new CorrelationIdentifier(Scope.Fragment, id));
        return this;
    }

    /**
     * This methd returns the subset of correlation ids that have the
     * specified scope.
     *
     * @param scope The scope
     * @return The subset of correlation ids that are associated with the scope
     */
    public List<CorrelationIdentifier> findCorrelationIds(Scope... scope) {
        List<CorrelationIdentifier> ret = null;

        for (int i=0; i < correlationIds.size(); i++) {
            CorrelationIdentifier cid = correlationIds.get(i);
            for (int j=0; j < scope.length; j++) {
                if (cid.getScope() == scope[j]) {
                    if (ret == null) {
                        ret = new ArrayList<CorrelationIdentifier>();
                    }
                    ret.add(cid);
                }
            }
        }

        return ret == null ? Collections.emptyList() : ret;
    }

    /**
     * @return the issues
     */
    public List<Issue> getIssues() {
        return issues;
    }

    /**
     * @param issues the issues to set
     */
    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    /**
     * This method calculates the end time of this node based on the
     * base time and duration. An end time will only be returned if
     * the base time has been set.
     *
     * @return The end time (in nanoseconds), based on base time and duration, or 0 if
     *                  not known
     */
    protected long endTime() {
        long ret = 0;

        if (baseTime > 0) {
            ret = baseTime + duration;
        }

        return ret;
    }

    /**
     * This method calculates the time (in nanoseconds) when all work initiated
     * by this node has been completed. Where async execution is performed, this could
     * mean the work continues beyond the scope of the node that initiates
     * the work.
     *
     * @return The completed time (in nanoseconds)
     */
    protected long completedTime() {
        return overallEndTime();
    }

    /**
     * This method calculates the duration when all work initiated by this node
     * has been completed. Where async execution is performed, this could
     * mean the work continues beyond the scope of the node that initiates
     * the work. This will only be calculated where a base time (in nanoseconds)
     * has been set.
     *
     * @return The completed duration (ns)
     */
    protected long completedDuration() {
        long ret = 0;

        if (baseTime > 0) {
            ret = overallEndTime() - baseTime;
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
     * This method identifies all of the nodes within a trace that
     * are associated with the supplied correlation identifier.
     *
     * @param cid The correlation identifier
     * @param nodes The set of nodes that are associated with the correlation identifier
     */
    protected void findCorrelatedNodes(CorrelationIdentifier cid, Set<Node> nodes) {
        if (isCorrelated(cid)) {
            nodes.add(this);
        }
    }

    /**
     * This method determines whether the node is correlated to the supplied
     * identifier.
     *
     * @param cid The correlation id
     * @return Whether the node is correlated to the supplied id
     */
    protected boolean isCorrelated(CorrelationIdentifier cid) {
        for (CorrelationIdentifier id : correlationIds) {
            if (id.equals(cid)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (baseTime ^ (baseTime >>> 32));
        result = prime * result + ((correlationIds == null) ? 0 : correlationIds.hashCode());
        result = prime * result + ((details == null) ? 0 : details.hashCode());
        result = prime * result + (int) (duration ^ (duration >>> 32));
        result = prime * result + ((fault == null) ? 0 : fault.hashCode());
        result = prime * result + ((faultDescription == null) ? 0 : faultDescription.hashCode());
        result = prime * result + ((issues == null) ? 0 : issues.hashCode());
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
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
        Node other = (Node) obj;
        if (baseTime != other.baseTime)
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
        if (faultDescription == null) {
            if (other.faultDescription != null)
                return false;
        } else if (!faultDescription.equals(other.faultDescription))
            return false;
        if (issues == null) {
            if (other.issues != null)
                return false;
        } else if (!issues.equals(other.issues))
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
        if (type != other.type)
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

}
