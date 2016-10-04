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

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.Node;

/**
 * This class provides node utility functions.
 *
 * @author gbrown
 */
public class NodeUtil {

    private static final String APM_ORIGINAL_URI = "apm_original_uri";

    /**
     * This method tests whether the URI has been rewritten.
     *
     * @param node The node
     * @return Whether the node's URI has been rewritten
     */
    public static boolean isURIRewritten(Node node) {
        return node.getDetails().containsKey(APM_ORIGINAL_URI);
    }

    /**
     * This method rewrites the URI associated with the supplied
     * node and stores the original in the node's details.
     *
     * @param node The node
     * @param uri The new URI
     */
    public static void rewriteURI(Node node, String uri) {
        node.getDetails().put(APM_ORIGINAL_URI, node.getUri());
        node.setUri(uri);
    }

    /**
     * This method determines whether the supplied URI matches the
     * original URI on the node.
     *
     * @param node The node
     * @param uri The URI
     * @return Whether the supplied URI is the same as the node's original
     */
    public static boolean isOriginalURI(Node node, String uri) {
        if (node.getUri().equals(uri)) {
            return true;
        }
        String original = node.getDetails().get(APM_ORIGINAL_URI);
        if (original != null) {
            return original.equals(uri);
        }
        return false;
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
