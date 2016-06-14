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
package org.hawkular.apm.processor.tracecompletiontime;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.processor.tracecompletiontime.TraceCompletionInformation.Communication;
import org.hawkular.apm.server.api.task.AbstractProcessor;

/**
 * This class represents the function for processing completion information associated with
 * a trace instance by locating details for related fragments.
 *
 * @author gbrown
 */
public class TraceCompletionInformationProcessor extends
                    AbstractProcessor<TraceCompletionInformation, TraceCompletionInformation> {

    private static final Logger log = Logger.getLogger(TraceCompletionInformationProcessor.class.getName());

    private static final long DEFAULT_DELAY = 500;

    @Inject
    private CommunicationDetailsCache communicationDetailsCache;

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#getDeliveryDelay(java.util.List)
     */
    @Override
    public long getDeliveryDelay(List<TraceCompletionInformation> results) {
        // TODO: Just return default 500msec delay for now, but when supporting long running
        // processes HWKBTM-348, then will need to detect delivery delay on a
        // per result basis, in case some long delays are required
        return DEFAULT_DELAY;
    }

    /**
     * @return the communicationDetailsCache
     */
    public CommunicationDetailsCache getCommunicationDetailsCache() {
        return communicationDetailsCache;
    }

    /**
     * @param communicationDetailsCache the communicationDetailsCache to set
     */
    public void setCommunicationDetailsCache(CommunicationDetailsCache communicationDetailsCache) {
        this.communicationDetailsCache = communicationDetailsCache;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.String,java.lang.Object)
     */
    @Override
    public TraceCompletionInformation processSingle(String tenantId,
                            TraceCompletionInformation item) throws Exception {

        if (!item.getCommunications().isEmpty()) {
            long currentTime = System.currentTimeMillis();

            for (int i = 0; i < item.getCommunications().size(); i++) {
                Communication c = item.getCommunications().get(i);

                if (c.getExpire() < currentTime) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Completion info " + item + ": communication expired = " + c);
                    }

                    // Remove communication from remaining list to process
                    item.getCommunications().remove(i);
                    i--;

                } else if (!c.isMultipleConsumers()) {
                    // Find communication details for communication id
                    CommunicationDetails cd = null;

                    // Find communication details associated with one of the ids
                    // specified with the communication
                    for (int j = 0; cd == null && j < c.getIds().size(); j++) {
                        cd = communicationDetailsCache.getSingleConsumer(tenantId, c.getIds().get(j));
                    }

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Completion info " + item + ": communication details for communication " + c
                                + " = " + cd);
                    }

                    if (cd != null) {
                        long targetFragmentBaseDuration = c.getBaseDuration() + cd.getLatency();

                        // Check if target fragment duration increases overall time
                        long durationWithTargetFragment = targetFragmentBaseDuration + cd.getTargetFragmentDuration();

                        if (durationWithTargetFragment > item.getCompletionTime().getDuration()) {
                            item.getCompletionTime().setDuration(durationWithTargetFragment);
                        }

                        // Merge properties
                        if (!cd.getProperties().isEmpty()) {
                            item.getCompletionTime().getProperties().addAll(cd.getProperties());
                        }

                        // Add any outbound comms from target
                        for (int j = 0; j < cd.getOutbound().size(); j++) {
                            CommunicationDetails.Outbound ob = cd.getOutbound().get(j);
                            Communication newc = new Communication();
                            newc.setIds(ob.getIds());
                            newc.setMultipleConsumers(ob.isMultiConsumer());
                            newc.setBaseDuration(targetFragmentBaseDuration + ob.getProducerOffset());
                            newc.setExpire(System.currentTimeMillis()
                                    + TraceCompletionInformation.Communication.DEFAULT_EXPIRY_WINDOW);

                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("Completion info " + item + ": new communication = " + newc);
                            }

                            item.getCommunications().add(newc);
                        }

                        // Remove communication from remaining list to process
                        item.getCommunications().remove(i);
                        i--;
                    }
                } else {
                    // TODO: HWKBTM-356 Deal with multiple consumers - query
                    // for interaction id
                }
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Updated completion information = " + item);
            }

            return item;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("No communications to be processed for completion information = " + item);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processMultiple(java.lang.String,java.lang.Object)
     */
    @Override
    public List<TraceCompletionInformation> processMultiple(String tenantId,
                            TraceCompletionInformation item) throws Exception {
        return null;
    }
}
