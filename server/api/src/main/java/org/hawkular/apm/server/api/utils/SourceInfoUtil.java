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
package org.hawkular.apm.server.api.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanDeriverUtil;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;

/**
 * This class represents the capability for initialising the source information.
 *
 * @author gbrown
 */
public class SourceInfoUtil {

    private static final Logger log = Logger.getLogger(SourceInfoUtil.class.getName());

    private SourceInfoUtil() {
    }

    /**
     * This method gets the source information associated with the supplied
     * traces.
     *
     * @param tenantId The tenant id
     * @param items The trace instances
     * @return The source info
     * @throws RetryAttemptException Failed to initialise source information
     */
    public static List<SourceInfo> getSourceInfo(String tenantId, List<Trace> items)
                                throws RetryAttemptException {
        List<SourceInfo> sourceInfoList = new ArrayList<SourceInfo>();

        int curpos=0;

        // This method initialises the deriver with a list of trace fragments
        // that will need to be referenced when correlating a consumer with a producer
        for (int i = 0; i < items.size(); i++) {

            // Need to check for Producer nodes
            Trace trace = items.get(i);
            StringBuffer nodeId = new StringBuffer(trace.getFragmentId());

            for (int j = 0; j < trace.getNodes().size(); j++) {
                Node node = trace.getNodes().get(j);
                int len = nodeId.length();

                initialiseSourceInfo(sourceInfoList, tenantId, trace, nodeId, j,
                        node);

                // Trim the node id for use with next node
                nodeId.delete(len, nodeId.length());
            }

            // Apply origin information to the source info
            EndpointRef ep = EndpointUtil.getSourceEndpoint(trace);
            for (int j=curpos; j < sourceInfoList.size(); j++) {
                SourceInfo si = sourceInfoList.get(j);
                si.setEndpoint(ep);
            }

            curpos = sourceInfoList.size();
        }

        return sourceInfoList;
    }

    /**
     * This method initialises an individual node within a trace.
     *
     * @param sourceInfoList The source info list
     * @param tenantId The tenant id
     * @param trace The trace
     * @param parentNodeId The parent node id
     * @param node The node
     */
    protected static void initialiseSourceInfo(List<SourceInfo> sourceInfoList, String tenantId,
            Trace trace, StringBuffer parentNodeId, int pos, Node node) {

        SourceInfo si = new SourceInfo();

        parentNodeId.append(':');
        parentNodeId.append(pos);

        si.setId(parentNodeId.toString());

        si.setTimestamp(node.getTimestamp());
        si.setDuration(node.getDuration());
        si.setTraceId(trace.getTraceId());
        si.setFragmentId(trace.getFragmentId());
        si.setHostName(trace.getHostName());
        si.setHostAddress(trace.getHostAddress());
        si.setMultipleConsumers(true);              // Multiple links could reference same node
        si.setProperties(node.getProperties());     // Just reference, to avoid unnecessary copying

        // TODO: HWKBTM-348: Should be configurable based on the wait interval plus
        // some margin of error - primarily for cases where a job scheduler
        // is used. If direct communications, then only need to cater for
        // latency.

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Adding source information for node id=" + si.getId() + " si=" + si);
        }
        sourceInfoList.add(si);

        // If node is a Producer, then check if other correlation ids are available
        if (node.getClass() == Producer.class) {
            List<CorrelationIdentifier> cids = node.findCorrelationIds(Scope.Interaction, Scope.ControlFlow);
            if (!cids.isEmpty()) {
                for (int i = 0; i < cids.size(); i++) {
                    CorrelationIdentifier cid = cids.get(i);
                    SourceInfo copy = new SourceInfo(si);
                    copy.setId(cid.getValue());
                    copy.setMultipleConsumers(((Producer)node).multipleConsumers());

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Extra source information for scope=" + cid.getScope()
                            + " id=" + copy.getId() + " si=" + copy);
                    }
                    sourceInfoList.add(copy);
                }
            }
        }

        if (node instanceof ContainerNode) {
            int nodeIdLen = parentNodeId.length();

            for (int j = 0; j < ((ContainerNode) node).getNodes().size(); j++) {
                initialiseSourceInfo(sourceInfoList, tenantId, trace, parentNodeId, j,
                        ((ContainerNode) node).getNodes().get(j));

                // Restore parent node id
                parentNodeId.delete(nodeIdLen, parentNodeId.length());
            }
        }
    }

    /**
     * This method identifies the root or enclosing server span that contains the
     * supplied client span.
     *
     * @param tenantId The tenant id
     * @param span The client span
     * @param spanCache The span cache
     * @return The root or enclosing server span, or null if not found
     */
    protected static Span findRootOrServerSpan(String tenantId, Span span, SpanCache spanCache) {
        while (span != null &&
                !span.serverSpan() && !span.topLevelSpan()) {
            span = spanCache.get(tenantId, span.getParentId());
        }
        return span;
    }

    /**
     * This method attempts to derive the Source Information for the supplied server
     * span. If the information is not available, then a null will be returned, which
     * can be used to trigger a retry attempt if appropriate.
     *
     * @param tenantId The tenant id
     * @param serverSpan The server span
     * @param spanCache The cache
     * @return The source information, or null if not found
     */
    public static SourceInfo getSourceInfo(String tenantId, Span serverSpan, SpanCache spanCache) {
        String clientSpanId = SpanUniqueIdGenerator.getClientId(serverSpan.getId());
        if (spanCache != null && clientSpanId != null) {
            Span clientSpan = spanCache.get(tenantId, clientSpanId);

            // Work up span hierarchy until find a server span, or top level span
            Span rootOrServerSpan = findRootOrServerSpan(tenantId, clientSpan, spanCache);

            if (rootOrServerSpan != null) {
                // Build source information
                SourceInfo si = new SourceInfo();

                if (clientSpan.getDuration() != null) {
                    si.setDuration(clientSpan.getDuration());
                }
                if (clientSpan.getTimestamp() != null) {
                    si.setTimestamp(clientSpan.getTimestamp());
                }
                si.setTraceId(clientSpan.getTraceId());
                si.setFragmentId(clientSpan.getId());

                si.getProperties().addAll(clientSpan.binaryAnnotationMapping().getProperties());
                si.setHostAddress(clientSpan.ipv4());

                if (clientSpan.service() != null) {
                    si.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, clientSpan.service()));
                }

                si.setId(clientSpan.getId());
                si.setMultipleConsumers(false);

                URL url = rootOrServerSpan.url();
                si.setEndpoint(new EndpointRef((url != null ? url.getPath() : null),
                        SpanDeriverUtil.deriveOperation(rootOrServerSpan), !rootOrServerSpan.serverSpan()));

                return si;
            }
        }

        return null;
    }

}
