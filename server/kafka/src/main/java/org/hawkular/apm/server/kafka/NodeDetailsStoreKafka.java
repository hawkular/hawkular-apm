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

import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.services.AnalyticsService;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.hawkular.apm.server.api.task.RetryAttemptException;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
public class NodeDetailsStoreKafka extends AbstractConsumerKafka<NodeDetails, Void> {

    private static final Logger log = Logger.getLogger(NodeDetailsStoreKafka.class.getName());

    /**  */
    private static final String GROUP_ID = "NodeDetailsStore";

    /**  */
    private static final String TOPIC = "NodeDetails";

    private AnalyticsService analyticsService;

    public NodeDetailsStoreKafka() {
        super(TOPIC, GROUP_ID);

        analyticsService = ServiceResolver.getSingletonService(AnalyticsService.class);

        if (analyticsService == null) {
            log.severe("Analytics Service not found - possibly not configured correctly");
        } else {

            setTypeReference(new TypeReference<NodeDetails>() {
            });

            setProcessor(new AbstractProcessor<NodeDetails, Void>(ProcessorType.ManyToMany) {

                @Override
                public List<Void> processManyToMany(String tenantId, List<NodeDetails> items)
                        throws RetryAttemptException {
                    try {
                        analyticsService.storeNodeDetails(tenantId, items);
                    } catch (StoreException se) {
                        throw new RetryAttemptException(se);
                    }
                    return null;
                }
            });
        }
    }
}
