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
package org.hawkular.btm.processor.responsetime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.InteractionNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.events.NodeDetails;
import org.hawkular.btm.server.api.task.Processor;

/**
 * This class represents the node details deriver.
 *
 * @author gbrown
 */
public class NodeDetailsDeriver implements Processor<BusinessTransaction, NodeDetails> {

    private static final Logger log = Logger.getLogger(NodeDetailsDeriver.class.getName());

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public NodeDetails processSingle(BusinessTransaction item) throws Exception {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.Object)
     */
    @Override
    public List<NodeDetails> processMultiple(BusinessTransaction item) throws Exception {
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
     * @param btxn The business transaction
     * @param baseTime The base time, in nanoseconds, for the business transaction
     * @param nodes The nodes
     * @param rts The list of node details
     */
    protected void deriveNodeDetails(BusinessTransaction btxn, long baseTime,
            List<Node> nodes, List<NodeDetails> rts) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            long diffns = n.getBaseTime() - baseTime;
            long diffms = TimeUnit.MILLISECONDS.convert(diffns, TimeUnit.NANOSECONDS);

            NodeDetails nd = new NodeDetails();
            nd.setId(btxn.getId() + "-" + rts.size());
            nd.setBusinessTransaction(btxn.getName());
            nd.setCorrelationIds(n.getCorrelationIds());
            nd.setDetails(n.getDetails());
            nd.setElapsed(n.getDuration());

            long childElapsed = 0;
            if (n.containerNode()) {
                for (int j = 0; j < ((ContainerNode) n).getNodes().size(); j++) {
                    childElapsed += ((ContainerNode) n).getNodes().get(j).getDuration();
                }
            }
            nd.setActual(n.getDuration()-childElapsed);

            if (n.getType() == NodeType.Component) {
                nd.setComponentType(((Component) n).getComponentType());
                nd.setOperation(((Component) n).getOperation());
            } else {
                nd.setComponentType(n.getType().name());
            }

            if (n.getFault() != null && n.getFault().trim().length() > 0) {
                nd.setFault(n.getFault());
            }

            if (btxn.getHostName() != null && btxn.getHostName().trim().length() > 0) {
                nd.setHostName(btxn.getHostName());
            }

            nd.setProperties(btxn.getProperties());
            nd.setTimestamp(btxn.getStartTime() + diffms);
            nd.setType(n.getType());
            nd.setUri(n.getUri());

            rts.add(nd);

            if (n.interactionNode()) {
                deriveNodeDetails(btxn, baseTime, ((InteractionNode) n).getNodes(), rts);
            }
        }
    }
}
