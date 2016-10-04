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
package org.hawkular.apm.server.kafka;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SourceInfoCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.api.utils.SourceInfoUtil;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class SourceInfoCacheKafka extends AbstractConsumerKafka<Trace, Void> {

    private static final Logger log = Logger.getLogger(SourceInfoCacheKafka.class.getName());

    /** Create a unique group id, to enable each separate instance of this processor to be
     * able to receive all messages stored on the topic (i.e. topic subscriber rather than
     * queue semantics) */
    private static final String GROUP_ID = "SourceInfoCache_" + UUID.randomUUID().toString();

    private static final String TOPIC = "Traces";

    private SourceInfoCache sourceInfoCache;

    public SourceInfoCacheKafka() {
        super(TOPIC, GROUP_ID);

        sourceInfoCache = ServiceResolver.getSingletonService(SourceInfoCache.class);

        if (sourceInfoCache == null) {
            log.severe("Source Info Cache not found - possibly not configured correctly");
        } else {
            setTypeReference(new TypeReference<Trace>() {
            });

            setProcessor(new AbstractProcessor<Trace, Void>(ProcessorType.ManyToMany) {

                @Override
                public List<Void> processManyToMany(String tenantId, List<Trace> items)
                        throws RetryAttemptException {
                    List<SourceInfo> sourceInfoList = SourceInfoUtil.getSourceInfo(tenantId, items);

                    try {
                        sourceInfoCache.store(tenantId, sourceInfoList);
                    } catch (CacheException e) {
                        throw new RetryAttemptException(e);
                    }
                    return null;
                }
            });
        }
    }
}
