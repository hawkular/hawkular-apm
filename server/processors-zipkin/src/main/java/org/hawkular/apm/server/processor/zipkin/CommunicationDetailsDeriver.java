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
package org.hawkular.apm.server.processor.zipkin;

import java.net.URL;
import java.util.UUID;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.SourceInfoUtil;
import org.hawkular.apm.server.api.utils.zipkin.SpanDeriverUtil;
import org.jboss.logging.Logger;

/**
 * This class represents the zipkin communication details deriver.
 *
 * @author gbrown
 */
public class CommunicationDetailsDeriver extends AbstractProcessor<Span, CommunicationDetails> {

    private static final Logger log = Logger.getLogger(CommunicationDetailsDeriver.class);

    private final SpanCache spanCache;


    /**
     * This constructor initialises the span cache.
     *
     * @param spanCache The span cache
     */
    public CommunicationDetailsDeriver(SpanCache spanCache) {
        super(ProcessorType.OneToOne);
        this.spanCache = spanCache;
    }

    @Override
    public boolean isReportRetryExpirationAsWarning() {
        // We don't want to report a warning, as server spans with no matching client span
        // will result in the retry expiration
        return false;
    }

    @Override
    public CommunicationDetails processOneToOne(String tenantId, Span item) throws RetryAttemptException {
        CommunicationDetails ret = null;

        log.debugf("Derive communication details for span: %s", item);

        // Check if trace has a Consumer top level node with an
        // interaction based correlation id
        if (item.serverSpan()) {
            SourceInfo si = SourceInfoUtil.getSourceInfo(tenantId, item, spanCache);
            if (si != null) {
                ret = new CommunicationDetails();
                ret.setId(UUID.randomUUID().toString());
                ret.setLinkId(item.getId());

                ret.setSource(si.getEndpoint().toString());

                URL url = CompletionTimeUtil.getUrl(spanCache, item);
                String operation = SpanDeriverUtil.deriveOperation(item);

                if (url != null) {
                    ret.setTarget(EndpointUtil.encodeEndpoint(url.getPath(), operation));
                } else {
                    // TODO: ERRORS IN DATA???
                    log.debugf("NO URL, span = %s", item);
                }

                // Calculate difference in milliseconds
                long diff = si.getDuration() - item.getDuration();
                if (diff > 0) {
                    ret.setLatency(diff / 2);
                } else if (diff < 0) {
                    log.debugf("WARNING: Negative latency for consumer = %s", item);
                }

                ret.setSourceDuration(si.getDuration());
                ret.setTargetDuration(item.getDuration());

                ret.setMultiConsumer(si.isMultipleConsumers());
                //ret.setInternal(consumer.getEndpointType() == null);

                // Merge properties from consumer and producer
                ret.getProperties().addAll(item.binaryAnnotationMapping().getProperties());
                ret.getProperties().addAll(si.getProperties());

                if (item.service() != null) {
                    ret.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, item.service()));
                }

                ret.setTraceId(item.getTraceId());
                ret.setSourceFragmentId(si.getFragmentId());
                ret.setSourceHostName(si.getHostName());
                ret.setSourceHostAddress(si.getHostAddress());
                ret.setTargetFragmentId(item.getId());
                //ret.setTargetHostName(item.getHostName());
                ret.setTargetHostAddress(item.ipv4());
                //ret.setTargetFragmentDuration(item.calculateDuration());

                // HWKBTM-349 Deal with timestamp and offset. Currently
                // just copying timestamp as-is from producer fragment
                ret.setTimestamp(si.getTimestamp());

                long timestampOffset = item.getTimestamp() - si.getTimestamp() - ret.getLatency();

                ret.setTimestampOffset(timestampOffset);
            } else {
                log.debugf("WARNING: Producer information not available [id checked = %s]", item.getId());
                // Need to retry, as the source information is not currently available
                throw new RetryAttemptException("Producer information not available [id checked = "
                                        + item.getId() + "]");
            }
        }

        log.debugf("Derived communication details: %s", ret);

        return ret;
    }
}
