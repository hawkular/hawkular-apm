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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.ProducerInfoUtil;
import org.hawkular.apm.server.api.utils.zipkin.SpanDeriverUtil;

/**
 * This class represents the zipkin communication details deriver.
 *
 * @author gbrown
 */
public class CommunicationDetailsDeriver extends AbstractProcessor<Span, CommunicationDetails> {

    private static final Logger log = Logger.getLogger(CommunicationDetailsDeriver.class.getName());

    @Inject
    private SpanCache spanCache;

    /**
     * The default constructor.
     */
    public CommunicationDetailsDeriver() {
        super(ProcessorType.OneToOne);
    }

    /**
     * This constructor initialises the span cache.
     *
     * @param cache The span cache
     */
    public CommunicationDetailsDeriver(SpanCache cache) {
        this();
        spanCache = cache;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#isReportRetryExpirationAsWarning()
     */
    @Override
    public boolean isReportRetryExpirationAsWarning() {
        // We don't want to report a warning, as server spans with no matching client span
        // will result in the retry expiration
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public CommunicationDetails processOneToOne(String tenantId, Span item) throws RetryAttemptException {
        CommunicationDetails ret = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Derive communication details for span: " + item);
        }

        // Check if trace has a Consumer top level node with an
        // interaction based correlation id
        if (item.serverSpan()) {
            ProducerInfo pi = ProducerInfoUtil.getProducerInfo(tenantId, item, spanCache);
            if (pi != null) {
                ret = new CommunicationDetails();
                ret.setId(item.getId());

                ret.setSource(EndpointUtil.encodeEndpoint(pi.getSourceUri(),
                        pi.getSourceOperation()));

                URL url = item.url();
                String op = SpanDeriverUtil.deriveOperation(item);

                if (url != null) {
                    ret.setTarget(EndpointUtil.encodeEndpoint(url.getPath(), op));
                } else {
                    // TODO: ERRORS IN DATA???
                    log.warning("NO URL");
                }

                // Calculate difference in milliseconds
                long diff = pi.getDuration() - TimeUnit.MILLISECONDS.convert(item.getDuration(),
                        TimeUnit.MICROSECONDS);
                if (diff > 0) {
                    ret.setLatency(diff / 2);
                } else if (diff < 0) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("WARNING: Negative latency for consumer = " + item);
                    }
                }

                ret.setProducerDuration(pi.getDuration());
                ret.setConsumerDuration(TimeUnit.MILLISECONDS.convert(item.getDuration(), TimeUnit.MICROSECONDS));

                ret.setMultiConsumer(pi.isMultipleConsumers());
                //ret.setInternal(consumer.getEndpointType() == null);

                // Merge properties from consumer and producer
                ret.getProperties().addAll(item.binaryAnnotationMapping().getProperties());
                ret.getProperties().addAll(pi.getProperties());

                if (item.service() != null) {
                    ret.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, item.service()));
                }

                ret.setSourceFragmentId(pi.getFragmentId());
                ret.setSourceHostName(pi.getHostName());
                ret.setSourceHostAddress(pi.getHostAddress());
                ret.setTargetFragmentId(item.getId());
                //ret.setTargetHostName(item.getHostName());
                ret.setTargetHostAddress(item.ipv4());
                //ret.setTargetFragmentDuration(item.calculateDuration());

                // HWKBTM-349 Deal with timestamp and offset. Currently
                // just copying timestamp as-is from producer fragment
                ret.setTimestamp(pi.getTimestamp());

                long timestampOffset = item.getTimestamp() - pi.getTimestamp() - ret.getLatency();

                ret.setTimestampOffset(timestampOffset);
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("WARNING: Producer information not available [id checked = " + item.getId() + "]");
                }

                // Need to retry, as the producer information is not currently available
                throw new RetryAttemptException("Producer information not available [id checked = "
                                        + item.getId() + "]");
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Derived communication details: " + ret);
        }

        return ret;
    }

}
