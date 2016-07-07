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

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionInformation;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionTimeDeriver;
import org.hawkular.apm.server.api.services.TraceCompletionTimePublisher;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class TraceCompletionTimeDeriverKafka
        extends AbstractConsumerKafka<TraceCompletionInformation, CompletionTime> {

    private static final String GROUP_ID = "TraceCompletionTimeDeriver";

    /**  */
    private static final String TOPIC = "TraceCompletionInformation";

    public TraceCompletionTimeDeriverKafka() {
        super(TOPIC, GROUP_ID);

        setProcessor(new TraceCompletionTimeDeriver());

        setPublisher(ServiceResolver.getSingletonService(TraceCompletionTimePublisher.class));

        setTypeReference(new TypeReference<TraceCompletionInformation>() {
        });
    }

}
