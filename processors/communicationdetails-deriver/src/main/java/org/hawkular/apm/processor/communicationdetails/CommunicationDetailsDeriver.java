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
package org.hawkular.apm.processor.communicationdetails;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the communication details deriver.
 *
 * @author gbrown
 */
public class CommunicationDetailsDeriver extends AbstractProcessor<Trace, CommunicationDetails> {

    private static final Logger log = Logger.getLogger(CommunicationDetailsDeriver.class.getName());

    @Inject
    private ProducerInfoCache producerInfoCache;

    private ProducerInfoInitialiser producerInfoInitialiser;

    /**
     * The default constructor.
     */
    public CommunicationDetailsDeriver() {
        super(ProcessorType.OneToOne);
    }

    @PostConstruct
    public void init() {
        producerInfoInitialiser = new ProducerInfoInitialiser();
        producerInfoInitialiser.setProducerInfoCache(producerInfoCache);
    }

    /**
     * @return the producerInfoCache
     */
    public ProducerInfoCache getProducerInfoCache() {
        return producerInfoCache;
    }

    /**
     * @param producerInfoCache the producerInfoCache to set
     */
    public void setProducerInfoCache(ProducerInfoCache producerInfoCache) {
        this.producerInfoCache = producerInfoCache;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#initialise(java.util.List)
     */
    @Override
    public void initialise(String tenantId, List<Trace> items) throws RetryAttemptException {
        producerInfoInitialiser.initialise(tenantId, items);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public CommunicationDetails processOneToOne(String tenantId, Trace item) throws RetryAttemptException {
        CommunicationDetails ret = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Derive communication details for trace fragment: " + item);
        }

        // Check if trace has a Consumer top level node with an
        // interaction based correlation id
        if (item.getNodes().size() == 1 && item.getNodes().get(0).getClass() == Consumer.class) {
            Consumer consumer = (Consumer) item.getNodes().get(0);
            List<CorrelationIdentifier> cids = consumer.getCorrelationIds(Scope.Interaction);
            if (!cids.isEmpty()) {
                String lastId=null;

                for (int i = 0; ret == null && i < cids.size(); i++) {
                    String id = cids.get(i).getValue();
                    ProducerInfo pi = producerInfoCache.get(tenantId, id);
                    if (pi != null) {
                        ret = new CommunicationDetails();
                        ret.setId(id);
                        ret.setBusinessTransaction(item.getBusinessTransaction());

                        ret.setSource(EndpointUtil.encodeEndpoint(pi.getSourceUri(),
                                pi.getSourceOperation()));

                        ret.setTarget(EndpointUtil.encodeEndpoint(consumer.getUri(),
                                consumer.getOperation()));

                        long diff = TimeUnit.MILLISECONDS.convert(pi.getDuration() - consumer.getDuration(),
                                TimeUnit.NANOSECONDS);
                        if (diff > 0) {
                            ret.setLatency(diff / 2);
                        } else if (diff < 0) {
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("WARNING: Negative latency for consumer = " + consumer);
                            }
                        }

                        ret.setProducerDuration(pi.getDuration());
                        ret.setConsumerDuration(consumer.getDuration());

                        ret.setMultiConsumer(pi.isMultipleConsumers());
                        ret.setInternal(consumer.getEndpointType() == null);

                        // Merge properties from consumer and producer
                        ret.getProperties().addAll(item.getProperties());
                        ret.getProperties().addAll(pi.getProperties());

                        ret.setSourceFragmentId(pi.getFragmentId());
                        ret.setSourceHostName(pi.getHostName());
                        ret.setSourceHostAddress(pi.getHostAddress());
                        ret.setTargetFragmentId(item.getId());
                        ret.setTargetHostName(item.getHostName());
                        ret.setTargetHostAddress(item.getHostAddress());
                        ret.setTargetFragmentDuration(item.calculateDuration());
                        ret.setPrincipal(item.getPrincipal());

                        // HWKBTM-349 Deal with timestamp and offset. Currently
                        // just copying timestamp as-is from producer fragment
                        ret.setTimestamp(pi.getTimestamp());

                        long timestampOffset = item.getStartTime() - pi.getTimestamp() - ret.getLatency();

                        ret.setTimestampOffset(timestampOffset);

                        // Build outbound information
                        initialiseOutbound(consumer.getNodes(), item.getNodes().get(0).getBaseTime(), ret);
                    } else {
                        lastId = id;
                    }
                }
                if (ret == null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("WARNING: Producer information not available [last id checked = " + lastId + "]");
                    }

                    // Need to retry, as the producer information is not currently available
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

    /**
     * This method initialises the outbound information from the consumer's nodes in the supplied
     * communication details.
     *
     * @param consumerNodes The consumer nodes
     * @param baseTime The fragment's base time (ns)
     * @param cd The communication details
     */
    protected static void initialiseOutbound(List<Node> consumerNodes, long baseTime, CommunicationDetails cd) {
        for (int i = 0; i < consumerNodes.size(); i++) {
            Node n = consumerNodes.get(i);
            if (n.getClass() == Producer.class) {
                CommunicationDetails.Outbound ob = new CommunicationDetails.Outbound();
                for (int j = 0; j < ((Producer) n).getCorrelationIds().size(); j++) {
                    CorrelationIdentifier ci = ((Producer) n).getCorrelationIds().get(j);
                    if (ci.getScope() == Scope.Interaction) {
                        ob.getIds().add(ci.getValue());
                    }
                }
                // Only record if outbound ids found
                if (!ob.getIds().isEmpty()) {
                    // Check if pub/sub
                    ob.setMultiConsumer(((Producer) n).multipleConsumers());

                    ob.setProducerOffset(TimeUnit.MILLISECONDS.convert((n.getBaseTime() - baseTime),
                            TimeUnit.NANOSECONDS));
                    cd.getOutbound().add(ob);
                }
            } else if (n.containerNode()) {
                initialiseOutbound(((ContainerNode) n).getNodes(), baseTime, cd);
            }
        }
    }

}
