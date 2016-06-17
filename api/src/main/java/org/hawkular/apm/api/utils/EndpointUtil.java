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
package org.hawkular.apm.api.utils;

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
        StringBuffer buf=new StringBuffer(uri);
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
     * @return The URI
     */
    public static String decodeEndpointURI(String endpoint) {
        int ind=endpoint.indexOf('[');
        if (ind != -1) {
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

}
