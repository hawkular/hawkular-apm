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
package org.hawkular.apm.api.model.events;

import java.io.Serializable;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.utils.EndpointUtil;

/**
 * This class represents an endpoint reference.
 *
 * @author gbrown
 */
public class EndpointRef implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uri;
    private String operation;
    private boolean client = false;

    /**
     * The default constructor.
     */
    public EndpointRef() {
    }

    /**
     * This constructor initialises the endpoint details.
     *
     * @param uri The URI
     * @param operation The operation
     * @param client Whether the endpoint relates to the client
     */
    public EndpointRef(String uri, String operation, boolean client) {
        this.uri = uri;
        this.operation = operation;
        this.client = client;
    }

    /**
     * This method returns the URI associated with the endpoint. If it
     * relates to the client side of the endpoint, it will be prefixed
     * to distinguish it as such.
     *
     * @return The URI
     */
    public final String getUri() {
        if (client) {
            return EndpointUtil.encodeClientURI(uri);
        }
        return uri;
    }

    /**
     * This method returns the operation.
     *
     * @return The operation
     */
    public final String getOperation() {
        return operation;
    }

    /**
     * This method identifies whether the endpoint relates to the client
     * side.
     *
     * @return Whether related to a client
     */
    public final boolean isClient() {
        return client;
    }

    @Override
    public String toString() {
        StringBuilder buf=new StringBuilder();
        if (client) {
            buf.append(Constants.URI_CLIENT_PREFIX);
        }
        if (uri != null && !uri.trim().isEmpty()) {
            buf.append(uri);
        }
        if (operation != null && !operation.trim().isEmpty()) {
            buf.append('[');
            buf.append(operation);
            buf.append(']');
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (client ? 1231 : 1237);
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EndpointRef other = (EndpointRef) obj;
        if (client != other.client)
            return false;
        if (operation == null) {
            if (other.operation != null)
                return false;
        } else if (!operation.equals(other.operation))
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

}
