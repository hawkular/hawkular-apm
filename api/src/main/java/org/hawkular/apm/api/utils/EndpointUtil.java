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
package org.hawkular.apm.api.utils;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * This class provides endpoint utility functions.
 *
 * @author gbrown
 */
public class EndpointUtil {

     /**
     * This method converts the supplied URI and optional operation
     * into an endpoint descriptor.
     *
     * @param uri The URI
     * @param operation The optional operation
     * @return The endpoint descriptor
     */
    public static String encodeEndpoint(String uri, String operation) {
        StringBuilder buf=new StringBuilder();
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

    /**
     * This method returns the URI part of the supplied endpoint.
     *
     * @param endpoint The endpoint
     * @return The URI, or null if endpoint starts with '[' (operation prefix)
     */
    public static String decodeEndpointURI(String endpoint) {
        int ind=endpoint.indexOf('[');
        if (ind == 0) {
            return null;
        } else if (ind != -1) {
            return endpoint.substring(0, ind);
        }
        return endpoint;
    }

    /**
     * This method returns the operation part of the supplied endpoint.
     *
     * @param endpoint The endpoint
     * @param stripped Whether brackets should be stripped
     * @return The operation
     */
    public static String decodeEndpointOperation(String endpoint, boolean stripped) {
        int ind=endpoint.indexOf('[');
        if (ind != -1) {
            if (stripped) {
                return endpoint.substring(ind+1, endpoint.length()-1);
            }
            return endpoint.substring(ind);
        }
        return null;
    }

    /**
     * This method provides a client based encoding of an URI. This is required to identify
     * the client node invoking a service using a particular URI.
     *
     * @param uri The original URI
     * @return The client side version of the URI
     */
    public static String encodeClientURI(String uri) {
        if (uri == null) {
            return Constants.URI_CLIENT_PREFIX;
        }
        return Constants.URI_CLIENT_PREFIX + uri;
    }

    /**
     * This method provides a decoding of a client based URI.
     *
     * @param clientUri The client URI
     * @return The original URI
     */
    public static String decodeClientURI(String clientUri) {
        return clientUri.startsWith(Constants.URI_CLIENT_PREFIX)
                ? clientUri.substring(Constants.URI_CLIENT_PREFIX.length()): clientUri;
    }

    /**
     * This method determines the source URI that should be attributed to the supplied
     * fragment. If the top level fragment just contains a Producer, then prefix with
     * 'client' to distinguish it from the same endpoint for the service.
     *
     * @param fragment The trace fragment
     * @return The source endpoint
     */
    public static EndpointRef getSourceEndpoint(Trace fragment) {
        Node rootNode = fragment.getNodes().isEmpty() ? null : fragment.getNodes().get(0);
        if (rootNode == null) {
            return null;
        }
        // Create endpoint reference. If initial fragment and root node is a Producer,
        // then define it as a 'client' endpoint to distinguish it from the same
        // endpoint representing the server
        return new EndpointRef(rootNode.getUri(), rootNode.getOperation(),
                    fragment.initialFragment() && rootNode instanceof Producer);
    }

}
