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
package org.hawkular.apm.server.api.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.ProducerInfoCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the capability for initialising the producer information.
 *
 * @author gbrown
 */
public class ProducerInfoCacheUtil {

    private static final Logger log = Logger.getLogger(ProducerInfoCacheUtil.class.getName());

    private ProducerInfoCache producerInfoCache;

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

    /**
     * This method initialises producer information associated with the supplied
     * traces.
     *
     * @param tenantId
     * @param items
     * @throws RetryAttemptException Failed to initialise producer information
     */
    public void initialise(String tenantId, List<Trace> items) throws RetryAttemptException {
        List<ProducerInfo> producerInfoList = new ArrayList<ProducerInfo>();

        // This method initialises the deriver with a list of trace fragments
        // that will need to be referenced when correlating a consumer with a producer
        for (int i = 0; i < items.size(); i++) {
            Origin originUri = new Origin();

            // Need to check for Producer nodes
            Trace trace = items.get(i);
            for (int j = 0; j < trace.getNodes().size(); j++) {
                Node node = trace.getNodes().get(j);
                initialiseProducerInfo(producerInfoList, tenantId, trace, originUri, node);
            }
        }

        try {
            producerInfoCache.store(tenantId, producerInfoList);
        } catch (CacheException e) {
            throw new RetryAttemptException(e);
        }
    }

    /**
     * This method initialises an individual node within a trace.
     *
     * @param producerInfoList The producer info list
     * @param tenantId The tenant id
     * @param trace The trace
     * @param origin The origin node information
     * @param node The node
     */
    protected void initialiseProducerInfo(List<ProducerInfo> producerInfoList, String tenantId,
            Trace trace, Origin origin, Node node) {
        if (node.getClass() == Producer.class) {
            // Check for interaction correlation ids
            Producer producer = (Producer) node;

            // Check if origin URI has already been set - if not
            // identify based on being a client of the URI associated
            // with the producer
            if (origin.getUri() == null) {
                origin.setUri(Constants.URI_CLIENT_PREFIX + producer.getUri());
            }

            // Calculate the timestamp for the producer
            long diffns = producer.getBaseTime() - trace.getNodes().get(0).getBaseTime();
            long diffms = TimeUnit.MILLISECONDS.convert(diffns, TimeUnit.NANOSECONDS);
            long timestamp = trace.getStartTime() + diffms;

            List<CorrelationIdentifier> cids = producer.getCorrelationIds(Scope.Interaction);
            if (!cids.isEmpty()) {
                for (int i = 0; i < cids.size(); i++) {
                    ProducerInfo pi = new ProducerInfo();
                    pi.setId(cids.get(i).getValue());
                    pi.setSourceUri(origin.getUri());
                    pi.setSourceOperation(origin.getOperation());
                    pi.setTimestamp(timestamp);
                    pi.setDuration(producer.getDuration());
                    pi.setFragmentId(trace.getId());
                    pi.setHostName(trace.getHostName());
                    pi.setHostAddress(trace.getHostAddress());
                    pi.setMultipleConsumers(producer.multipleConsumers());
                    pi.getProperties().addAll(trace.getProperties());

                    // TODO: HWKBTM-348: Should be configurable based on the wait interval plus
                    // some margin of error - primarily for cases where a job scheduler
                    // is used. If direct communications, then only need to cater for
                    // latency.

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Adding producer information for id=" + cids.get(i).getValue() + " pi=" + pi);
                    }
                    producerInfoList.add(pi);
                }
            }
        } else if (node instanceof ContainerNode) {
            if (origin.getUri() == null && node.getClass() == Consumer.class) {
                origin.setUri(node.getUri());
                origin.setOperation(node.getOperation());
            }
            for (int j = 0; j < ((ContainerNode) node).getNodes().size(); j++) {
                initialiseProducerInfo(producerInfoList, tenantId, trace, origin,
                        ((ContainerNode) node).getNodes().get(j));
            }
        }
    }

    /**
     * Container for details about the origin node.
     *
     * @author gbrown
     */
    public class Origin {

        private String uri;
        private String operation;

        /**
         * @return the uri
         */
        public String getUri() {
            return uri;
        }

        /**
         * @param uri the uri to set
         */
        public void setUri(String uri) {
            this.uri = uri;
        }

        /**
         * @return the operation
         */
        public String getOperation() {
            return operation;
        }

        /**
         * @param operation the operation to set
         */
        public void setOperation(String operation) {
            this.operation = operation;
        }

    }
}
