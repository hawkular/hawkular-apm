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

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.processor.communicationdetails.CommunicationDetailsDeriver;
import org.hawkular.apm.processor.communicationdetails.ProducerInfoCache;
import org.hawkular.apm.server.api.services.CommunicationDetailsPublisher;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class CommunicationDetailsDeriverKafka extends AbstractRetryConsumerKafka<Trace, CommunicationDetails> {

    private static final String GROUP_ID = "CommunicationDetailsDeriver";

    /**  */
    private static final String TOPIC = "Traces";

    public CommunicationDetailsDeriverKafka() {
        super(TOPIC, GROUP_ID);

        CommunicationDetailsDeriver communicationDetailsDeriver = new CommunicationDetailsDeriver();
        communicationDetailsDeriver.setProducerInfoCache(ServiceResolver.getSingletonService(ProducerInfoCache.class));
        communicationDetailsDeriver.init();

        setProcessor(communicationDetailsDeriver);

        setPublisher(ServiceResolver.getSingletonService(CommunicationDetailsPublisher.class));

        setTypeReference(new TypeReference<Trace>() {
        });
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.kafka.AbstractRetryConsumerKafka#isExpired(java.lang.Object, long)
     */
    @Override
    protected boolean isExpired(Trace item, long currentTime) {
        // TODO Auto-generated method stub
        return false;
    }

}
