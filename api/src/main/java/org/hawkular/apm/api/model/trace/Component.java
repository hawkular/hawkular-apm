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

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;

/**
 * This class represents the invocation of a component.
 *
 * @author gbrown
 *
 */
@ApiModel(parent = InteractionNode.class)
public class Component extends InteractionNode {

    @JsonInclude
    private String componentType;

    public Component() {
        super(NodeType.Component);
    }

    public Component(String uri, String componentType) {
        super(NodeType.Component, uri);
        this.componentType = componentType;
    }

    /**
     * @return the componentType
     */
    public String getComponentType() {
        return componentType;
    }

    /**
     * @param componentType the componentType to set
     */
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((componentType == null) ? 0 : componentType.hashCode());
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
        Component other = (Component) obj;
        if (componentType == null) {
            if (other.componentType != null) {
                return false;
            }
        } else if (!componentType.equals(other.componentType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Component [componentType=" + componentType + ", getIn()=" + getIn() + ", getOut()=" + getOut()
                + ", getNodes()=" + getNodes() + ", getType()=" + getType() + ", getUri()=" + getUri()
                + ", getOperation()=" + getOperation() + ", getTimestamp()=" + getTimestamp() + ", getDuration()="
                + getDuration() + ", getProperties()=" + getProperties()
                + ", getCorrelationIds()=" + getCorrelationIds() + "]";
    }

}
