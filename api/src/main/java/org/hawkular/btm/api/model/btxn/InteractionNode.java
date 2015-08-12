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
    @Type(value = Producer.class),
    @Type(value = Service.class) })
@ApiModel(parent = ContainerNode.class,
    subTypes = { Consumer.class, Producer.class, Service.class }, discriminator = "type")
public abstract class InteractionNode extends ContainerNode {

    @JsonInclude(Include.NON_NULL)
    private Message request;

    @JsonInclude(Include.NON_NULL)
    private Message response;

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
     * @return the request
     */
    public Message getRequest() {
        return request;
    }

    /**
     * @param request the request to set
     */
    public void setRequest(Message request) {
        this.request = request;
    }

    /**
     * @return the response
     */
    public Message getResponse() {
        return response;
    }

    /**
     * @param response the response to set
     */
    public void setResponse(Message response) {
        this.response = response;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((request == null) ? 0 : request.hashCode());
        result = prime * result + ((response == null) ? 0 : response.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        InteractionNode other = (InteractionNode) obj;
        if (request == null) {
            if (other.request != null) {
                return false;
            }
        } else if (!request.equals(other.request)) {
            return false;
        }
        if (response == null) {
            if (other.response != null) {
                return false;
            }
        } else if (!response.equals(other.response)) {
            return false;
        }
        return true;
    }

}
