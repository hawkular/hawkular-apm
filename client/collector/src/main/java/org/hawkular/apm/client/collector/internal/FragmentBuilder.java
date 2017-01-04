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
package org.hawkular.apm.client.collector.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * This class represents the builder for a trace fragment. NOTE: This
 * class is not thread safe as the sequence of events within a thread of execution
 * should be in sequence, and therefore should not result in any concurrent conflicts.
 *
 * @author gbrown
 */
public class FragmentBuilder {

    private static final Logger log = Logger.getLogger(FragmentBuilder.class.getName());

    private Trace trace;

    private long baseNanoseconds;

    private Stack<Node> nodeStack = new Stack<Node>();

    private Stack<Node> poppedNodes = new Stack<Node>();

    private Stack<Node> suppressedNodeStack = new Stack<Node>();

    private Map<String, Node> retainedNodes = new HashMap<String, Node>();

    private List<Node> ignoredNodes = new ArrayList<Node>();

    private Map<String,NodePlaceholder> uncompletedCorrelationIdsNodeMap = new HashMap<String,NodePlaceholder>();

    private Map<String,StateInformation> stateInformation = new HashMap<String,StateInformation>();

    private boolean suppress = false;

    private ReportingLevel level = ReportingLevel.All;

    private int inHashCode = 0;
    private ByteArrayOutputStream inStream = null;
    private int outHashCode = 0;
    private ByteArrayOutputStream outStream = null;

    private AtomicInteger threadCount = new AtomicInteger();

