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
package org.hawkular.apm.api.model.config.txn;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.trace.NodeType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a processor, used to extract information from the instrumented
 * location, to derive properties, correlation ids and contents to be stored in the
 * trace fragment/nodes.
 *
 * @author gbrown
 */
public class Processor {

    @JsonInclude(Include.NON_NULL)
    private String description;

    @JsonInclude
    private NodeType nodeType;

    @JsonInclude
    private Direction direction = Direction.In;

    @JsonInclude
    private String uriFilter;

    @JsonInclude(Include.NON_NULL)
    private String operation;

    @JsonInclude(Include.NON_NULL)
    private String faultFilter;

    @JsonInclude(Include.NON_NULL)
    private Expression predicate;

    @JsonInclude
    private List<ProcessorAction> actions = new ArrayList<ProcessorAction>();

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the nodeType
     */
    public NodeType getNodeType() {
        return nodeType;
    }

    /**
     * @param nodeType the nodeType to set
     */
    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * @return the uriFilter
     */
    public String getUriFilter() {
        return uriFilter;
    }

    /**
     * @param uriFilter the uriFilter to set
     */
    public void setUriFilter(String uriFilter) {
        this.uriFilter = uriFilter;
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
     * @return the faultFilter
     */
    public String getFaultFilter() {
        return faultFilter;
    }

    /**
     * @param faultFilter the faultFilter to set
     */
    public void setFaultFilter(String faultFilter) {
        this.faultFilter = faultFilter;
    }

    /**
     * @return the predicate
     */
    public Expression getPredicate() {
        return predicate;
    }

    /**
     * @param predicate the predicate to set
     */
    public void setPredicate(Expression predicate) {
        this.predicate = predicate;
    }

    /**
     * @return the actions
     */
    public List<ProcessorAction> getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(List<ProcessorAction> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "Processor [description=" + description + ", nodeType=" + nodeType + ", direction=" + direction
                + ", uriFilter=" + uriFilter + ", operation=" + operation + ", faultFilter=" + faultFilter
                + ", predicate=" + predicate + ", actions=" + actions + "]";
    }

}
