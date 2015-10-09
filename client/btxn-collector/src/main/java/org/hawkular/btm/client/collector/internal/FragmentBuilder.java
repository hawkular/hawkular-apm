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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
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

    private static final Logger log = Logger.getLogger(FragmentBuilder.class.getName());

    private BusinessTransaction businessTransaction;

    private Stack<Node> nodeStack = new Stack<Node>();

    private Stack<Node> poppedNodes = new Stack<Node>();

    private Stack<Node> suppressedNodeStack = new Stack<Node>();

    private Map<String, Node> retainedNodes = new HashMap<String, Node>();

    private List<String> uncompletedCorrelationIds = new ArrayList<String>();

    private boolean suppress = false;

    private static String hostName;
    private static String hostAddress;

    private int inHashCode = 0;
    private ByteArrayOutputStream inStream = null;
    private int outHashCode = 0;
    private ByteArrayOutputStream outStream = null;

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.severe("Unable to determine host name");
        }

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.severe("Unable to determine host address");
        }
    }

    {
        businessTransaction = new BusinessTransaction()
        .setId(UUID.randomUUID().toString())
        .setStartTime(System.currentTimeMillis())
        .setHostName(hostName)
        .setHostAddress(hostAddress);
    }

    /**
     * This method determines if the fragment is complete.
     *
     * @return Whether the fragment is complete
     */
    public boolean isComplete() {
        synchronized (nodeStack) {
            return nodeStack.isEmpty() && retainedNodes.isEmpty();
        }
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
        synchronized (nodeStack) {
            return (nodeStack.isEmpty() ? null : nodeStack.peek());
        }
    }

    /**
     * @return the nodeStack
     */
    protected Stack<Node> getNodeStack() {
        return nodeStack;
    }

    /**
     * @return the poppedNodes
     */
    protected Stack<Node> getPoppedNodes() {
        return poppedNodes;
    }

    /**
     * This method returns the latest node of the specified type,
     * either on the stack, or on the 'popped' nodes.
     *
     * @param nodeType The node type
     * @param onStack Whether from the stack
     * @return The node, or null if not found
     */
    public Node getLatestNode(String nodeType, boolean onStack) {
        Node node = null;

        synchronized (nodeStack) {
            Stack<Node> stack = (onStack ? nodeStack : poppedNodes);

            for (int i = 0; node == null && i < stack.size(); i++) {
                Node n = stack.elementAt(i);

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Get latest node: checking node type '" + nodeType
                            + "' against '" + n.getClass().getSimpleName()
                            + "' with node=" + n);
                }

                if (n.getClass().getSimpleName().equals(nodeType)) {
                    node = n;
                }
            }
        }

        return node;
    }

    /**
     * This method pushes a new node into the business transaction
     * fragment hierarchy.
     *
     * @param node The new node
     */
    public void pushNode(Node node) {

        // Reset in stream
        inStream = null;

        synchronized (nodeStack) {

            // Clear popped stack
            poppedNodes.clear();

            // Check if fragment is in suppression mode
            if (suppress) {
                suppressedNodeStack.push(node);
                return;
            }

            if (nodeStack.isEmpty()) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Pushing top level node: " + node + " for txn: " + businessTransaction);
                }
                businessTransaction.getNodes().add(node);
            } else {
                Node parent = nodeStack.peek();

                if (parent instanceof ContainerNode) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Add node: " + node + " to parent: " + parent + " in txn: " + businessTransaction);
                    }
                    ((ContainerNode) parent).getNodes().add(node);
                } else {
                    log.severe("Attempt to add node '" + node + "' under non-container node '" + parent + "'");
                }
            }
            nodeStack.push(node);
        }
    }

    /**
     * This method pops the latest node from the business transaction
     * fragment hierarchy.
     *
     * @param cls The type of node to pop
     * @param uri The optional uri to match
     * @return The node
     */
    public Node popNode(Class<? extends Node> cls, String uri) {

        synchronized (nodeStack) {
            // Check if fragment is in suppression mode
            if (suppress) {
                if (!suppressedNodeStack.isEmpty()) {

                    // Check if node is on the suppressed stack
                    Node suppressed = popNode(suppressedNodeStack, cls, uri);
                    if (suppressed != null) {
                        // Popped node from suppressed stack
                        return suppressed;
                    }
                } else {
                    // If suppression parent popped, then canel the suppress mode
                    suppress = false;
                }
            }

            return popNode(nodeStack, cls, uri);
        }
    }

    /**
     * This method pops a node of the defined class and optional uri from the stack.
     * If the uri is not defined, then the latest node of the approach class will
     * be chosen.
     *
     * @param stack The stack
     * @param cls The node type
     * @param uri The optional uri to match
     * @return The node, or null if no suitable candidate is found
     */
    protected Node popNode(Stack<Node> stack, Class<? extends Node> cls, String uri) {
        Node top = stack.isEmpty() ? null : stack.peek();

        if (top != null) {
            if (nodeMatches(top, cls, uri)) {
                Node node = stack.pop();
                poppedNodes.push(node);
                return node;
            } else {
                // Scan for potential match, from -2 so don't repeat
                // check of top node
                for (int i = stack.size() - 2; i >= 0; i--) {
                    if (nodeMatches(stack.get(i), cls, uri)) {
                        Node node = stack.remove(i);
                        poppedNodes.push(node);
                        return node;
                    }
                }
            }
        }

        return null;
    }

    /**
     * This method determines whether the supplied node matches the specified class
     * and optional URI.
     *
     * @param node The node
     * @param cls The class
     * @param uri The optional URI
     * @return Whether the node is of the correct type and matches the optional URI
     */
    protected boolean nodeMatches(Node node, Class<? extends Node> cls, String uri) {
        if (node.getClass() == cls) {
            return uri == null || uri.equals(node.getUri());
        }

        return false;
    }

    /**
     * This method indicates that the current node, for this thread of execution, should
     * be retained temporarily pending further changes.
     *
     * @param id The identifier used to later on to identify the node
     */
    public void retainNode(String id) {
        synchronized (retainedNodes) {
            Node current = getCurrentNode();

            if (current != null) {
                retainedNodes.put(id, current);
            }
        }
    }

    /**
     * This method indicates that the identified node, for this thread of execution, should
     * be released.
     *
     * @param id The identifier used to identify the node
     * @return The node, or null if not found
     */
    public Node releaseNode(String id) {
        synchronized (retainedNodes) {
            return retainedNodes.remove(id);
        }
    }

    /**
     * This method returns the node associated, for this thread of execution, identified
     * by the supplied id.
     *
     * @param id The identifier used to identify the node
     * @return The node, or null if not found
     */
    public Node retrieveNode(String id) {
        synchronized (retainedNodes) {
            return retainedNodes.get(id);
        }
    }

    /**
     * This method returns the un-completed correlation ids list.
     *
     * @return The uncompleted correlation ids
     */
    public List<String> getUncompletedCorrelationIds() {
        return uncompletedCorrelationIds;
    }

    /**
     * This method initiates suppression of any child node.
     */
    public void suppress() {
        this.suppress = true;
    }

    /**
     * This method returns whether the fragment is in suppression
     * mode.
     *
     * @return Whether fragment is being suppressed
     */
    public boolean isSuppressed() {
        return suppress;
    }

    /**
     * This method initialises the in data buffer.
     *
     * @param hashCode The hash code
     */
    public void initInBuffer(int hashCode) {
        inHashCode = hashCode;
        inStream = new ByteArrayOutputStream();
    }

    /**
     * This method determines if the in data buffer is active.
     *
     * @param hashCode The hash code, or -1 to ignore the hash code
     * @return Whether the data buffer is active
     */
    public boolean isInBufferActive(int hashCode) {
        return inStream != null && (hashCode == -1 || hashCode == inHashCode);
    }

    /**
     * This method writes data to the in buffer.
     *
     * @param hashCode The hash code, or -1 to ignore the hash code
     * @param b The bytes
     * @param offset The offset
     * @param len The length
     */
    public void writeInData(int hashCode, byte[] b, int offset, int len) {
        if (inStream != null && (hashCode == -1 || hashCode == inHashCode)) {
            inStream.write(b, offset, len);
        }
    }

    /**
     * This method returns the data associated with the in
     * buffer and resets the buffer to be inactive.
     *
     * @param hashCode The hash code, or -1 to ignore the hash code
     * @return The data
     */
    public byte[] getInData(int hashCode) {
        if (inStream != null && (hashCode == -1 || hashCode == inHashCode)) {
            try {
                inStream.close();
            } catch (IOException e) {
                log.severe("Failed to close in data stream: " + e);
            }
            byte[] b = inStream.toByteArray();
            inStream = null;
            return b;
        }
        return null;
    }

    /**
     * This method initialises the out data buffer.
     *
     * @param hashCode The hash code
     */
    public void initOutBuffer(int hashCode) {
        outHashCode = hashCode;
        outStream = new ByteArrayOutputStream();
    }

    /**
     * This method determines if the out data buffer is active.
     *
     * @param hashCode The hash code, or -1 to ignore the hash code
     * @return Whether the data buffer is active
     */
    public boolean isOutBufferActive(int hashCode) {
        return outStream != null && (hashCode == -1 || hashCode == outHashCode);
    }

    /**
     * This method writes data to the out buffer.
     *
     * @param hashCode The hash code, or -1 to ignore the hash code
     * @param b The bytes
     * @param offset The offset
     * @param len The length
     */
    public void writeOutData(int hashCode, byte[] b, int offset, int len) {
        if (outStream != null && (hashCode == -1 || hashCode == outHashCode)) {
            outStream.write(b, offset, len);
        }
    }

    /**
     * This method returns the data associated with the out
     * buffer and resets the buffer to be inactive.
     *
     * @param hashCode The hash code, or -1 to ignore the hash code
     * @return The data
     */
    public byte[] getOutData(int hashCode) {
        if (outStream != null && (hashCode == -1 || hashCode == outHashCode)) {
            try {
                outStream.close();
            } catch (IOException e) {
                log.severe("Failed to close out data stream: " + e);
            }
            byte[] b = outStream.toByteArray();
            outStream = null;
            return b;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("Fragment builder: current btxn=[");
        info.append(businessTransaction);
        info.append("] complete=");
        info.append(isComplete());
        info.append(" unlinkedIds=");
        info.append(getUncompletedCorrelationIds());
        info.append(" stack=\r\n");

        synchronized (nodeStack) {
            for (Node node : nodeStack) {
                info.append("         node: ");
                info.append(node);
                info.append("\r\n");
            }
        }

        return (info.toString());
    }
}
