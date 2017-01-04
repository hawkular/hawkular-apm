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

import java.util.Set;

import org.hawkular.apm.api.model.Property;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This abstract class represents an invocation.
 *
 * @author gbrown
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = Consumer.class),
    @Type(value = Producer.class), @Type(value = Component.class) })
@ApiModel(parent = ContainerNode.class,
    subTypes = { Consumer.class, Producer.class, Component.class }, discriminator = "type")
public abstract class InteractionNode extends ContainerNode {

    public static final String PROPERTY_PUBLISH = "apm_publish";

    @JsonInclude(Include.NON_NULL)
    private Message in;

    @JsonInclude(Include.NON_NULL)
    private Message out;

    public InteractionNode(NodeType type) {
        super(type);
    }

    public InteractionNode(NodeType type, String uri) {
        super(type, uri);
    }

    /**
     * {@inheritDoc}
     */
    public boolean interactionNode() {
        return true;
    }

    /**
     * @return the in message
     */
    public Message getIn() {
        return in;
    }

    /**
     * @param in the in message to set
     */
    public void setIn(Message in) {
        this.in = in;
    }

    /**
     * @return the out message
     */
    public Message getOut() {
        return out;
    }

    /**
     * @param out the out message to set
     */
    public void setOut(Message out) {
        this.out = out;
    }

    /**
     * This method determines whether the interaction node is associated with
     * a multi-consumer communication.
     *
     * @return Whether interaction relates to multiple consumers
     */
    public boolean multipleConsumers() {
        // TODO: When HWKAPM-698 implemented, it may no longer be necessary to capture this info
        Set<Property> props=getProperties(Producer.PROPERTY_PUBLISH);
        return !props.isEmpty()
                && props.iterator().next().getValue().equalsIgnoreCase(Boolean.TRUE.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((in == null) ? 0 : in.hashCode());
        result = prime * result + ((out == null) ? 0 : out.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null) {
            return false;
        }
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        InteractionNode other = (InteractionNode) obj;
        if (in == null) {
            if (other.in != null)
                return false;
        } else if (!in.equals(other.in))
            return false;
        if (out == null) {
            if (other.out != null)
                return false;
        } else if (!out.equals(other.out))
            return false;
        return true;
    }

}
