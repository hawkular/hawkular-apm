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
package org.hawkular.apm.server.processor.zipkin;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanDeriverUtil;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;

/**
 * This class represents the zipkin node details deriver.
 *
 * @author gbrown
 */
public class NodeDetailsDeriver extends AbstractProcessor<Span, NodeDetails> {

    private static final Logger log = Logger.getLogger(NodeDetailsDeriver.class.getName());

    /**
     * The default constructor.
     */
    public NodeDetailsDeriver() {
        super(ProcessorType.OneToOne);
    }

    @Override
    public NodeDetails processOneToOne(String tenantId, Span item) throws RetryAttemptException {
        NodeDetails nd = createTypedNodeDetails(item);

        URL url = item.url();
        if (url != null) {
            nd.setUri(url.getPath());
        }

        if (item.getTimestamp() != null) {
            nd.setTimestamp(item.getTimestamp());
        }
        if (item.getDuration() != null) {
            nd.setElapsed(item.getDuration());
            // TODO: How to calculate actual - i.e. would need to know child times???
            nd.setActual(item.getDuration());
        }

        nd.getProperties().addAll(item.binaryAnnotationMapping().getProperties());
        nd.setHostAddress(item.ipv4());

        if (item.service() != null) {
            nd.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, item.service()));
        }

        nd.getProperties().add(new Property(Constants.PROP_FAULT, SpanDeriverUtil.deriveFault(item)));
        nd.setOperation(SpanDeriverUtil.deriveOperation(item));

        nd.setInitial(item.topLevelSpan() || item.serverSpan());

        if (log.isLoggable(Level.FINEST)) {
            log.finest("NodeDetailsDeriver ret=" + nd);
        }
        return nd;
    }

    private NodeDetails createTypedNodeDetails(Span span) {
        NodeDetails nd = new NodeDetails();
        nd.setId(span.getId());
        nd.setTraceId(span.getTraceId());

        nd.setType(NodeType.Component);
        nd.setComponentType(span.binaryAnnotationMapping().getComponentType());

        if (span.binaryAnnotationMapping().getComponentType() == null) {
            if (span.clientSpan()) {
//             Need to qualify id, as same id used for both client and server
                nd.setId(SpanUniqueIdGenerator.toUnique(span));
                nd.setType(NodeType.Producer);
                nd.setComponentType("Producer");
                nd.getCorrelationIds().add(new CorrelationIdentifier(CorrelationIdentifier.Scope.Interaction, span.getId()));
            } else if (span.serverSpan()) {
                nd.setType(NodeType.Consumer);
                nd.setComponentType("Consumer");
                nd.getCorrelationIds().add(new CorrelationIdentifier(CorrelationIdentifier.Scope.Interaction, span.getId()));
            }
        }

        return nd;
    }
}
