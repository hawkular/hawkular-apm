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
import java.util.logging.Logger;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class TraceStoreKafka extends AbstractConsumerKafka<Trace, Void> {

    private static final Logger log = Logger.getLogger(TraceStoreKafka.class.getName());

    /**  */
    private static final String GROUP_ID = "TraceStore";

    /**  */
    private static final String TOPIC = "Traces";

    private TraceService traceService;

    public TraceStoreKafka() {
        super(TOPIC, GROUP_ID);

        traceService = ServiceResolver.getSingletonService(TraceService.class);

        if (traceService == null) {
            log.severe("Trace Service not found - possibly not configured correctly");
        } else {

            setTypeReference(new TypeReference<Trace>() {
            });

            setProcessor(new AbstractProcessor<Trace, Void>(ProcessorType.ManyToMany) {

                @Override
                public List<Void> processManyToMany(String tenantId, List<Trace> items)
                        throws RetryAttemptException {
                    try {
                        traceService.storeTraces(tenantId, items);
                    } catch (StoreException se) {
                        throw new RetryAttemptException(se);
                    }
                    return null;
                }
            });
        }
    }
}
