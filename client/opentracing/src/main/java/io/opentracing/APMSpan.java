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
package io.opentracing;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.utils.TimeUtil;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.client.api.reporter.TraceReporter;
import org.hawkular.apm.client.opentracing.NodeBuilder;
import org.hawkular.apm.client.opentracing.TraceContext;

import io.opentracing.AbstractSpanBuilder.Reference;

/**
 * The APM span.
 *
 * @author gbrown
 */
public class APMSpan extends AbstractSpan {

    private static final Logger log = Logger.getLogger(APMSpan.class.getName());

    private TraceContext traceContext;

    private NodeBuilder nodeBuilder;
    private String nodePath;

    private String interactionId;

    /**
     * @param builder The span builder
     * @param reporter The trace reporter
     */
    public APMSpan(APMSpanBuilder builder, TraceReporter reporter) {
        super(builder.operationName, builder.start);

        init(builder, reporter);
    }

    protected void init(APMSpanBuilder builder, TraceReporter reporter) {
        // Check references
        for (Reference ref : builder.references) {
            if (ref.getReferenceType() == References.CHILD_OF) {
                initChildOf(builder, reporter, ref);

            } else if (ref.getReferenceType() == References.FOLLOWS_FROM) {
                initFollowsFrom(builder, reporter, ref);
            }

            if (nodeBuilder != null) {
                break;
            }
        }

        // If no nodebuilder established based on reference information, then create a new
        // one and trace context
        if (nodeBuilder == null) {
            initTopLevelState(this, reporter);
        }

        // Initialise node path
        nodePath = nodeBuilder.getNodePath();

        traceContext.startProcessingNode();
    }

    /**
     * This method initialises the node builder and trace context for a top level
     * trace fragment.
     *
     * @param topSpan The top level span in the trace
     * @param reporter The trace reporter
     */
    protected void initTopLevelState(APMSpan topSpan, TraceReporter reporter) {
        nodeBuilder = new NodeBuilder();
        traceContext = new TraceContext(topSpan, nodeBuilder, reporter);
    }

    /**
     * This method initialises the span based on a 'child-of' relationship.
     *
     * @param builder The span builder
     * @param reporter The trace reporter
     * @param ref The 'child-of' relationship
     */
    protected void initChildOf(APMSpanBuilder builder, TraceReporter reporter, Reference ref) {
        // If referring to a APMSpan, then create a direct parent relationship
        // to the span
        if (ref.getReferredTo() instanceof APMSpan) {
            APMSpan parent = (APMSpan) ref.getReferredTo();

            // TODO: Will need to protect against parent's trace already
            // being reported - in which case will need to create as
            // separate trace fragment with a "CausedBy" correlation
            // to the parent node.

            if (parent.getNodeBuilder() != null) {
                nodeBuilder = new NodeBuilder(parent.getNodeBuilder());
                traceContext = parent.traceContext;

                // As it is not possible to know if a tag has been set after span
                // creation, we use this situation to check if the parent span
                // has the 'transaction.name' specified, to set on the trace
                // context. This is required in case a child span is used to invoke
                // another service (and needs to propagate the transaction
                // name).
                if (parent.getTags().containsKey(Constants.PROP_TRANSACTION_NAME)
                        && traceContext.getBusinessTransaction() == null) {
                    traceContext.setBusinessTransaction(
                            parent.getTags().get(Constants.PROP_TRANSACTION_NAME).toString());
                }
            }

            // If APMSpanBuilder, then implies that the referred to details
            // represent an id passed by a client, and therefore we are
            // creating the 'server' span
        } else if (ref.getReferredTo() instanceof APMSpanBuilder) {
            APMSpanBuilder parentBuilder = (APMSpanBuilder) ref.getReferredTo();

            initTopLevelState(this, reporter);

            // Check for passed state
            if (parentBuilder.getState().containsKey(Constants.HAWKULAR_APM_ID)) {
                setInteractionId(parentBuilder.getState().get(Constants.HAWKULAR_APM_ID).toString(),
                        NodeType.Consumer);
                
                if (parentBuilder.getState().containsKey(Constants.HAWKULAR_APM_TRACEID)) {
                    traceContext.setTraceId(
                            parentBuilder.getState().get(Constants.HAWKULAR_APM_TRACEID).toString());
                } else {
                    log.severe("Trace id has not been propagated");
                }
                if (parentBuilder.getState().containsKey(Constants.HAWKULAR_APM_TXN)) {
                    traceContext.setBusinessTransaction(
                            parentBuilder.getState().get(Constants.HAWKULAR_APM_TXN).toString());
                }
                if (parentBuilder.getState().containsKey(Constants.HAWKULAR_APM_LEVEL)) {
                    traceContext.setReportingLevel(
                            parentBuilder.getState().get(Constants.HAWKULAR_APM_LEVEL).toString());
                }
            } else {
                // Assume top level consumer, even though no state was provider, as span context
                // as passed using a 'child of' relationship
                getNodeBuilder().setNodeType(NodeType.Consumer);
            }
        } else {
            log.severe("Unknown parent type = " + ref.getReferredTo());
        }
    }

