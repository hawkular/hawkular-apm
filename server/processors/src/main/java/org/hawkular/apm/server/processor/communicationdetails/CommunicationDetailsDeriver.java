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
package org.hawkular.apm.server.processor.communicationdetails;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SourceInfoCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.SourceInfoUtil;

/**
 * This class represents the communication details deriver.
 *
 * @author gbrown
 */
public class CommunicationDetailsDeriver extends AbstractProcessor<Trace, CommunicationDetails> {

    private static final Logger log = Logger.getLogger(CommunicationDetailsDeriver.class.getName());

    @Inject
    private SourceInfoCache sourceInfoCache;

    /**
     * The default constructor.
     */
    public CommunicationDetailsDeriver() {
        super(ProcessorType.OneToOne);
    }

    /**
     * @return the sourceInfoCache
     */
    public SourceInfoCache getSourceInfoCache() {
        return sourceInfoCache;
    }

    /**
     * @param sourceInfoCache the sourceInfoCache to set
     */
    public void setSourceInfoCache(SourceInfoCache sourceInfoCache) {
        this.sourceInfoCache = sourceInfoCache;
    }

    @Override
    public void initialise(String tenantId, List<Trace> items) throws RetryAttemptException {
        List<SourceInfo> sourceInfoList = SourceInfoUtil.getSourceInfo(tenantId, items);

        try {
            sourceInfoCache.store(tenantId, sourceInfoList);
        } catch (CacheException e) {
            throw new RetryAttemptException(e);
        }
    }

    @Override
    public CommunicationDetails processOneToOne(String tenantId, Trace item) throws RetryAttemptException {
        CommunicationDetails ret = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Derive communication details for trace fragment: " + item);
        }

        // Check if trace has a Consumer top level node with a correlation id
        if (item.getNodes().size() == 1 && item.getNodes().get(0).getClass() == Consumer.class) {
            Consumer consumer = (Consumer) item.getNodes().get(0);
            List<CorrelationIdentifier> cids = consumer.getCorrelationIds();
            if (!cids.isEmpty()) {
                String lastId=null;

                for (int i = 0; ret == null && i < cids.size(); i++) {
                    String id = cids.get(i).getValue();
                    SourceInfo si = sourceInfoCache.get(tenantId, id);
                    if (si != null) {
                        ret = new CommunicationDetails();
                        ret.setId(UUID.randomUUID().toString());
                        ret.setLinkId(id);
                        ret.setTransaction(item.getTransaction());

                        ret.setSource(si.getEndpoint().toString());

                        ret.setTarget(EndpointUtil.encodeEndpoint(consumer.getUri(),
                                consumer.getOperation()));

                        ret.setLatency(calculateLatency(si, item, consumer));

                        ret.setSourceDuration(si.getDuration());
                        ret.setTargetDuration(consumer.getDuration());

                        ret.setMultiConsumer(si.isMultipleConsumers());
                        ret.setInternal(consumer.getEndpointType() == null);

                        // Merge properties from consumer and producer
                        ret.getProperties().addAll(consumer.getProperties());
                        ret.getProperties().addAll(si.getProperties());

                        ret.setTraceId(si.getTraceId());
                        ret.setSourceFragmentId(si.getFragmentId());
                        ret.setSourceHostName(si.getHostName());
                        ret.setSourceHostAddress(si.getHostAddress());
                        ret.setTargetFragmentId(item.getFragmentId());
                        ret.setTargetHostName(item.getHostName());
                        ret.setTargetHostAddress(item.getHostAddress());
                        ret.setTargetFragmentDuration(item.calculateDuration());

                        // HWKBTM-349 Deal with timestamp and offset. Currently
                        // just copying timestamp as-is from producer fragment
                        ret.setTimestamp(si.getTimestamp());

                        long timestampOffset = item.getTimestamp() - si.getTimestamp() - ret.getLatency();

                        ret.setTimestampOffset(timestampOffset);

                        // Build outbound information
                        StringBuilder nodeId = new StringBuilder(item.getFragmentId());
                        nodeId.append(":0");

                        initialiseOutbound(consumer, item.getNodes().get(0).getTimestamp(), ret, nodeId);
                    } else {
                        lastId = id;
                    }
                }
                if (ret == null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("WARNING: Producer information not available [last id checked = " + lastId + "]");
                    }

                    // Need to retry, as the source information is not currently available
                    throw new RetryAttemptException("Producer information not available [last id checked = "
                                            + lastId + "]");
                }
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Derived communication details: " + ret);
        }

        return ret;
    }

    protected static long calculateLatency(SourceInfo si, Trace trace, Consumer consumer) {
        long latency = 0;

        if (!si.isMultipleConsumers()) {
            long diff = si.getDuration() - consumer.getDuration();
            if (diff > 0) {
                // Latency is being calculated as half the difference between the producer and consumer
                // durations - so is an approximation of the latency based on the assumption that
                // the request and response delivery is the same. This may not always be the case,
                // but is the best approximation in an environment where clock synchronization between
                // remote servers cannot be guaranteed.
                latency = diff >> 1;
            } else if (diff < 0) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("WARNING: Negative latency based on source/target duration, consumer trace = " + trace);
                }
                latency = calculateTimestampLatency(si, trace);
            }
        } else {
            latency = calculateTimestampLatency(si, trace);
        }

        return latency;
    }

    private static long calculateTimestampLatency(SourceInfo si, Trace trace) {
        long latency = 0;

        latency = trace.getTimestamp() - si.getTimestamp();
        if (latency < 0) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("WARNING: Negative latency based on timestamps, consumer trace = " + trace);
            }
            latency = 0;
        }

        return latency;
    }

    /**
     * This method initialises the outbound information from the consumer's nodes in the supplied
     * communication details.
     *
     * @param n The node
     * @param baseTime The fragment's base time (ns)
     * @param cd The communication details
     * @param nodeId The path id for the node
     */
    protected static void initialiseOutbound(Node n, long baseTime, CommunicationDetails cd,
            StringBuilder nodeId) {
        CommunicationDetails.Outbound ob = new CommunicationDetails.Outbound();
        ob.getLinkIds().add(nodeId.toString());
        ob.setMultiConsumer(true);
        ob.setProducerOffset(n.getTimestamp() - baseTime);
        cd.getOutbound().add(ob);

        if (n.getClass() == Producer.class) {
            ob = new CommunicationDetails.Outbound();
            for (int j = 0; j < n.getCorrelationIds().size(); j++) {
                CorrelationIdentifier ci = n.getCorrelationIds().get(j);
                if (ci.getScope() == Scope.Interaction || ci.getScope() == Scope.ControlFlow) {
                    ob.getLinkIds().add(ci.getValue());
                }
            }
            // Only record if outbound ids found
            if (!ob.getLinkIds().isEmpty()) {
                // Check if pub/sub
                ob.setMultiConsumer(((Producer) n).multipleConsumers());

                ob.setProducerOffset(n.getTimestamp() - baseTime);
                cd.getOutbound().add(ob);
            }
        } else if (n.containerNode()) {
            for (int i = 0; i < ((ContainerNode)n).getNodes().size(); i++) {
                int len = nodeId.length();
                nodeId.append(':');
                nodeId.append(i);
                initialiseOutbound(((ContainerNode) n).getNodes().get(i), baseTime, cd, nodeId);

                // Remove this child's specific path, so that next iteration will add a different path number
                nodeId.delete(len, nodeId.length());
            }
        }
    }

}
