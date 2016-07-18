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

import java.util.logging.Logger;

import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionInformation;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionInformationProcessor;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionInformationPublisher;
import org.hawkular.apm.server.api.services.CommunicationDetailsCache;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class TraceCompletionInformationProcessorKafka
        extends AbstractRetryConsumerKafka<TraceCompletionInformation, TraceCompletionInformation> {

    private static final Logger log = Logger.getLogger(TraceCompletionInformationProcessorKafka.class.getName());

    private static final String GROUP_ID = "TraceCompletionInformationProcessor";

    /**  */
    private static final String TOPIC = "TraceCompletionInformation";

    public TraceCompletionInformationProcessorKafka() {
        super(TOPIC, GROUP_ID);

        TraceCompletionInformationProcessor processor = new TraceCompletionInformationProcessor();
        processor.setCommunicationDetailsCache(ServiceResolver.getSingletonService(CommunicationDetailsCache.class));

        if (processor.getCommunicationDetailsCache() == null) {
            log.severe("Communication Details Cache not found - possibly not configured correctly");
            processor = null;
        }

        setPublisher(ServiceResolver.getSingletonService(TraceCompletionInformationPublisher.class));

        if (getPublisher() == null) {
            log.severe("Trace Completion Information Publisher not found - possibly not configured correctly");
            processor = null;
        }

        if (processor != null) {

            setTypeReference(new TypeReference<TraceCompletionInformation>() {
            });

            setProcessor(processor);
        } else {
            log.severe("Trace Completion Information Processor not started - publisher and/or cache not available");
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.kafka.AbstractRetryConsumerKafka#isExpired(java.lang.Object, long)
     */
    @Override
    protected boolean isExpired(TraceCompletionInformation item, long currentTime) {
        // TODO Auto-generated method stub
        return false;
    }

}
