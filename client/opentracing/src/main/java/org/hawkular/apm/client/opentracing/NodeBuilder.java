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
package org.hawkular.apm.client.opentracing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.utils.NodeUtil;

/**
 * This class is responsible for building up the information related to a particular node
 * within a trace fragment. The actual node type may not be known until part way through
 * this process. So instead a tree of node builders is constructed, and only converted into
 * a node tree as a final step.
 *
 * @author gbrown
 */
public class NodeBuilder {

    private String uri;
    private String operation;
    private String endpointType = "n/a";    // Default endpoint type used to signify an external endpoint but
                                            // type is unknown. A 'null' endpoint is for internal connections.
    private String componentType;
    /**
     * Duration in microseconds
     */
    private long duration;
    private Set<Property> properties = new HashSet<>();
    private List<CorrelationIdentifier> correlationIds = new ArrayList<>();
    private List<NodeBuilder> nodes = new ArrayList<>();

    /**
     * Timestamp in microseconds
     */
    private long timestamp;

    private NodeType nodeType = NodeType.Component;

    private String nodePath;

    /**
     * The default constructor.
     */
    public NodeBuilder() {
    }

    /**
     * This constructor is initialised with the parent node builder.
     *
     * @param parent The parent
     */
    public NodeBuilder(NodeBuilder parent) {
        int pos = parent.addChildNode(this);
        nodePath = String.format("%s:%d", parent.getNodePath(), pos);
    }

    /**
     * This method adds the supplied node builder as a child
     * of this node builder.
     *
     * @param child
     * @return The position of the child node
     */
    protected int addChildNode(NodeBuilder child) {
        synchronized (nodes) {
            nodes.add(child);
            return nodes.size() - 1;
        }
    }

    /**
     * This method returns the node path associated with this node.
     *
     * @return The node path
     */
    public String getNodePath() {
        return nodePath;
    }

    /**
     * This method sets the node path for the node.
     *
     * @param nodePath The node path
     */
    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    /**
     * @param uri the uri to set
     * @return The node builder
     */
    public NodeBuilder setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * @param operation the operation to set
     * @return The node builder
     */
    public NodeBuilder setOperation(String operation) {
        this.operation = operation;
        return this;
    }

    /**
     * @param endpointType the endpointType to set
     * @return The node builder
     */
    public NodeBuilder setEndpointType(String endpointType) {
        this.endpointType = endpointType;
        return this;
    }

    /**
     * @param componentType the componentType to set
     * @return The node builder
     */
    public NodeBuilder setComponentType(String componentType) {
        this.componentType = componentType;
        return this;
    }

    /**
     * @param duration The duration (in microseconds)
     * @return The node builder
     */
    public NodeBuilder setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    /**
     * @param timestamp The timestamp (in microseconds)
     * @return The node builder
     */
    public NodeBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * @return the property
     * @return The node builder
     */
    public NodeBuilder addProperty(Property property) {
        this.properties.add(property);
        return this;
    }

    /**
     * @param cid The correlation id
     * @return The node builder
     */
    public NodeBuilder addCorrelationId(CorrelationIdentifier cid) {
        this.correlationIds.add(cid);
        return this;
    }

    /**
     * This method sets the node type.
     *
     * @param nodeType The node type
     * @return The node builder
     */
    public NodeBuilder setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    /**
     * This method builds the node hierarchy.
     *
     * @return The node hierarchy
     */
    public Node build() {
        ContainerNode ret = null;

        if (nodeType == NodeType.Component) {
            ret = new Component();
            ((Component) ret).setComponentType(componentType);
        } else if (nodeType == NodeType.Consumer) {
            ret = new Consumer();
            ((Consumer) ret).setEndpointType(endpointType);
        } else if (nodeType == NodeType.Producer) {
            ret = new Producer();
            ((Producer) ret).setEndpointType(endpointType);
        }
        ret.setCorrelationIds(correlationIds);
        ret.setOperation(operation);
        ret.setProperties(properties);
        ret.setUri(uri);
        ret.setDuration(duration);
        ret.setTimestamp(timestamp);

        for (int i = 0; i < nodes.size(); i++) {
            ret.getNodes().add(nodes.get(i).build());
        }

        // Check if template has been supplied for URI
        NodeUtil.rewriteURI(ret);

        return ret;
    }

}
