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

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.utils.TimeUtil;
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
        
        if (!builder.references.isEmpty()) {
            initReferences(builder, reporter);
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

    protected void initReferences(APMSpanBuilder builder, TraceReporter reporter) {
        // Find primary reference
        Reference primaryRef = findPrimaryReference(builder.references);

        if (primaryRef != null) {
            // Primary reference found, so it will provide the main 'parent' relationship
            // to the existing trace instance. Other relationships will be recorded as
            // correlation identifiers.
            
            // Process references to extracted trace state
            if (primaryRef.getReferredTo() instanceof APMSpanBuilder) {
                initFromExtractedTraceState(builder, reporter, primaryRef);

            } else if (primaryRef.getReferredTo() instanceof APMSpan) {
                
                // Process references for direct ChildOf
                if (References.CHILD_OF.equals(primaryRef.getReferenceType())) {
                    initChildOf(builder, reporter, primaryRef);
                
                // Process references for direct FollowsFrom
                } else if (References.FOLLOWS_FROM.equals(primaryRef.getReferenceType())) {
                    initFollowsFrom(builder, reporter, primaryRef);
                }
            }
        } else {
            processNoPrimaryReference(builder, reporter);
        }
    }

    /**
     * This method identifies the primary 'parent' reference that should be used
     * to link the associated span to an existing trace instance.
     *
     * @param references The list of references
     * @return The primary reference, or null if one could not be determined
     */
    public static Reference findPrimaryReference(List<Reference> references) {
        List<Reference> followsFrom = references.stream()
                .filter(ref -> References.FOLLOWS_FROM.equals(ref.getReferenceType())
                        && ref.getReferredTo() instanceof APMSpan)
                .collect(Collectors.toList());

        List<Reference> childOfSpan = references.stream()
                .filter(ref -> References.CHILD_OF.equals(ref.getReferenceType())
                        && ref.getReferredTo() instanceof APMSpan)
                .collect(Collectors.toList());

        List<Reference> extractedTraceState = references.stream()
                .filter(ref -> ref.getReferredTo() instanceof APMSpanBuilder)
                .collect(Collectors.toList());

        if (!extractedTraceState.isEmpty()) {
            if (extractedTraceState.size() == 1) {
                return extractedTraceState.get(0);
            }
            return null;
        }
        if (!childOfSpan.isEmpty()) {
            if (childOfSpan.size() == 1) {
                return childOfSpan.get(0);
            }
            return null;
        }
        if (followsFrom.size() == 1) {
            return followsFrom.get(0);
        }
        return null;
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
     * This method initialises the span based on extracted trace state.
     *
     * @param builder The span builder
     * @param reporter The trace reporter
     * @param ref The reference
     */
    protected void initFromExtractedTraceState(APMSpanBuilder builder, TraceReporter reporter, Reference ref) {
        APMSpanBuilder parentBuilder = (APMSpanBuilder) ref.getReferredTo();

        initTopLevelState(this, reporter);

        // Check for passed state
        if (parentBuilder.getState().containsKey(Constants.HAWKULAR_APM_ID)) {
            setInteractionId(parentBuilder.getState().get(Constants.HAWKULAR_APM_ID).toString());
            
            traceContext.initTraceState(parentBuilder.getState());
        }

        // Assume top level consumer, even if no state was provided, as span context
        // as passed using a 'child of' relationship
        getNodeBuilder().setNodeType(NodeType.Consumer);
        
        processRemainingReferences(builder, ref);
    }

    /**
     * This method initialises the span based on a 'child-of' relationship.
     *
     * @param builder The span builder
     * @param reporter The trace reporter
     * @param ref The 'child-of' relationship
     */
    protected void initChildOf(APMSpanBuilder builder, TraceReporter reporter, Reference ref) {
        APMSpan parent = (APMSpan) ref.getReferredTo();

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
                    && traceContext.getTransaction() == null) {
                traceContext.setTransaction(
                        parent.getTags().get(Constants.PROP_TRANSACTION_NAME).toString());
            }
        }
        
        processRemainingReferences(builder, ref);
    }
    
    /**
     * This method initialises the span based on a 'follows-from' relationship.
     *
     * @param builder The span builder
     * @param reporter The trace reporter
     * @param ref The 'follows-from' relationship
     */
    protected void initFollowsFrom(APMSpanBuilder builder, TraceReporter reporter, Reference ref) {
        APMSpan referenced = (APMSpan) ref.getReferredTo();

        initTopLevelState(referenced.getTraceContext().getTopSpan(), reporter);

        // Top level node in spawned fragment should be a Consumer with correlation id
        // referencing back to the 'spawned' node
        String nodeId = referenced.getNodePath();
        getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.CausedBy, nodeId));

        // Propagate trace id, transaction name and reporting level as creating a
        // separate trace fragment to represent the 'follows from' activity
        traceContext.initTraceState(referenced.getTraceContext());

        makeInternalLink(builder);
    }

    /**
     * This method initialises the span based on there being no primary reference.
     *
     * @param builder The span builder
     * @param reporter The trace reporter
     */
    protected void processNoPrimaryReference(APMSpanBuilder builder, TraceReporter reporter) {
        // No primary reference found, so means that all references will be treated
        // as equal, to provide a join construct within a separate fragment.
        initTopLevelState(this, reporter);

        Set<String> traceIds = builder.references.stream().map(ref -> {
            if (ref.getReferredTo() instanceof APMSpan) {
                return ((APMSpan)ref.getReferredTo()).getTraceContext().getTraceId();
            } else if (ref.getReferredTo() instanceof APMSpanBuilder) {
                return ((APMSpanBuilder)ref.getReferredTo()).getState().get(Constants.HAWKULAR_APM_TRACEID).toString();
            }
            log.warning("Reference refers to an unsupported SpanContext implementation: " + ref.getReferredTo());
            return null;
        }).collect(Collectors.toSet());

        if (traceIds.size() > 0) {
            if (traceIds.size() > 1) {
                log.warning("References should all belong to the same 'trace' instance");
            }
            if (builder.references.get(0).getReferredTo() instanceof APMSpan) {
                traceContext.initTraceState(((APMSpan)builder.references.get(0).getReferredTo()).getTraceContext());
            } else if (builder.references.get(0).getReferredTo() instanceof APMSpanBuilder) {
                traceContext.initTraceState(((APMSpanBuilder)builder.references.get(0).getReferredTo()).getState());
            }
        }

        processRemainingReferences(builder, null);
        
        makeInternalLink(builder);
    }

    /**
     * This method processes the remaining references by creating appropriate correlation ids
     * against the current node.
     *
     * @param builder The span builder
     * @param primaryRef The primary reference, if null if one was not found
     */
    protected void processRemainingReferences(APMSpanBuilder builder, Reference primaryRef) {
        // Check if other references
        for (Reference ref : builder.references) {
            if (primaryRef == ref) {
                continue;
            }
            // Setup correlation ids for other references
            if (ref.getReferredTo() instanceof APMSpan) {
                APMSpan referenced = (APMSpan) ref.getReferredTo();
    
                String nodeId = referenced.getNodePath();
                getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.CausedBy, nodeId));

            } else if (ref.getReferredTo() instanceof APMSpanBuilder
                    && ((APMSpanBuilder)ref.getReferredTo()).getState().containsKey(Constants.HAWKULAR_APM_ID)) {
                getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.Interaction,
                        ((APMSpanBuilder)ref.getReferredTo()).getState().get(Constants.HAWKULAR_APM_ID).toString()));
            }
        }
    }

    /**
     * This method creates a Consumer node to link the new trace fragment to a
     * node associated with a referenced Span. This method sets the type of
     * the current node to be Consumer, initialises its URI/Operation to the
     * endpoint details of the supplied trace context, and then creates a new
     * child node upon which subsequent configuration will be performed.
     *
     * @param builder The span builder
     */
    protected void makeInternalLink(APMSpanBuilder builder) {
        getNodeBuilder().setNodeType(NodeType.Consumer);
        getNodeBuilder().setEndpointType(null);

        EndpointRef epref = getTraceContext().getSourceEndpoint();
        getNodeBuilder().setUri(epref.getUri());
        getNodeBuilder().setOperation(epref.getOperation());

        getNodeBuilder().setTimestamp(TimeUtil.toMicros(builder.start));

        // Create new node builder for the actual span, as a child of the 'Consumer' that
        // is providing the link back to the referenced node
        nodeBuilder = new NodeBuilder(getNodeBuilder());
    }

    protected void setInteractionId(String id) {
        interactionId = id;
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
