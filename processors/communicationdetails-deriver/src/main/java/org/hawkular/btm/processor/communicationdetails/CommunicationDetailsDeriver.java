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
package org.hawkular.btm.processor.communicationdetails;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.events.CommunicationDetails;
import org.hawkular.btm.server.api.task.AbstractProcessor;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

/**
 * This class represents the communication details deriver.
 *
 * @author gbrown
 */
public class CommunicationDetailsDeriver extends AbstractProcessor<BusinessTransaction, CommunicationDetails> {

    /**  */
    protected static final String CLIENT_PREFIX = "[client]";

    private static final Logger log = Logger.getLogger(CommunicationDetailsDeriver.class.getName());

    @Resource(lookup = "java:jboss/infinispan/BTM")
    private CacheContainer container;

    private Cache<String, ProducerInfo> producerInfo;

    @PostConstruct
    public void init() {
        producerInfo = container.getCache("producerinfo");
    }

    /**
     * This method sets the cache for producer info.
     *
     * @param cache The cache
     */
    protected void setProducerInfoCache(Cache<String, ProducerInfo> cache) {
        producerInfo = cache;
    }

    /**
     * @return the producerinfo cache
     */
    protected Cache<String, ProducerInfo> getProducerinfoCache() {
        return producerInfo;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#initialise(java.util.List)
     */
    @Override
    public void initialise(String tenantId, List<BusinessTransaction> items) {
        // This method initialises the deriver with a list of business transaction fragments
        // that will need to be referenced when correlating a consumer with a producer
        for (int i = 0; i < items.size(); i++) {
            String originUri = null;

            // Need to check for Producer nodes
            BusinessTransaction btxn = items.get(i);
            for (int j = 0; j < btxn.getNodes().size(); j++) {
                Node node = btxn.getNodes().get(j);
                originUri = initialiseNode(btxn, originUri, node);
            }
        }
    }

    /**
     * This method initialises an individual node within a business transaction.
     *
     * @param btxn The business transaction
     * @param originUri The origin uri
     * @param node The node
     * @return The origin URI
     */
    protected String initialiseNode(BusinessTransaction btxn, String originUri, Node node) {
        if (node.getClass() == Producer.class) {
            // Check for interaction correlation ids
            Producer producer = (Producer) node;

            // Check if origin URI has already been set - if not
            // identify based on being a client of the URI associated
            // with the producer
            if (originUri == null) {
                originUri = CLIENT_PREFIX + producer.getUri();
            }

            // Calculate the timestamp for the producer
            long diffns = producer.getBaseTime() - btxn.getNodes().get(0).getBaseTime();
            long diffms = TimeUnit.MILLISECONDS.convert(diffns, TimeUnit.NANOSECONDS);
            long timestamp = btxn.getStartTime() + diffms;

            List<CorrelationIdentifier> cids = producer.getCorrelationIds(Scope.Interaction);
            if (!cids.isEmpty()) {
                for (int i = 0; i < cids.size(); i++) {
                    ProducerInfo pi = new ProducerInfo();
                    pi.setOriginUri(originUri);
                    pi.setTimestamp(timestamp);
                    pi.setDuration(producer.getDuration());
                    pi.setFragmentId(btxn.getId());
                    pi.setHostName(btxn.getHostName());
                    pi.setHostAddress(btxn.getHostAddress());
                    pi.setMultipleConsumers(producer.multipleConsumers());

                    // TODO: HWKBTM-348: Should be configurable based on the wait interval plus
                    // some margin of error - primarily for cases where a job scheduler
                    // is used. If direct communications, then only need to cater for
                    // latency.
                    producerInfo.put(cids.get(i).getValue(), pi, 1, TimeUnit.MINUTES);
                }
            }
        } else if (node instanceof ContainerNode) {
            if (originUri == null && node.getClass() == Consumer.class) {
                originUri = node.getUri();
            }
            for (int j = 0; j < ((ContainerNode) node).getNodes().size(); j++) {
                originUri = initialiseNode(btxn, originUri, ((ContainerNode) node).getNodes().get(j));
            }
        }

        return originUri;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public CommunicationDetails processSingle(String tenantId, BusinessTransaction item) throws Exception {
        CommunicationDetails ret = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Derive communication details for business transaction fragment: " + item);
        }

        // Check if business transaction has a Consumer top level node with an
        // interaction based correlation id
        if (item.getNodes().size() == 1 && item.getNodes().get(0).getClass() == Consumer.class) {
            Consumer consumer = (Consumer) item.getNodes().get(0);
            List<CorrelationIdentifier> cids = consumer.getCorrelationIds(Scope.Interaction);
            if (!cids.isEmpty()) {
                for (int i = 0; ret == null && i < cids.size(); i++) {
                    String id = cids.get(i).getValue();
                    ProducerInfo pi = producerInfo.get(id);
                    if (pi != null) {
                        ret = new CommunicationDetails();
                        ret.setId(id);
                        ret.setBusinessTransaction(item.getName());
                        ret.setUri(consumer.getUri());

                        long diff = TimeUnit.MILLISECONDS.convert(pi.getDuration() - consumer.getDuration(),
                                            TimeUnit.NANOSECONDS);
                        if (diff > 0) {
                            ret.setLatency(diff / 2);
                        } else if (diff < 0) {
                            log.warning("Negative latency for consumer = " + consumer);
                        }

                        ret.setProducerDuration(pi.getDuration());
                        ret.setConsumerDuration(consumer.getDuration());

                        ret.setOriginUri(pi.getOriginUri());
                        ret.setMultiConsumer(pi.isMultipleConsumers());
                        ret.setProperties(item.getProperties());
                        ret.setSourceFragmentId(pi.getFragmentId());
                        ret.setSourceHostName(pi.getHostName());
                        ret.setSourceHostAddress(pi.getHostAddress());
                        ret.setTargetFragmentId(item.getId());
                        ret.setTargetHostName(item.getHostName());
                        ret.setTargetHostAddress(item.getHostAddress());
                        ret.setTargetFragmentDuration(item.calculateDuration());

                        // HWKBTM-349 Deal with timestamp and offset. Currently
                        // just copying timestamp as-is from producer fragment
                        ret.setTimestamp(pi.getTimestamp());

                        long timestampOffset = item.getStartTime() - pi.getTimestamp() - ret.getLatency();

                        ret.setTimestampOffset(timestampOffset);

                        // Build outbound information
                        initialiseOutbound(consumer.getNodes(), item.getNodes().get(0).getBaseTime(), ret);
                    }
                }
                if (ret == null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Producer information not available");
                    }

                    // Need to retry, as the producer information is not currently available
                    throw new RuntimeException("Producer information not available");
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

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.Object)
     */
    @Override
    public List<CommunicationDetails> processMultiple(String tenantId, BusinessTransaction item) throws Exception {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#cleanup(java.util.List)
     */
    @Override
    public void cleanup(String tenantId, List<BusinessTransaction> items) {
    }
}
