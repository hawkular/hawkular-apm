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
package org.hawkular.apm.client.opentracing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.client.api.sampler.ContextSampler;

import io.opentracing.impl.APMSpan;
import io.opentracing.tag.Tags;

/**
 * This class represents the context associated with a trace instance.
 *
 * @author gbrown
 */
public class TraceContext {

    private static final Logger log = Logger.getLogger(TraceContext.class.getName());

    private Trace trace;

    private APMSpan topSpan;

    private NodeBuilder rootNode;

    private String transaction;

    private ReportingLevel reportingLevel;

    private AtomicInteger nodeCount = new AtomicInteger(0);

    private TraceRecorder recorder;
    private ContextSampler sampler;

    private static List<NodeProcessor> nodeProcessors = new ArrayList<>();

    static {
        nodeProcessors.add(new DefaultNodeProcessor());
    }

    /**
     * This constructor initialises the trace context.
     *
     * @param topSpan The top level span in the trace
     * @param rootNode The builder for the root node of the trace fragment
     * @param recorder The trace recorder
     */
    public TraceContext(APMSpan topSpan, NodeBuilder rootNode, TraceRecorder recorder, ContextSampler sampler) {
        this.topSpan = topSpan;
        this.rootNode = rootNode;
        this.recorder = recorder;
        this.sampler = sampler;

        trace = new Trace();
        trace.setFragmentId(UUID.randomUUID().toString());
        trace.setTraceId(trace.getFragmentId());
        trace.setHostName(PropertyUtil.getHostName());
        trace.setHostAddress(PropertyUtil.getHostAddress());

        // Initialise the root node's path
        rootNode.setNodePath(String.format("%s:0", trace.getFragmentId()));

    }

    /**
     * This method indicates the start of processing a node within the trace
     * instance.
     */
    public void startProcessingNode() {
        nodeCount.incrementAndGet();
    }

    /**
     * This method indicates the end of processing a node within the trace
     * instance. Once all nodes for a trace have completed being processed,
     * the trace will be reported.
     */
    public void endProcessingNode() {
        if (nodeCount.decrementAndGet() == 0 && recorder != null) {
            Node node = rootNode.build();

            trace.setTimestamp(node.getTimestamp());
            trace.setTransaction(getTransaction());
            trace.getNodes().add(node);

            if (checkForSamplingProperties(node)) {
                reportingLevel = ReportingLevel.All;
            }

            boolean sampled = sampler.isSampled(trace, reportingLevel);
            if (sampled && reportingLevel == null) {
                reportingLevel = ReportingLevel.All;
            }

            if (sampled) {
                recorder.record(trace);
            }
        }
    }

    /**
     * This method returns the trace id.
     *
     * @return The trace id
     */
    public String getTraceId() {
        return trace.getTraceId();
    }

    /**
     * This method sets the trace id.
     *
     * @param traceId The trace id
     */
    public void setTraceId(String traceId) {
        trace.setTraceId(traceId);
    }

    /**
     * @return  transaction the transaction to set
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * @param transaction the transaction to set
     */
    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    /**
     * @return the reportingLevel
     */
    public ReportingLevel getReportingLevel() {
        return reportingLevel;
    }

    /**
     * @param reportingLevel the reportingLevel to set
     */
    public void setReportingLevel(ReportingLevel reportingLevel) {
        this.reportingLevel = reportingLevel;
    }

    /**
     * This method returns the node processors.
     *
     * @return The node processors
     */
    public List<NodeProcessor> getNodeProcessors() {
        return nodeProcessors;
    }

    /**
     * This method returns the top span associated with the traces involved in the current
     * service invocation.
     *
     * @return The top span
     */
    public APMSpan getTopSpan() {
        return topSpan;
    }

    /**
     * This method returns the source endpoint for the root trace fragment generated
     * by the service invocation.
     *
     * @return The source endpoint
     */
    public EndpointRef getSourceEndpoint() {
        return new EndpointRef(TagUtil.getUriPath(topSpan.getTags()), topSpan.getOperationName(), false);
    }

    /**
     * Initialise the trace state from the supplied state.
     *
     * @param state The propagated state
     */
    public void initTraceState(Map<String, Object> state) {
        Object traceId = state.get(Constants.HAWKULAR_APM_TRACEID);
        Object transaction = state.get(Constants.HAWKULAR_APM_TXN);
        Object level = state.get(Constants.HAWKULAR_APM_LEVEL);
        if (traceId != null) {
            setTraceId(traceId.toString());
        } else {
            log.severe("Trace id has not been propagated");
        }
        if (transaction != null) {
            setTransaction(transaction.toString());
        }
        if (level != null) {
            setReportingLevel(ReportingLevel.valueOf(level.toString()));
        }
    }

    /**
     * This method is looking for sampling tag in nodes properties to override current sampling.
     *
     * @param node It sh
     * @return boolean whether trace should be sampled or not
     */
    private static boolean checkForSamplingProperties(Node node) {
        Set<Property> samplingProperties = node instanceof ContainerNode ?
                ((ContainerNode) node).getPropertiesIncludingDescendants(Tags.SAMPLING_PRIORITY.getKey()) :
                node.getProperties(Tags.SAMPLING_PRIORITY.getKey());

        for (Property prop: samplingProperties) {
            int priority = 0;
            try {
                priority = Integer.parseInt(prop.getValue());
            } catch (NumberFormatException ex) {
                // continue on error
            }

            if (priority > 0) {
                return true;
            }
        }

        return false;
    }
}
