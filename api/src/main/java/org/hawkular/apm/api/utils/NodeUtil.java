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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.Node;

/**
 * This class provides node utility functions.
 *
 * @author gbrown
 */
public class NodeUtil {

    private static final Logger log = Logger.getLogger(NodeUtil.class);

    private static final String APM_ORIGINAL_URI = "apm_original_uri";

    /**
     * This method tests whether the URI has been rewritten.
     *
     * @param node The node
     * @return Whether the node's URI has been rewritten
     * @deprecated Only used in original JavaAgent. Not to be used with OpenTracing.
     */
    @Deprecated
    public static boolean isURIRewritten(Node node) {
        return node.hasProperty(APM_ORIGINAL_URI);
    }

    /**
     * This method rewrites the URI associated with the supplied
     * node and stores the original in the node's details.
     *
     * @param node The node
     * @param uri The new URI
     * @deprecated Only used in original JavaAgent. Not to be used with OpenTracing.
     */
    @Deprecated
    public static void rewriteURI(Node node, String uri) {
        node.getProperties().add(new Property(APM_ORIGINAL_URI, node.getUri()));
        node.setUri(uri);
    }

    /**
     * This method determines whether the supplied URI matches the
     * original URI on the node.
     *
     * @param node The node
     * @param uri The URI
     * @return Whether the supplied URI is the same as the node's original
     * @deprecated Only used in original JavaAgent. Not to be used with OpenTracing.
     */
    @Deprecated
    public static boolean isOriginalURI(Node node, String uri) {
        if (node.getUri().equals(uri)) {
            return true;
        }
        if (node.hasProperty(APM_ORIGINAL_URI)) {
            return node.getProperties(APM_ORIGINAL_URI).iterator().next().getValue().equals(uri);
        }
        return false;
    }

    /**
     * This method checks whether a URI template has been defined. If so, then it will
     * extract the relevant properties from the URI and rewrite the URI to be the template.
     * This results in the URI being a stable/consistent value (for aggregation purposes)
     * while retaining the parameters from the actual URI as properties.
     *
     * @param node The node containing the current URI and optional template
     * @return Whether the node was processed
     */
    public static boolean rewriteURI(Node node) {
        boolean processed = false;

        // Check if URI and template has been defined
        if (node.getUri() != null && node.hasProperty(Constants.PROP_HTTP_URL_TEMPLATE)) {
            List<String> queryParameters = new ArrayList<String>();
            String template = node.getProperties(Constants.PROP_HTTP_URL_TEMPLATE).iterator().next().getValue();
            if (template == null) {
                return false;
            }

            // If template contains a query string component, then separate the details
            if (template.indexOf('?') != -1) {
                int index = template.indexOf('?');
                String queryString = template.substring(index + 1);

                template = template.substring(0, index);

                StringTokenizer st = new StringTokenizer(queryString, "&");

                while (st.hasMoreTokens()) {
                    String token = st.nextToken();

                    if (token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}') {
                        queryParameters.add(token.substring(1, token.length() - 1));
                    } else {
                        log.severe("Expecting query parameter template, e.g. {name}, but got '" + token + "'");
                    }
                }
            }

            String[] templateTokensArray = template.split("/");
            String[] uriTokensArray = node.getUri().split("/", templateTokensArray.length);

            if (templateTokensArray.length != uriTokensArray.length) {
                return false;
            }

            Set<Property> props = null;
            for (int i = 1 ; i < uriTokensArray.length ; i++) {
                String uriToken = uriTokensArray[i];
                String templateToken = templateTokensArray[i];

                if (templateToken.charAt(0) == '{' && templateToken.charAt(templateToken.length() - 1) == '}') {
                    int lastPosition = templateToken.length() - 1;
                    int positionColon = templateToken.indexOf(':');
                    if (positionColon > 0) {
                        lastPosition = positionColon;
                    }
                    String name = templateToken.substring(1, lastPosition);
                    if (props == null) {
                        props = new HashSet<Property>();
                    }
                    try {
                        props.add(new Property(name, URLDecoder.decode(uriToken, "UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("Failed to decode value '" + uriToken + "': " + e);
                        }
                    }
                } else if (!uriToken.equals(templateToken)) {
                    // URI template mismatch
                    return false;
                }
            }

            // If properties extracted, then add to txn properties, and set the node's
            // URI to the template, to make it stable/consistent - to make analytics easier
            if (props != null) {
                node.setUri(template);
                node.getProperties().addAll(props);
                processed = true;
            }

            if (rewriteURIQueryParameters(node, queryParameters)) {
                processed = true;
            }
        }

        return processed;
    }

    private static boolean rewriteURIQueryParameters(Node node, List<String> queryParameters) {
        boolean processed = false;

        // If query parameter template defined, then process
        if (!queryParameters.isEmpty() && node.hasProperty(Constants.PROP_HTTP_QUERY)) {
            StringTokenizer st = new StringTokenizer(node.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue(), "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                String[] namevalue = token.split("=");
                if (namevalue.length == 2) {
                    if (queryParameters.contains(namevalue[0])) {
                        try {
                            node.getProperties().add(new Property(URLDecoder.decode(namevalue[0], "UTF-8"),
                                    URLDecoder.decode(namevalue[1], "UTF-8")));
                            processed = true;
                        } catch (UnsupportedEncodingException e) {
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("Failed to decode value '" + namevalue[1] + "': " + e);
                            }
                        }
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest("Ignoring query parameter '" + namevalue[0] + "'");
                    }
                } else if (log.isLoggable(Level.FINEST)) {
                    log.finest("Query string part does not include name/value pair: " + token);
                }
            }
        }

        return processed;
    }

    /**
     * This method recursively scans a node hierarchy to locate instances of a particular
     * type.
     *
     * @param nodes The nodes to scan
     * @param cls The class of the node to be returned
     * @return The list of nodes found
     */
    public static <T extends Node> List<T> findNodes(List<Node> nodes, Class<T> cls) {
        List<T> results = new ArrayList<>();
        findNodes(nodes, cls, results);
        return results;
    }

    /**
     * This method recursively scans a node hierarchy to locate instances of a particular
     * type.
     *
     * @param nodes The nodes to scan
     * @param cls The class of the node to be returned
     * @param results The list of nodes found
     */
    @SuppressWarnings("unchecked")
    public static <T extends Node> void findNodes(List<Node> nodes, Class<T> cls, List<T> results) {
        for (Node n : nodes) {
            if (n instanceof ContainerNode) {
                findNodes(((ContainerNode) n).getNodes(), cls, results);
            }

            if (cls == n.getClass()) {
                results.add((T) n);
            }
        }
    }

}
