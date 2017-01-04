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
package org.hawkular.apm.api.services.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics.ConnectionStatistics;
import org.hawkular.apm.api.utils.EndpointUtil;

/**
 * This class provides a utility for building communication summary trees
 * from a list of nodes.
 *
 * @author gbrown
 */
public class CommunicationSummaryTreeBuilder {

    private static final Logger log = Logger.getLogger(CommunicationSummaryTreeBuilder.class.getName());

    /**
     * This method returns the supplied list of flat nodes as a set of tree structures with related nodes.
     *
     * @param nodes The collection of nodes represented as a flat list
     * @param endpoints The initial endpoints
     * @return The nodes returns as a collection of independent tree structures
     */
    public static Collection<CommunicationSummaryStatistics> buildCommunicationSummaryTree(
            Collection<CommunicationSummaryStatistics> nodes, Set<String> endpoints) {
        Map<String, CommunicationSummaryStatistics> nodeMap = new HashMap<String, CommunicationSummaryStatistics>();

        // Create a map of nodes
        for (CommunicationSummaryStatistics css : nodes) {
            nodeMap.put(css.getId(), css);
        }

        List<CommunicationSummaryStatistics> ret = new ArrayList<>();
        for (String endpoint : endpoints) {
            // Check if a 'client' node also exists for the endpoint, and if so, use this as the
            // initial endpoint
            CommunicationSummaryStatistics n = nodeMap.get(EndpointUtil.encodeClientURI(endpoint));
            if (n == null) {
                n = nodeMap.get(endpoint);
            }
            if (n != null) {
                CommunicationSummaryStatistics rootNode = new CommunicationSummaryStatistics(n);
                initCommunicationSummaryTreeNode(rootNode, nodeMap,
                        new HashSet<>(Collections.singleton(rootNode.getId())));
                ret.add(rootNode);
            }
        }

        return ret;
    }

    /**
     * This method recursively builds the communication summary tree, using the supplied node map,
     * taking copies of each node to ensure the original list is not modified as some of the nodes
     * may be shared between multiple trees. If a node is add to the tree, its id will be added to the
     * 'used' list, to ensure that repetitions are only handled by a link (not including the sub-tree
     * multiple times).
     *
     * @param node The current node
     * @param nodeMap The map of possible nodes
     * @param usedIds The list of node ids already included in the tree
     */
    protected static void initCommunicationSummaryTreeNode(CommunicationSummaryStatistics node,
            Map<String, CommunicationSummaryStatistics> nodeMap, Set<String> usedIds) {
        for (String id : node.getOutbound().keySet()) {
            if (!usedIds.contains(id)) {
                CommunicationSummaryStatistics orig = nodeMap.get(id);
                CommunicationSummaryStatistics copy = null;
                if (orig == null) {
                    log.fine("Node missing for id = " + id + " keySet = " + nodeMap.keySet());
                    // Create a placeholder for the invoked service
                    copy = new CommunicationSummaryStatistics();
                    copy.setId(id);
                } else {
                    copy = new CommunicationSummaryStatistics(orig);
                }
                ConnectionStatistics cs = node.getOutbound().get(id);
                cs.setNode(copy);
                usedIds.add(id);

                // Recusively process the added tree node
                initCommunicationSummaryTreeNode(copy, nodeMap, usedIds);
            }
        }
    }

}
