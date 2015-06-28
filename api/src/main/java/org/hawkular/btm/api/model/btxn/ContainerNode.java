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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * This abstract class represents the base for all nodes that can contain
 * other nodes within the business transaction instance.
 *
 * @author gbrown
 */
@ApiModel(parent = Node.class)
public abstract class ContainerNode extends Node {

    @JsonInclude(Include.NON_NULL)
    private List<Node> nodes = new ArrayList<Node>();

    public ContainerNode() {
    }

    public ContainerNode(String uri) {
        super(uri);
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
    protected void findCorrelatedNodes(CorrelationIdentifier cid, long baseTime, Set<Node> nodes) {
        super.findCorrelatedNodes(cid, baseTime, nodes);

        // Propagate to child nodes
        for (Node child : getNodes()) {
            child.findCorrelatedNodes(cid, baseTime, nodes);
        }
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
