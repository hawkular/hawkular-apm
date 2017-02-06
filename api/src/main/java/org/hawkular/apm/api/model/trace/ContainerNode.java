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
package org.hawkular.apm.api.model.trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.Property;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This abstract class represents the base for all nodes that can contain
 * other nodes within the trace instance.
 *
 * @author gbrown
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = InteractionNode.class) })
@ApiModel(parent = Node.class,
    subTypes = { InteractionNode.class }, discriminator = "type")
public abstract class ContainerNode extends Node {

    @JsonInclude(Include.NON_NULL)
    private List<Node> nodes = new ArrayList<Node>();

    public ContainerNode(NodeType type) {
        super(type);
    }

    public ContainerNode(NodeType type, String uri) {
        super(type, uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containerNode() {
        return true;
    }

    /**
     * @return the nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    /**
     * This method adds the properties for this node to the
     * supplied set.
     *
     * @param allProperties The aggregated set of properties
     */
    @Override
    protected void includeProperties(Set<Property> allProperties) {
        super.includeProperties(allProperties);
        nodes.forEach(n -> n.includeProperties(allProperties));
    }

    /**
     * This method determines the overall end time of this node.
     *
     * @return The overall end time
     */
    @Override
    protected long overallEndTime() {
        long ret = super.overallEndTime();

        for (Node child : nodes) {
            long childEndTime = child.overallEndTime();

            if (childEndTime > ret) {
                ret = childEndTime;
            }
        }

        return ret;
    }

    @Override
    protected void findCorrelatedNodes(CorrelationIdentifier cid, Set<Node> nodes) {
        super.findCorrelatedNodes(cid, nodes);

        // Propagate to child nodes
        for (Node child : getNodes()) {
            child.findCorrelatedNodes(cid, nodes);
        }
    }

    public Set<Property> getPropertiesIncludingDescendants(String name) {
        Set<Property> result = new HashSet<>(this.getProperties(name));

        for (Node node : nodes) {
            if (node instanceof ContainerNode) {
                result.addAll(((ContainerNode)node).getPropertiesIncludingDescendants(name));
            } else {
                result.addAll(node.getProperties(name));
            }
        }

        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
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
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContainerNode other = (ContainerNode) obj;
        if (nodes == null) {
            if (other.nodes != null) {
                return false;
            }
        } else if (!nodes.equals(other.nodes)) {
            return false;
        }
        return true;
    }

}
