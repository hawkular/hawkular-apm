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
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.ProducerInfoCache;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanDeriverUtil;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;

/**
 * This class represents the capability for initialising the producer information.
 *
 * @author gbrown
 */
public class ProducerInfoUtil {

    private static final Logger log = Logger.getLogger(ProducerInfoUtil.class.getName());

    private ProducerInfoUtil() {
    }

    /**
     * This method initialises producer information associated with the supplied
     * traces.
     *
     * @param tenantId
     * @param items
     * @param producerInfoCache The cache
     * @throws RetryAttemptException Failed to initialise producer information
     */
    public static void initialise(String tenantId, List<Trace> items, ProducerInfoCache producerInfoCache)
                                throws RetryAttemptException {
        List<ProducerInfo> producerInfoList = new ArrayList<ProducerInfo>();

        // This method initialises the deriver with a list of trace fragments
        // that will need to be referenced when correlating a consumer with a producer
        for (int i = 0; i < items.size(); i++) {
            Origin originUri = new Origin();

            // Need to check for Producer nodes
            Trace trace = items.get(i);
            for (int j = 0; j < trace.getNodes().size(); j++) {
                Node node = trace.getNodes().get(j);
                initialiseProducerInfo(producerInfoList, tenantId, trace, originUri, node, producerInfoCache);
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
     * @param producerInfoCache The cache
     */
    protected static void initialiseProducerInfo(List<ProducerInfo> producerInfoList, String tenantId,
            Trace trace, Origin origin, Node node, ProducerInfoCache producerInfoCache) {
        if (node.getClass() == Producer.class) {
            // Check for interaction correlation ids
            Producer producer = (Producer) node;

            // Check if origin URI has already been set - if not
            // identify based on being a client of the URI associated
            // with the producer
            if (origin.getUri() == null) {
                origin.setUri(Constants.URI_CLIENT_PREFIX + producer.getUri());
                origin.setOperation(producer.getOperation());
            }

            // Calculate the timestamp for the producer
            long diffns = producer.getBaseTime() - trace.getNodes().get(0).getBaseTime();
            long diffms = TimeUnit.MILLISECONDS.convert(diffns, TimeUnit.NANOSECONDS);
            long timestamp = trace.getStartTime() + diffms;

            List<CorrelationIdentifier> cids = producer.findCorrelationIds(Scope.Interaction, Scope.Association);
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
                    pi.getProperties().addAll(producer.getProperties());

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
                        ((ContainerNode) node).getNodes().get(j), producerInfoCache);
            }
        }
    }

    /**
     * This method identifies the root or enclosing server span that contains the
     * supplied client span.
     *
     * @param tenantId The tenant id
     * @param span The client span
     * @param spanCache The span cache
     * @return The root or enclosing server span, or null if not found
     */
    protected static Span findRootOrServerSpan(String tenantId, Span span, SpanCache spanCache) {
        while (span != null &&
                !span.serverSpan() && !span.topLevelSpan()) {
            span = spanCache.get(tenantId, span.getParentId());
        }
        return span;
    }

    /**
     * This method attempts to derive the Producer Information for the supplied server
     * span. If the information is not available, then a null will be returned, which
     * can be used to trigger a retry attempt if appropriate.
     *
     * @param tenantId The tenant id
     * @param serverSpan The server span
     * @param spanCache The cache
     * @return The producer information, or null if not found
     */
    public static ProducerInfo getProducerInfo(String tenantId, Span serverSpan, SpanCache spanCache) {
        String clientSpanId = SpanUniqueIdGenerator.getClientId(serverSpan.getId());
        if (spanCache != null && clientSpanId != null) {
            Span clientSpan = spanCache.get(tenantId, clientSpanId);

            // Work up span hierarchy until find a server span, or top level span
            Span rootOrServerSpan = findRootOrServerSpan(tenantId, clientSpan, spanCache);

            if (rootOrServerSpan != null) {
                // Build producer information
                ProducerInfo pi = new ProducerInfo();
                pi.setDuration(TimeUnit.MILLISECONDS.convert(clientSpan.getDuration(), TimeUnit.MICROSECONDS));
                pi.setTimestamp(TimeUnit.MILLISECONDS.convert(clientSpan.getTimestamp(), TimeUnit.MICROSECONDS));
                pi.setFragmentId(clientSpan.getId());

                pi.getProperties().addAll(clientSpan.binaryAnnotationMapping().getProperties());
                pi.setHostAddress(clientSpan.ipv4());

                if (clientSpan.service() != null) {
                    pi.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, clientSpan.service()));
                }

                pi.setId(clientSpan.getId());
                pi.setMultipleConsumers(false);

                pi.setSourceOperation(SpanDeriverUtil.deriveOperation(rootOrServerSpan));

                if (rootOrServerSpan.serverSpan()) {
                    pi.setSourceUri(rootOrServerSpan.url().getPath());
                } else {
                    pi.setSourceUri(Constants.URI_CLIENT_PREFIX + clientSpan.url().getPath());
                }

                return pi;
            }
        }

        return null;
    }

    /**
     * Container for details about the origin node.
     *
     * @author gbrown
     */
    public static class Origin {

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
