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
package org.hawkular.apm.processor.nodedetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public NodeDetails processOneToOne(String tenantId, Trace item) throws Exception {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processMultiple(java.lang.Object)
     */
    @Override
    public List<NodeDetails> processOneToMany(String tenantId, Trace item) throws Exception {
        List<NodeDetails> ret = new ArrayList<NodeDetails>();

        long baseTime = 0;
        if (!item.getNodes().isEmpty()) {
            baseTime = item.getNodes().get(0).getBaseTime();
        }

        deriveNodeDetails(item, baseTime, item.getNodes(), ret);

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
     * @param baseTime The base time, in nanoseconds, for the trace
     * @param nodes The nodes
     * @param rts The list of node details
     */
    protected void deriveNodeDetails(Trace trace, long baseTime,
            List<Node> nodes, List<NodeDetails> rts) {
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
                long diffns = n.getBaseTime() - baseTime;
                long diffms = TimeUnit.MILLISECONDS.convert(diffns, TimeUnit.NANOSECONDS);

                NodeDetails nd = new NodeDetails();
                nd.setId(trace.getId() + "-" + rts.size());
                nd.setBusinessTransaction(trace.getBusinessTransaction());
                nd.setCorrelationIds(n.getCorrelationIds());
                nd.setDetails(n.getDetails());
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

                if (n.getFault() != null && !n.getFault().trim().isEmpty()) {
                    nd.setFault(n.getFault());
                }

                if (trace.getHostName() != null && !trace.getHostName().trim().isEmpty()) {
                    nd.setHostName(trace.getHostName());
                }

                if (trace.getPrincipal() != null && !trace.getPrincipal().trim().isEmpty()) {
                    nd.setPrincipal(trace.getPrincipal());
                }

                nd.setProperties(trace.getProperties());
                nd.setTimestamp(trace.getStartTime() + diffms);
                nd.setType(n.getType());
                nd.setUri(n.getUri());
                nd.setOperation(n.getOperation());

                rts.add(nd);
            }

            if (!ignoreChildNodes && n.interactionNode()) {
                deriveNodeDetails(trace, baseTime, ((InteractionNode) n).getNodes(), rts);
            }
        }
    }
}
