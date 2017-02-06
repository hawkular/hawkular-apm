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

package io.opentracing.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.utils.TimeUtil;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.client.api.sampler.ContextSampler;
import org.hawkular.apm.client.opentracing.NodeBuilder;
import org.hawkular.apm.client.opentracing.TraceContext;

import io.opentracing.PropagableState;
import io.opentracing.References;
import io.opentracing.impl.AbstractSpanBuilder.Reference;
import io.opentracing.tag.Tags;

/**
 * The APM span.
 *
 * @author gbrown
 */
public class APMSpan extends AbstractSpan implements PropagableState {

    private static final Logger log = Logger.getLogger(APMSpan.class.getName());

    private TraceContext traceContext;

    private NodeBuilder nodeBuilder;
    private String nodePath;

    private String interactionId;

    /**
     * @param builder  The span builder
     * @param recorder The trace recorder
     */
    public APMSpan(APMSpanBuilder builder, TraceRecorder recorder, ContextSampler sampler) {
        super(builder.operationName, builder.start);

        init(builder, recorder, sampler);
    }

    protected void init(APMSpanBuilder builder, TraceRecorder recorder, ContextSampler sampler) {

        if (!builder.references.isEmpty()) {
            initReferences(builder, recorder, sampler);
        }

        // If no nodebuilder established based on reference information, then create a new
        // one and trace context
        if (nodeBuilder == null) {
            initTopLevelState(this, recorder, sampler);
        }

        // Initialise node path
        nodePath = nodeBuilder.getNodePath();

        traceContext.startProcessingNode();
    }

