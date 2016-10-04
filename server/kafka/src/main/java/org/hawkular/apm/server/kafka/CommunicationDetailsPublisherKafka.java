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
import org.hawkular.apm.client.kafka.AbstractPublisherKafka;
import org.hawkular.apm.server.api.services.CommunicationDetailsPublisher;

/**
 * This class provides the Kafka based CommunicationDetailsPublisher implementation.
 *
 * @author gbrown
 */
public class CommunicationDetailsPublisherKafka extends AbstractPublisherKafka<CommunicationDetails>
        implements CommunicationDetailsPublisher {

    private static final String TOPIC = "CommunicationDetails";

    /**
     * The default constructor.
     */
    public CommunicationDetailsPublisherKafka() {
        super(TOPIC);
    }

}
