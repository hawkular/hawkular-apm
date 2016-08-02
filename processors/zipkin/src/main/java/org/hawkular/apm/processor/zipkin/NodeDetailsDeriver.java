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
package org.hawkular.apm.processor.zipkin;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.SpanUniqueIdGenerator;

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

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public NodeDetails processOneToOne(String tenantId, Span item) throws RetryAttemptException {

        NodeDetails nd = new NodeDetails();

        nd.setId(item.getId());

        URL url = item.url();
        if (url != null) {
            nd.setUri(url.getPath());
        }

        if (item.clientSpan()) {
            // Need to qualify id, as same id used for both client and server
            nd.setId(SpanUniqueIdGenerator.toUnique(item));
            nd.setType(NodeType.Producer);
            nd.setComponentType("Producer");
            nd.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, item.getId()));
        } else if (item.serverSpan()) {
            nd.setType(NodeType.Consumer);
            nd.setComponentType("Consumer");
            nd.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, item.getId()));
        } else {
            nd.setType(NodeType.Component);
            nd.setComponentType(item.componentType());
        }

        nd.setElapsed(item.getDuration());

        // TODO: How to calculate actual - i.e. would need to know child times???
        nd.setActual(item.getDuration());

        //ct.setIpAddress(item.getAnnotations().get(0).getEndpoint().getIpv4());

        // TODO: ADD SERVICE NAME AS PROPERTY?

        nd.setTimestamp(item.getTimestamp() / 1000);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("NodeDetailsDeriver ret=" + nd);
        }
        return nd;
    }

}