    protected void initReferences(APMSpanBuilder builder, TraceRecorder recorder, ContextSampler sampler) {
        // Find primary reference
        Reference primaryRef = findPrimaryReference(builder.references);

        if (primaryRef != null) {
            // Primary reference found, so it will provide the main 'parent' relationship
            // to the existing trace instance. Other relationships will be recorded as
            // correlation identifiers.

            // Process references to extracted trace state
            if (primaryRef.getReferredTo() instanceof APMSpanBuilder) {
                initFromExtractedTraceState(builder, recorder, primaryRef, sampler);

            } else if (primaryRef.getReferredTo() instanceof APMSpan) {

                // Process references for direct ChildOf
                if (References.CHILD_OF.equals(primaryRef.getReferenceType())) {
                    initChildOf(builder, primaryRef);

                    // Process references for direct FollowsFrom
                } else if (References.FOLLOWS_FROM.equals(primaryRef.getReferenceType())) {
                    initFollowsFrom(builder, recorder, primaryRef, sampler);
                }
            }
        } else {
            processNoPrimaryReference(builder, recorder, sampler);
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
     * @param topSpan  The top level span in the trace
     * @param recorder The trace recorder
     * @param sampler The sampler
     */
    protected void initTopLevelState(APMSpan topSpan, TraceRecorder recorder, ContextSampler sampler) {
        nodeBuilder = new NodeBuilder();
        traceContext = new TraceContext(topSpan, nodeBuilder, recorder, sampler);
    }

    /**
     * This method initialises the span based on extracted trace state.
     *
     * @param builder  The span builder
     * @param recorder The trace recorder
     * @param ref      The reference
     * @param sampler The sampler
     */
    protected void initFromExtractedTraceState(APMSpanBuilder builder, TraceRecorder recorder, Reference ref, ContextSampler sampler) {
        APMSpanBuilder parentBuilder = (APMSpanBuilder) ref.getReferredTo();

        initTopLevelState(this, recorder, sampler);

        // Check for passed state
        if (parentBuilder.state().containsKey(Constants.HAWKULAR_APM_ID)) {
            setInteractionId(parentBuilder.state().get(Constants.HAWKULAR_APM_ID).toString());

            traceContext.initTraceState(parentBuilder.state());
        }

        // Assume top level consumer, even if no state was provided, as span context
        // as passed using a 'child of' relationship
        getNodeBuilder().setNodeType(NodeType.Consumer);

        processRemainingReferences(builder, ref);
    }

    /**
     * This method initialises the span based on a 'child-of' relationship.
     *
     * @param builder  The span builder
     * @param ref      The 'child-of' relationship
     */
    protected void initChildOf(APMSpanBuilder builder, Reference ref) {
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
     * @param builder  The span builder
     * @param recorder The trace recorder
     * @param ref      The 'follows-from' relationship
     * @param sampler The sampler
     */
    protected void initFollowsFrom(APMSpanBuilder builder, TraceRecorder recorder, Reference ref, ContextSampler sampler) {
        APMSpan referenced = (APMSpan) ref.getReferredTo();

        initTopLevelState(referenced.getTraceContext().getTopSpan(), recorder, sampler);

        // Top level node in spawned fragment should be a Consumer with correlation id
        // referencing back to the 'spawned' node
        String nodeId = referenced.getNodePath();
        getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.CausedBy, nodeId));

        // Propagate trace id, transaction name and reporting level as creating a
        // separate trace fragment to represent the 'follows from' activity
        traceContext.initTraceState(referenced.state());

        makeInternalLink(builder);
    }

    /**
     * This method initialises the span based on there being no primary reference.
     *
     * @param builder  The span builder
     * @param recorder The trace recorder
     * @param sampler The sampler
     */
    protected void processNoPrimaryReference(APMSpanBuilder builder, TraceRecorder recorder, ContextSampler sampler) {
        // No primary reference found, so means that all references will be treated
        // as equal, to provide a join construct within a separate fragment.
        initTopLevelState(this, recorder, sampler);

        Set<String> traceIds = builder.references.stream().map(ref -> {
            if (ref.getReferredTo() instanceof APMSpan) {
                return ((APMSpan) ref.getReferredTo()).getTraceContext().getTraceId();
            } else if (ref.getReferredTo() instanceof APMSpanBuilder) {
                return ((APMSpanBuilder) ref.getReferredTo()).state().get(Constants.HAWKULAR_APM_TRACEID).toString();
            }
            log.warning("Reference refers to an unsupported SpanContext implementation: " + ref.getReferredTo());
            return null;
        }).collect(Collectors.toSet());

        if (traceIds.size() > 0) {
            if (traceIds.size() > 1) {
                log.warning("References should all belong to the same 'trace' instance");
            }
            if (builder.references.get(0).getReferredTo() instanceof APMSpan) {
                traceContext.initTraceState(((APMSpan) builder.references.get(0).getReferredTo()).state());
            }
        }

        processRemainingReferences(builder, null);

        makeInternalLink(builder);
    }

    /**
     * This method processes the remaining references by creating appropriate correlation ids
     * against the current node.
     *
     * @param builder    The span builder
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
                    && ((APMSpanBuilder) ref.getReferredTo()).state().containsKey(Constants.HAWKULAR_APM_ID)) {
                getNodeBuilder().addCorrelationId(new CorrelationIdentifier(Scope.Interaction,
                        ((APMSpanBuilder) ref.getReferredTo()).state().get(Constants.HAWKULAR_APM_ID).toString()));
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

    @Override
    public Map<String, Object> state() {
        Map<String, Object> state = new HashMap<>();
        state.put(Constants.HAWKULAR_APM_TRACEID, traceContext.getTraceId());

        Object reportingLevelFromTags = ReportingLevel.parse(getTags().get(Tags.SAMPLING_PRIORITY.getKey()));
        if (reportingLevelFromTags != null) {
            state.put(Constants.HAWKULAR_APM_LEVEL, reportingLevelFromTags);
        } else  if (traceContext.getReportingLevel() != null) {
            state.put(Constants.HAWKULAR_APM_LEVEL, traceContext.getReportingLevel().toString());
        }

        if (traceContext.getTransaction() != null) {
            state.put(Constants.HAWKULAR_APM_TXN, traceContext.getTransaction());
        } else {
            Object transactionFromTags = getTags().get(Constants.PROP_TRANSACTION_NAME);
            if (transactionFromTags != null) {
                state.put(Constants.HAWKULAR_APM_TXN, transactionFromTags);
            }
        }
        return state;
    }
}