    {
        trace = new Trace()
                .setFragmentId(UUID.randomUUID().toString())
                .setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()))
                .setHostName(PropertyUtil.getHostName())
                .setHostAddress(PropertyUtil.getHostAddress());
        trace.setTraceId(trace.getFragmentId());
        baseNanoseconds = System.nanoTime();
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
     * This method determines if the fragment is complete
     * with the exception of ignored nodes.
     *
     * @return Whether the fragment is complete with the exception of
     *              ignored nodes
     */
    public boolean isCompleteExceptIgnoredNodes() {
        synchronized (nodeStack) {
            if (nodeStack.isEmpty() && retainedNodes.isEmpty()) {
                return true;
            } else {
                // Check that remaining nodes can be ignored
                for (int i=0; i < nodeStack.size(); i++) {
                    if (!ignoredNodes.contains(nodeStack.get(i))) {
                        return false;
                    }
                }
                for (int i=0; i < retainedNodes.size(); i++) {
                    if (!ignoredNodes.contains(retainedNodes.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    /**
     * @return the trace
     */
    public Trace getTrace() {
        return trace;
    }

    /**
     * @return the level
     */
    public ReportingLevel getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(ReportingLevel level) {
        this.level = level;
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

    protected long currentTimeMicros() {
        return trace.getTimestamp() + TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - baseNanoseconds);
    }

    /**
     * This method initialises the supplied node.
     *
     * @param node The node
     */
    protected void initNode(Node node) {
        node.setTimestamp(currentTimeMicros());
    }

    /**
     * This method finishes the supplied node.
     *
     * @param node The node
     */
    protected void finishNode(Node node) {
        node.setDuration(currentTimeMicros() - node.getTimestamp());
    }

    /**
     * This method pushes a new node into the trace
     * fragment hierarchy.
     *
     * @param node The new node
     */
    public void pushNode(Node node) {
        initNode(node);

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
                    log.finest("Pushing top level node: " + node + " for txn: " + trace);
                }
                trace.getNodes().add(node);
            } else {
                Node parent = nodeStack.peek();

                if (parent instanceof ContainerNode) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Add node: " + node + " to parent: " + parent + " in txn: " + trace);
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
     * This method pops the latest node from the trace
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
                    // If suppression parent popped, then cancel the suppress mode
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
            return uri == null || NodeUtil.isOriginalURI(node, uri);
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
     * This method associates a parent node and child position with a
     * correlation id.
     *
     * @param id The correlation id
     * @param node The parent node
     * @param position The child node position within the parent node
     */
    public void addUncompletedCorrelationId(String id, Node node, int position) {
        NodePlaceholder placeholder = new NodePlaceholder();
        placeholder.setNode(node);
        placeholder.setPosition(position);
        uncompletedCorrelationIdsNodeMap.put(id, placeholder);
    }

    /**
     * This method returns the un-completed correlation ids.
     *
     * @return The uncompleted correlation ids
     */
    public Set<String> getUncompletedCorrelationIds() {
        return uncompletedCorrelationIdsNodeMap.keySet();
    }

    /**
     * This method returns the child position associated with the
     * supplied correlation id.
     *
     * @param id The correlation id
     * @return The child node position, or -1  if unknown
     */
    public int getUncompletedCorrelationIdPosition(String id) {
        if (uncompletedCorrelationIdsNodeMap.containsKey(id)) {
            return uncompletedCorrelationIdsNodeMap.get(id).getPosition();
        }
        return -1;
    }

    /**
     * This method removes the uncompleted correlation id and its
     * associated information.
     *
     * @param id The correlation id
     * @return The node associated with the correlation id
     */
    public Node removeUncompletedCorrelationId(String id) {
        NodePlaceholder placeholder=uncompletedCorrelationIdsNodeMap.remove(id);
        if (placeholder != null) {
            return placeholder.getNode();
        }
        return null;
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
     * This method adds the current node to the list of 'ignored'
     * nodes. These nodes are irrelant when determining if the
     * transaction has completed.
     */
    public void ignoreNode() {
        Node node=getCurrentNode();
        if (node != null) {
            ignoredNodes.add(node);
        }
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
     * This method stores state information associated with the name and optional
     * context.
     *
     * @param context The optional context
     * @param name The name
     * @param value The value
     */
    public void setState(Object context, String name, Object value) {
        StateInformation si = stateInformation.get(name);
        if (si == null) {
            si = new StateInformation();
            stateInformation.put(name, si);
        }
        si.set(context, value);
    }

    /**
     * This method returns the state associated with the name and optional
     * context.
     *
     * @param context The optional context
     * @param name The name
     * @return The state, or null if not found
     */
    public Object getState(Object context, String name) {
        StateInformation si = stateInformation.get(name);
        if (si == null) {
            return null;
        }
        return si.get(context);
    }

    /**
     * @return The thread count
     */
    public int getThreadCount() {
        return threadCount.get();
    }

    /**
     * Increment the thread count.
     *
     * @return The incremented thread count
     */
    protected int incrementThreadCount() {
        return threadCount.incrementAndGet();
    }

    /**
     * Decrement the thread count.
     *
     * @return The decremented thread count
     */
    protected int decrementThreadCount() {
        return threadCount.decrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(256);
        info.append("Fragment builder: current btxn=[");
        info.append(trace);
        info.append("] complete=");
        info.append(isComplete());
        info.append(" uncompletedCorrelationIdsNodeMap=");
        info.append(uncompletedCorrelationIdsNodeMap);
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

    /**
     * This class provides information about a placeholder for
     * adding a child node to an existing parent.
     */
    public static class NodePlaceholder {

        private Node node;
        private int position = -1;

        /**
         * @return the node
         */
        public Node getNode() {
            return node;
        }

        /**
         * @param node the node to set
         */
        public void setNode(Node node) {
            this.node = node;
        }

        /**
         * @return the position
         */
        public int getPosition() {
            return position;
        }

        /**
         * @param position the position to set
         */
        public void setPosition(int position) {
            this.position = position;
        }

        @Override
        public String toString() {
            return "NodePlaceholder [node=" + node + ", position=" + position + "]";
        }

    }

    /**
     * This class provides management of context to value state.
     *
     * @author gbrown
     */
    public static class StateInformation {

        private Object noContextValue = null;
        private Map<Object,Object> contextToValueMap = new HashMap<Object,Object>();

        /**
         * This method sets the value associated with an optional context.
         *
         * @param context The optional context
         * @param value The value
         */
        public void set(Object context, Object value) {
            if (context == null) {
                noContextValue = value;
            } else {
                contextToValueMap.put(context, value);
            }
        }

        /**
         * This method retrieves the value associated with an optional
         * context.
         *
         * @param context The optional context
         * @return The value, or null if not found
         */
        public Object get(Object context) {
            if (context == null) {
                return noContextValue;
            }
            return contextToValueMap.get(context);
        }

        /**
         * This method removes the value associated with the supplied
         * optional context.
         *
         * @param context The optional context
         */
        public void remove(Object context) {
            if (context == null) {
                noContextValue = null;
            } else {
                contextToValueMap.remove(context);
            }
        }
    }
}
