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
package org.hawkular.apm.server.jms;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.jms.MessageListener;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.TraceService;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author gbrown
 */
@MessageDriven(name = "Trace_Store", messageListenerInterface = MessageListener.class,
        activationConfig =
        {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "Traces"),
                @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
                @ActivationConfigProperty(propertyName = "clientID", propertyValue = "TraceStore"),
                @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "TraceStore")
        })
@TransactionManagement(value = TransactionManagementType.CONTAINER)
@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
public class TraceStoreMDB extends RetryCapableMDB<Trace> {

    private static final Logger perfLog=Logger.getLogger("org.hawkular.apm.performance.trace");

    @Inject
    private TracePublisherJMS tracePublisher;

    @Inject
    private TraceService traceService;

    @PostConstruct
    public void init() {
        setRetryPublisher(tracePublisher);
        setTypeReference(new TypeReference<java.util.List<Trace>>() {
        });
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.jms.AbstractRetryMDB#process(java.lang.String, java.util.List, int)
     */
    @Override
    protected void process(String tenantId, List<Trace> items, int retryCount) throws Exception {
        long startTime=0;
        if (perfLog.isLoggable(Level.FINEST)) {
            startTime = System.currentTimeMillis();
            perfLog.finest("Performance: about to store trace (first id="+items.get(0).getId()+")");
        }
        traceService.storeTraces(tenantId, items);
        if (perfLog.isLoggable(Level.FINEST)) {
            perfLog.finest("Performance: store trace (first id="+items.get(0).getId()+") duration="+
                        (System.currentTimeMillis()-startTime)+"ms");
        }
    }

}
