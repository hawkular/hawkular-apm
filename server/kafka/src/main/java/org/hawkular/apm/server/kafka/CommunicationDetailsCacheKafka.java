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

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.processor.tracecompletiontime.CommunicationDetailsCache;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class CommunicationDetailsCacheKafka extends AbstractConsumerKafka<CommunicationDetails, Void> {

    private static final Logger log = Logger.getLogger(CommunicationDetailsCacheKafka.class.getName());

    /** Create a unique group id, to enable each separate instance of this processor to be
     * able to receive all messages stored on the topic (i.e. topic subscriber rather than
     * queue semantics) */
    private static final String GROUP_ID = "CommunicationDetailsCache_" + UUID.randomUUID().toString();

    /**  */
    private static final String TOPIC = "CommunicationDetails";

    private CommunicationDetailsCache communicationDetailsCache;

    public CommunicationDetailsCacheKafka() {
        super(TOPIC, GROUP_ID);

        communicationDetailsCache = ServiceResolver.getSingletonService(CommunicationDetailsCache.class);

        if (communicationDetailsCache == null) {
            log.severe("Communication Details Cache not found - possibly not configured correctly");
        } else {

            setTypeReference(new TypeReference<CommunicationDetails>() {
            });

            setProcessor(new AbstractProcessor<CommunicationDetails, Void>(ProcessorType.ManyToMany) {

                @Override
                public List<Void> processManyToMany(String tenantId, List<CommunicationDetails> items)
                        throws RetryAttemptException {
                    try {
                        communicationDetailsCache.store(tenantId, items);
                    } catch (CacheException ce) {
                        throw new RetryAttemptException(ce);
                    }
                    return null;
                }
            });
        }
    }
}
