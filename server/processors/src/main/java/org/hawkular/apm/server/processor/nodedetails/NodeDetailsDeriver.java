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
package org.hawkular.apm.server.processor.nodedetails;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.InteractionNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the node details deriver.
 *
 * @author gbrown
 */
public class NodeDetailsDeriver extends AbstractProcessor<Trace, NodeDetails> {

    private static final Logger log = Logger.getLogger(NodeDetailsDeriver.class.getName());

    /**
     * The default constructor.
     */
    public NodeDetailsDeriver() {
        super(ProcessorType.OneToMany);
    }

    @Override
    public List<NodeDetails> processOneToMany(String tenantId, Trace item) throws RetryAttemptException {
        List<NodeDetails> ret = new ArrayList<NodeDetails>();

        deriveNodeDetails(item, item.getNodes(), ret, true);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("NodeDetailsDeriver [" + ret.size() + "] ret=" + ret);
        }

        return ret;
    }

    /**
     * This method recursively derives the node details metrics for the supplied
     * nodes.
     *
     * @param trace The trace
     * @param nodes The nodes
     * @param rts The list of node details
     * @param initial Whether the first node in the list is the initial node
     */
    protected void deriveNodeDetails(Trace trace, List<Node> nodes, List<NodeDetails> rts, boolean initial) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);

            // If consumer or producer, check that endpoint type has been set,
            // otherwise indicates internal communication between spawned
            // fragments, which should not be recorded as nodes, as they will
            // distort derived statistics. See HWKBTM-434.
            boolean ignoreNode = false;
            boolean ignoreChildNodes = false;
            if (n.getClass() == Consumer.class && ((Consumer) n).getEndpointType() == null) {
                ignoreNode = true;
            } else if (n.getClass() == Producer.class && ((Producer) n).getEndpointType() == null) {
                ignoreNode = true;
                ignoreChildNodes = true;
            }

            if (!ignoreNode) {
                NodeDetails nd = new NodeDetails();
                nd.setId(UUID.randomUUID().toString());
                nd.setTraceId(trace.getTraceId());
                nd.setFragmentId(trace.getFragmentId());
                nd.setTransaction(trace.getTransaction());
                nd.setCorrelationIds(n.getCorrelationIds());
                nd.setElapsed(n.getDuration());

                long childElapsed = 0;
                if (n.containerNode()) {
                    for (int j = 0; j < ((ContainerNode) n).getNodes().size(); j++) {
                        childElapsed += ((ContainerNode) n).getNodes().get(j).getDuration();
                    }
                }
                nd.setActual(n.getDuration() - childElapsed);

                if (n.getType() == NodeType.Component) {
                    nd.setComponentType(((Component) n).getComponentType());
                } else {
                    nd.setComponentType(n.getType().name());
                }

                if (trace.getHostName() != null && !trace.getHostName().trim().isEmpty()) {
                    nd.setHostName(trace.getHostName());
                }

                nd.setProperties(trace.allProperties());
                nd.setTimestamp(n.getTimestamp());
                nd.setType(n.getType());
                nd.setUri(n.getUri());
                nd.setOperation(n.getOperation());

                nd.setInitial(initial);
                initial = false;

                rts.add(nd);
            }

            if (!ignoreChildNodes && n.interactionNode()) {
                deriveNodeDetails(trace, ((InteractionNode) n).getNodes(), rts, initial);
            }
        }
    }
}
