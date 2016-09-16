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
package org.hawkular.apm.server.processor.zipkin;

import java.util.List;

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.jboss.logging.Logger;

/**
 * This class represents the zipkin fragment completion time deriver.
 *
 * @author gbrown
 */
public class FragmentCompletionTimeDeriver extends AbstractProcessor<Span, CompletionTime> {

    private static final Logger log = Logger.getLogger(FragmentCompletionTimeDeriver.class);

    private final SpanCache spanCache;


    /**
     * The default constructor.
     */
    public FragmentCompletionTimeDeriver(SpanCache spanCache) {
        super(ProcessorType.OneToOne);
        this.spanCache = spanCache;
    }

    @Override
    public CompletionTime processOneToOne(String tenantId, Span span) throws RetryAttemptException {

        if (span.getParentId() == null || span.serverSpan()) {
            CompletionTime ct = CompletionTimeUtil.spanToCompletionTime(spanCache, span);
            if (ct == null) {
                return null;
            }

            /**
             * Client span can contain URL.
             * If there is not URL after retry attempts then do not derive completion time.
             */
            if (span.serverSpan() && ct.getUri() == null) {
                // stop retries if the client span was found and url is still missing
                if (spanCache.get(null, SpanUniqueIdGenerator.getClientId(span.getId())) != null) {
                    log.warnf("NO URL, span = %s", span);
                    return null;
                }

                log.debugf("Server span does not contain URL, waiting for client span, span id=%s", span.getId());
                throw new RetryAttemptException("URL is null, span id = " + span.getId());
            }

            if (span.clientSpan()) {
                // To differentiate from the server fragment
                ct.setId(SpanUniqueIdGenerator.toUnique(span));
            }

            log.debugf("FragmentCompletionTimeDeriver ret= %s", ct);
            return ct;
        }

        return null;
    }

    @Override
    public long getRetryDelay(List<Span> spans, int retryCount) {
        // TODO HWKAPM-348
        return 5000;
    }
}