    /**
     * This method initialises the span based on a 'follows-from' relationship.
     *
     * @param builder The span builder
     * @param reporter The trace reporter
     * @param ref The 'follows-from' relationship
     */
    protected void initFollowsFrom(APMSpanBuilder builder, TraceReporter reporter, Reference ref) {

        // This reference refers to another span within the same process, but as part of a different
        // trace
        if (ref.getReferredTo() instanceof APMSpan) {
            APMSpan referenced = (APMSpan) ref.getReferredTo();

            initTopLevelState(referenced.getTraceContext().getTopSpan(), reporter);

            // Top level node in spawned fragment should be a Consumer with correlation id
            // referencing back to the 'spawned' node
            String nodeId = referenced.getNodePath();
            getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.CausedBy, nodeId));
            getNodeBuilder().setNodeType(NodeType.Consumer);
            getNodeBuilder().setEndpointType(null);

            EndpointRef epref = referenced.getTraceContext().getSourceEndpoint();
            getNodeBuilder().setUri(epref.getUri());
            getNodeBuilder().setOperation(epref.getOperation());

            getNodeBuilder().setTimestamp(TimeUtil.toMicros(builder.start));

            // Create new node builder for the actual span, as a child of the 'Consumer' that
            // is providing the link back to the referenced node
            nodeBuilder = new NodeBuilder(getNodeBuilder());

            // Propagate trace id, business transaction name and reporting level as creating a
            // separate trace fragment to represent the 'follows from' activity
            traceContext.setTraceId(referenced.getTraceContext().getTraceId());
            traceContext.setBusinessTransaction(referenced.getTraceContext().getBusinessTransaction());
            traceContext.setReportingLevel(referenced.getTraceContext().getReportingLevel());
        }
    }

    protected void setInteractionId(String id, NodeType nodeType) {
        interactionId = id;
        getNodeBuilder().setNodeType(nodeType);
        getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.Interaction, id));
    }

    protected String getInteractionId() {
        return interactionId;
    }

    protected String getNodePath() {
        return nodePath;
    }

    @Override
    public void finish() {
        if (!isCompleted()) {
            super.finish();
            completeNode();
        }
    }

    @Override
    public void finish(long finishMicros) {
        if (!isCompleted()) {
            super.finish(finishMicros);
            completeNode();
        }
    }

    /**
     * This method will complete the information associated with the node builder
     * in preparation for the trace fragment being built (once all nodes have
     * been completed).
     */
    private void completeNode() {
        if (nodeBuilder == null) {
            return;
        }
        nodeBuilder.setOperation(getOperationName());
        nodeBuilder.setTimestamp(TimeUtil.toMicros(getStart()));
        nodeBuilder.setDuration(TimeUnit.NANOSECONDS.toMicros(getDuration().toNanos()));

        // Process the span to initialise the node
        traceContext.getNodeProcessors().forEach(np -> np.process(traceContext, this, nodeBuilder));

        traceContext.endProcessingNode();
        nodeBuilder = null;
    }

    /**
     * Determine if span has been completed.
     *
     * @return Whether the span has been completed
     */
    private boolean isCompleted() {
        return nodeBuilder == null;
    }

    /**
     * This method returns the node builder associated with the span.
     *
     * @return The node builder
     */
    protected NodeBuilder getNodeBuilder() {
        return nodeBuilder;
    }

    /**
     * This method returns the trace context associated with the span.
     *
     * @return The trace context
     */
    protected TraceContext getTraceContext() {
        return traceContext;
    }

}
