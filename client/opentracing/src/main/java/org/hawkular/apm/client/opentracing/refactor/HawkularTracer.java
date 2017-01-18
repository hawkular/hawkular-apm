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
package org.hawkular.apm.client.opentracing.refactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.config.ReportingLevel;
import org.hawkular.apm.api.model.events.EndpointRef;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.client.api.recorder.BatchTraceRecorder;
import org.hawkular.apm.client.api.recorder.TraceRecorder;
import org.hawkular.apm.client.opentracing.TagUtil;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * @author Pavol Loffay
 */
public class HawkularTracer implements Tracer {

    private TraceRecorder traceRecorder;

    public HawkularTracer() {
        this(new BatchTraceRecorder());
    }

    public HawkularTracer(TraceRecorder recorder) {
        this.traceRecorder = recorder;
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new HawkularSpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (!(spanContext instanceof HawkularSpanContext)) {
            throw new IllegalArgumentException("SpanContext is from incompatible implementation!");
        }

        if (carrier instanceof TextMap) {
            HawkularSpanContext hawkularSpanContext = (HawkularSpanContext) spanContext;
            TextMap textMap = (TextMap) carrier;

            // TODO add to node
            String correlationId = UUID.randomUUID().toString();
            hawkularSpanContext.nodeBuilter().setNodeType(NodeType.Producer);
            hawkularSpanContext.nodeBuilter()
                    .addCorrelationId(new CorrelationIdentifier(CorrelationIdentifier.Scope.Interaction, correlationId));
            textMap.put(Constants.HAWKULAR_APM_ID, correlationId);
            textMap.put(Constants.HAWKULAR_APM_TRACEID, hawkularSpanContext.traceId());
            textMap.put(Constants.HAWKULAR_APM_TXN, hawkularSpanContext.transaction());
            textMap.put(Constants.HAWKULAR_APM_LEVEL, hawkularSpanContext.reportingLevel().toString());
        } else {
            throw new UnsupportedOperationException("Unsupported carrier");
        }
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {

        String correlationId = null;
        String traceId = null;
        String level = null;
        String transaction = null;

        if (carrier instanceof TextMap) {
            TextMap textMap = (TextMap) carrier;
            Iterator<Map.Entry<String, String>> iterator = textMap.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> carrierEntry = iterator.next();
                if (Constants.HAWKULAR_APM_TRACEID.equals(carrierEntry.getKey())) {
                    traceId = carrierEntry.getValue();
                } else if (Constants.HAWKULAR_APM_ID.equals(carrierEntry.getKey())) {
                    correlationId = carrierEntry.getValue();
                } else if (Constants.HAWKULAR_APM_LEVEL.equals(carrierEntry.getKey())) {
                    level = carrierEntry.getValue();
                } else if (Constants.HAWKULAR_APM_TXN.equals(carrierEntry.getKey())) {
                    transaction = carrierEntry.getValue();
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported carrier");
        }

        // TODO baggage
        return new ExtractedContext(Collections.emptyMap(),
                traceId, correlationId,
                transaction, level == null ? ReportingLevel.All: ReportingLevel.valueOf(level));
    }

    public void setTraceRecorder(TraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    class HawkularSpanBuilder implements SpanBuilder {

        private long startMicros;
        private List<ReferenceHolder> references = new ArrayList<>();
        private Map<String, Object> tags = new HashMap<>();
        private Map<String, String> baggage = new HashMap<>();

        private final String operationName;

        public HawkularSpanBuilder(String operationName) {
            this.operationName = operationName;
        }

        @Override
        public SpanBuilder asChildOf(Span parent) {
            return asChildOf(parent.context());
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent) {
            return addReference(References.CHILD_OF, parent);
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
            references.add(new ReferenceHolder(referenceType, referencedContext));
            addBaggage(referencedContext);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value) {
            tags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value) {
            tags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value) {
            tags.put(key, value);
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds) {
            startMicros = microseconds;
            return this;
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems() {
            return baggage.entrySet();
        }

        @Override
        public Span start() {
            // This builder is not reusable
            // https://github.com/opentracing/opentracing-java/issues/84
            if (startMicros == 0) {
                startMicros = HawkularTracer.nowMicros();
            }

            HawkularSpanContext spanContext = createSpanContext();
            HawkularSpan span = new HawkularSpan(spanContext, operationName, tags, startMicros);
            spanContext.traceFragmentState().setRootSpan(span);

            Object transaction = tags.get(Constants.PROP_TRANSACTION_NAME);
            if (transaction != null && spanContext.traceFragmentState().getTransaction() == null) {
                spanContext.traceFragmentState().setNamedTransaction(transaction.toString());
            }
            spanContext.nodeBuilter().setLinkedSpan(span);
            return span;
        }

        private void addBaggage(SpanContext spanContext) {
            spanContext.baggageItems().forEach(baggageEntry ->
                    baggage.put(baggageEntry.getKey(),  baggageEntry.getValue()));
        }

        private HawkularSpanContext createSpanContext() {

            if (references.isEmpty()) {
                TraceFragmentState traceFragmentState = TraceFragmentState.create(traceRecorder);
                return new HawkularSpanContext(Collections.emptyMap(), traceFragmentState.getRootBuilder(),
                        traceFragmentState);
            }

            /**
             * find primary reference
             */
            SpanContext primaryRef = findExtracted(references);
            if (primaryRef != null) {
                ExtractedContext extractedContext = (ExtractedContext) primaryRef;
                TraceFragmentState traceFragmentState = TraceFragmentState.createFromExtracted(traceRecorder,
                        extractedContext);

                traceFragmentState.rootBuilder.setNodeType(NodeType.Consumer)
                        .addCorrelationId(new CorrelationIdentifier(CorrelationIdentifier.Scope.Interaction,
                                extractedContext.getCorrelationId()));

                return new HawkularSpanContext(baggage, traceFragmentState.getRootBuilder(), traceFragmentState);
            }

            primaryRef = findOnlyOneReference(References.CHILD_OF, references);
            if (primaryRef != null) {
                HawkularSpanContext hawkularSpanContext = (HawkularSpanContext) primaryRef;
                NodeBuilder nodeBuilder = hawkularSpanContext.nodeBuilter().addChildNode();
                return new HawkularSpanContext(baggage, nodeBuilder, hawkularSpanContext.traceFragmentState());
            }

            primaryRef = findOnlyOneReference(References.FOLLOWS_FROM, references);
            if (primaryRef != null) {
                HawkularSpanContext parent = (HawkularSpanContext) primaryRef;

                TraceFragmentState traceFragmentState = TraceFragmentState.createFollowsFrom(traceRecorder,
                        parent.traceFragmentState());

                traceFragmentState.rootBuilder.addCorrelationId(
                        new CorrelationIdentifier(CorrelationIdentifier.Scope.CausedBy,parent.nodeBuilter().getNodePath()));

                // bit crazy
                EndpointRef epref = new EndpointRef(TagUtil.getUriPath(parent.traceFragmentState().rootSpan().tags()),
                        parent.traceFragmentState().rootSpan().operationName(),false);
                traceFragmentState.rootBuilder.setUri(epref.getUri());
                traceFragmentState.rootBuilder.setOperation(epref.getOperation());
                traceFragmentState.rootBuilder.setNodeType(NodeType.Consumer);

                NodeBuilder component = traceFragmentState.getRootBuilder().addChildNode();

                return new HawkularSpanContext(baggage, component, traceFragmentState);
            }

            //TODO join scenario
            throw new IllegalArgumentException("Parent span was not specified");
        }
    }

    private HawkularSpanContext findOnlyOneReference(String referenceType, List<ReferenceHolder> references) {
        SpanContext primaryReference = null;
        for (ReferenceHolder referenceHolder: references) {
            if (referenceType.equals(referenceHolder.getReferenceType())) {
                if (primaryReference != null) {
                    primaryReference = null;
                    break;
                }
                primaryReference = referenceHolder.getReferredTo();
            }
        }

        return (HawkularSpanContext) primaryReference;
    }

    private SpanContext findExtracted(List<ReferenceHolder> references) {
        SpanContext primaryReference = null;
        for (ReferenceHolder referenceHolder: references) {
            if (referenceHolder.getReferredTo() instanceof ExtractedContext) {
                if (primaryReference != null) {
                    primaryReference = null;
                    break;
                }
                primaryReference = referenceHolder.getReferredTo();

            }
        }

        return primaryReference;
    }

    static class ReferenceHolder {
        private final String referenceType;
        private final SpanContext referredTo;

        public ReferenceHolder(String referenceType, SpanContext referredTo) {
            this.referenceType = referenceType;
            this.referredTo = referredTo;
        }

        public String getReferenceType() {
            return referenceType;
        }

        public SpanContext getReferredTo() {
            return referredTo;
        }
    }

    public static long nowMicros() {
        return System.currentTimeMillis() * 1000;
    }
}
