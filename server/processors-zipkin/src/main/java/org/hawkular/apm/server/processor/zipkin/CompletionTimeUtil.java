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

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.utils.zipkin.SpanDeriverUtil;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;

/**
 * @author Pavol Loffay
 */
public class CompletionTimeUtil {

    private CompletionTimeUtil() {}

    /**
     * Convert span to CompletionTime object
     *
     * @param span the span
     * @param spanCache span cache
     * @return completion time derived from the supplied span, if the uri of the completion time
     * cannot be derived (span is server span and client span also does not contain url) it returns null
     */
    public static CompletionTime spanToCompletionTime(SpanCache spanCache, Span span) {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setId(span.getId());

        if (span.getTimestamp() != null) {
            completionTime.setTimestamp(span.getTimestamp());
        }
        if (span.getDuration() != null) {
            completionTime.setDuration(span.getDuration());
        }

        completionTime.setOperation(SpanDeriverUtil.deriveOperation(span));
        completionTime.getProperties().add(new Property(Constants.PROP_FAULT, SpanDeriverUtil.deriveFault(span)));

        completionTime.setHostAddress(span.ipv4());
        if (span.service() != null) {
            completionTime.getProperties().add(new Property(Constants.PROP_SERVICE_NAME, span.service()));
        }

        URL url = getUrl(spanCache, span);
        if (url == null &&
                span.serverSpan() && spanCache.get(null, SpanUniqueIdGenerator.getClientId(span.getId())) != null) {
            return null;
        }

        if (url != null) {
            String uri = span.clientSpan() ? EndpointUtil.encodeClientURI(url.getPath()) : url.getPath();

            completionTime.setUri(uri);
            completionTime.setEndpointType(url.getProtocol() == null ? null : url.getProtocol().toUpperCase());
        } else {
            completionTime.setEndpointType("Unknown");
        }

        completionTime.getProperties().addAll(span.binaryAnnotationMapping().getProperties());

        return completionTime;
    }

    static URL getUrl(SpanCache spanCache, Span span) {
        if (span.url() != null) {
            return span.url();
        }

        Span clientSpan = spanCache.get(null, SpanUniqueIdGenerator.getClientId(span.getId()));
        return clientSpan != null ? clientSpan.url() : null;
    }
}
