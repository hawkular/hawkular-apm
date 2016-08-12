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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the zipkin trace completion time deriver.
 *
 * @author gbrown
 */
public class TraceCompletionTimeDeriver extends AbstractProcessor<Span, CompletionTime> {

    private static final Logger log = Logger.getLogger(TraceCompletionTimeDeriver.class.getName());

    /**
     * The default constructor.
     */
    public TraceCompletionTimeDeriver() {
        super(ProcessorType.OneToOne);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public CompletionTime processOneToOne(String tenantId, Span item) throws RetryAttemptException {

        if (item.getParentId() == null) {
            CompletionTime ct = new CompletionTime();
            ct.setId(item.getId());

            URL url = item.url();
            if (url != null) {
                ct.setUri(url.getPath());
                ct.setEndpointType(url.getProtocol() == null ? null : url.getProtocol().toUpperCase());
            } else {
                ct.setEndpointType("Unknown");
            }

            ct.setDuration(TimeUnit.MILLISECONDS.convert(item.getDuration(), TimeUnit.NANOSECONDS));

            ct.setTimestamp(item.getTimestamp()/1000);
            ct.setOperation(SpanDeriverUtil.deriveOperation(item));

            List<Property> spanProperties = item.properties();
            ct.getProperties().addAll(spanProperties);
            ct.setHostAddress(Span.ipv4Address(spanProperties));

            if (log.isLoggable(Level.FINEST)) {
                log.finest("TraceCompletionTimeDeriver ret=" + ct);
            }
            return ct;
        }

        return null;
    }

}
