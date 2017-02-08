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
package org.hawkular.apm.client.opentracing.refactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.client.opentracing.TagUtil;

import io.opentracing.tag.Tags;

/**
 * This class is responsible for building up the information related to a particular node
 * within a trace fragment. The actual node type may not be known until part way through
 * this process. So instead a tree of node builders is constructed, and only converted into
 * a node tree as a final step.
 *
 * @author gbrown
 * @author Pavol Loffay
 */
public class NodeBuilder {

    private TraceFragmentState traceFragmentState;
    // "almost" each node corresponds to one span
    private HawkularSpan linkedSpan;

    private String uri;
    private String operation;
    private NodeType nodeType = NodeType.Component;
    private List<NodeBuilder> nodes = new ArrayList<>();
    private List<CorrelationIdentifier> correlationIds = new ArrayList<>();

    private final String nodePath;

    NodeBuilder(TraceFragmentState traceFragmentState) {
        this.traceFragmentState = traceFragmentState;
        if (traceFragmentState.getRootBuilder() == null) {
            nodePath = String.format("%s:0", traceFragmentState.getFragmentId());
        } else {
            nodePath = String.format("%s:0", traceFragmentState.getRootBuilder().getNodePath());
        }
    }

    private NodeBuilder(String nodePath, TraceFragmentState traceFragmentState) {
        this.nodePath = nodePath;
        this.traceFragmentState = traceFragmentState;
    }

    // almost each node corresponds to ospan
    void setLinkedSpan(HawkularSpan span) {
        this.linkedSpan = span;
    }

    NodeBuilder addChildNode() {
        synchronized (nodes) {
            traceFragmentState.nodeCounter.incrementAndGet();
            NodeBuilder descendant = new NodeBuilder(String.format("%s:%d", nodePath, this.nodes.size()),
                    traceFragmentState);
            nodes.add(descendant);
            return descendant;
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
        String component = null;
        String endpointType = null;
        Set<Property> properties = Collections.emptySet();

        if (linkedSpan != null) {
            properties = new HashSet<>(linkedSpan.tags().size());
            for (Map.Entry<String, Object> tagEntry : linkedSpan.tags().entrySet()) {
                if (tagEntry.getKey() != null && tagEntry.getValue() != null) {
                    properties.add(new Property(tagEntry.getKey(), tagEntry.getValue()));
                }

                if (Tags.COMPONENT.getKey().equals(tagEntry.getKey())) {
                    component = tagEntry.getValue().toString();
                }

                if (TagUtil.isUriKey(tagEntry.getKey())) {
                    if (uri == null) {
                        uri = TagUtil.getUriPath(tagEntry.getValue().toString());
                    }
                    endpointType = TagUtil.getTypeFromUriKey(tagEntry.getKey());
                }

                if (Constants.PROP_TRANSACTION_NAME.equals(tagEntry.getKey())) {
                    if (traceFragmentState.getTransaction() == null) {
                        traceFragmentState.setNamedTransaction(tagEntry.getValue().toString());
                    }
                }
            }
        }

        ContainerNode node = null;
        if (nodeType == NodeType.Component) {
            node = new Component();
            ((Component) node).setComponentType(component == null ? endpointType : component);
        } else if (nodeType == NodeType.Consumer) {
            node = new Consumer();
            ((Consumer) node).setEndpointType(endpointType);
        } else if (nodeType == NodeType.Producer) {
            node = new Producer();
            ((Producer) node).setEndpointType(endpointType);
        }

        if (linkedSpan != null) {
            EndpointRef epref = new EndpointRef(TagUtil.getUriPath(linkedSpan.tags()),
                    linkedSpan.operationName(), false);
            node.setUri(epref.getUri());
            node.setOperation(epref.getOperation());
        } else {
            node.setUri(uri);
            node.setOperation(operation);
        }

        node.setCorrelationIds(correlationIds);


        node.setProperties(properties);

        if (linkedSpan != null) {
            node.setTimestamp(linkedSpan.startMicros());
            node.setDuration(linkedSpan.finishMicros() - linkedSpan.startMicros());
        }

        for (int i = 0; i < nodes.size(); i++) {
            node.getNodes().add(nodes.get(i).build());
        }

        return node;
    }
}

