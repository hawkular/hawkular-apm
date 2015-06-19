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
package org.hawkular.btm.client.collector.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.Node;

/**
 * This class represents the builder for a business transaction fragment. NOTE: This
 * class is not thread safe as the sequence of events within a thread of execution
 * should be in sequence, and therefore should not result in any concurrent conflicts.
 *
 * @author gbrown
 */
public class FragmentBuilder {

    private static final Logger log=Logger.getLogger(FragmentBuilder.class.getName());

    private BusinessTransaction businessTransaction =
            new BusinessTransaction().setId(UUID.randomUUID().toString());

    private Stack<Node> nodeStack = new Stack<Node>();

    private Map<String,Node> retainedNodes = new HashMap<String,Node>();

    private List<String> unlinkedIds=new ArrayList<String>();

    /**
     * This method determines if the fragment is complete.
     *
     * @return Whether the fragment is complete
     */
    public boolean isComplete() {
        return nodeStack.isEmpty() && retainedNodes.isEmpty();
    }

    /**
     * @return the businessTransaction
     */
    public BusinessTransaction getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * This method returns the current node providing the scope
     * for further levels of detail.
     *
     * @return The current node
     */
    public Node getCurrentNode() {
        return (nodeStack.isEmpty() ? null : nodeStack.peek());
    }

    /**
     * This method pushes a new node into the business transaction
     * fragment hierarchy.
     *
     * @param node The new node
     */
    public void pushNode(Node node) {
        if (nodeStack.isEmpty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Pushing top level node: "+node+" for txn: "+businessTransaction);
            }
            businessTransaction.getNodes().add(node);
        } else {
            Node parent = nodeStack.peek();

            if (parent instanceof ContainerNode) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Add node: "+node+" to parent: "+parent+" in txn: "+businessTransaction);
                }
                ((ContainerNode) parent).getNodes().add(node);
            } else {
                log.severe("Attempt to add node '"+node+"' under non-container node '"+parent+"'");
            }
        }
        nodeStack.push(node);
    }

    /**
     * This method pops the latest node from the business transaction
     * fragment hierarchy.
     *
     * @return The latest node
     */
    public Node popNode() {
        return nodeStack.pop();
    }

    /**
     * This method indicates that the current node, for this thread of execution, should
     * be retained temporarily pending further changes.
     *
     * @param id The identifier used to later on to identify the node
     */
    public void retainNode(String id) {
        Node current=getCurrentNode();

        if (current != null) {
            retainedNodes.put(id, current);
        }
    }

    /**
     * This method indicates that the identified node, for this thread of execution, should
     * be released.
     *
     * @param id The identifier used to identify the node
     */
    public void releaseNode(String id) {
        retainedNodes.remove(id);
    }

    /**
     * This method returns the node associated, for this thread of execution, identified
     * by the supplied id.
     *
     * @param id The identifier used to identify the node
     * @return The node, or null if not found
     */
    public Node retrieveNode(String id) {
        return retainedNodes.get(id);
    }

    /**
     * This method returns the 'unlinked ids' list.
     *
     * @return The unlinked ids
     */
    public List<String> getUnlinkedIds() {
        return unlinkedIds;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder info=new StringBuilder();
        info.append("Fragment builder: current btxn=[");
        info.append(businessTransaction);
        info.append("] complete=");
        info.append(isComplete());
        info.append(" unlinkedIds=");
        info.append(getUnlinkedIds());
        return (info.toString());
    }
}
