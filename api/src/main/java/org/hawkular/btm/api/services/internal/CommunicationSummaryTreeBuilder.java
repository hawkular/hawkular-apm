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
package org.hawkular.btm.api.services.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics.ConnectionStatistics;

/**
 * This class provides a utility for building communication summary trees
 * from a list of nodes.
 *
 * @author gbrown
 */
public class CommunicationSummaryTreeBuilder {

    /**
     * This method returns the supplied list of flat nodes as a set of tree structures with related nodes.
     *
     * @param nodes The collection of nodes represented as a flat list
     * @return The nodes returns as a collection of independent tree structures
     */
    public static Collection<CommunicationSummaryStatistics> buildCommunicationSummaryTree(
            Collection<CommunicationSummaryStatistics> nodes) {
        Map<String, CommunicationSummaryStatistics> nodeMap = new HashMap<String, CommunicationSummaryStatistics>();

        // Create a map of nodes
        for (CommunicationSummaryStatistics css : nodes) {
            nodeMap.put(css.getId(), css);
        }

        Collection<CommunicationSummaryStatistics> rootNodes = getRootCommunicationSummaryNodes(nodeMap);

        if (rootNodes != null) {
            List<CommunicationSummaryStatistics> ret = new ArrayList<CommunicationSummaryStatistics>();
            for (CommunicationSummaryStatistics css : rootNodes) {
                CommunicationSummaryStatistics rootNode = new CommunicationSummaryStatistics(css);
                List<String> usedIds = new ArrayList<String>();
                usedIds.add(rootNode.getId());
                initCommunicationSummaryTreeNode(rootNode, nodeMap, usedIds);
                ret.add(rootNode);
            }
            return ret;
        }

        return null;
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
            Map<String, CommunicationSummaryStatistics> nodeMap, List<String> usedIds) {
        for (String id : node.getOutbound().keySet()) {
            if (!usedIds.contains(id)) {
                CommunicationSummaryStatistics copy = new CommunicationSummaryStatistics(nodeMap.get(id));
                ConnectionStatistics cs = node.getOutbound().get(id);
                cs.setNode(copy);
                usedIds.add(id);

                // Recusively process the added tree node
                initCommunicationSummaryTreeNode(copy, nodeMap, usedIds);
            }
        }
    }

    /**
     * This method returns the subset of supplied nodes that are root nodes.
     *
     * @param nodeMap The map of all nodes
     * @return The list of root nodes
     */
    protected static Collection<CommunicationSummaryStatistics> getRootCommunicationSummaryNodes(
            Map<String, CommunicationSummaryStatistics> nodeMap) {
        Map<String, CommunicationSummaryStatistics> nodeMapCopy =
                new HashMap<String, CommunicationSummaryStatistics>(nodeMap);

        for (CommunicationSummaryStatistics css : nodeMap.values()) {
            for (String linkId : css.getOutbound().keySet()) {
                // Remove linked node from copy map
                nodeMapCopy.remove(linkId);
            }
        }

        return nodeMapCopy.values();
    }

}
