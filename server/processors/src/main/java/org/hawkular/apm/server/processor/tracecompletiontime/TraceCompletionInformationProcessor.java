/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.apm.server.processor.tracecompletiontime;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.server.api.services.CommunicationDetailsCache;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.hawkular.apm.server.processor.tracecompletiontime.TraceCompletionInformation.Communication;

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

    /**
     * The default constructor.
     */
    public TraceCompletionInformationProcessor() {
        super(ProcessorType.OneToOne);
    }

    @Override
    public long getDeliveryDelay(List<TraceCompletionInformation> results) {
        // TODO: Just return default 500msec delay for now, but when supporting long running
        // processes HWKAPM-348, then will need to detect delivery delay on a
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

    @Override
    public TraceCompletionInformation processOneToOne(String tenantId,
            TraceCompletionInformation item) throws RetryAttemptException {

        if (!item.getCommunications().isEmpty()) {
            long currentTime = System.currentTimeMillis();

            for (int i = 0; i < item.getCommunications().size(); i++) {
                Communication c = item.getCommunications().get(i);

                if (c.getExpire() < currentTime) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Completion info " + item + ": communication expired = " + c);
                    }

                    // If related to a Pub/Sub (multiple consumer) situation, then need to wait
                    // until the expiration time, to ensure all possible consumer details are
                    // located
                    if (c.isMultipleConsumers()) {
                        for (int j = 0; j < c.getIds().size(); j++) {
                            List<CommunicationDetails> cds = communicationDetailsCache.getById(tenantId,
                                    c.getIds().get(j));
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("Multiconsumer comms details: id = " + c.getIds().get(j)
                                        + " communication details = " + cds);
                            }
                            for (int k = 0; k < cds.size(); k++) {
                                processCommunication(tenantId, item, c, cds.get(k));
                            }
                        }
                    }

                    // Remove communication from remaining list to process
                    item.getCommunications().remove(i);
                    i--;

                } else if (!c.isMultipleConsumers()) {
                    // Point to Point communications dealt with as soon as the details are available

                    // Find communication details for communication id
                    CommunicationDetails cd = null;

                    // Find communication details associated with one of the ids
                    // specified with the communication
                    for (int j = 0; cd == null && j < c.getIds().size(); j++) {
                        cd = communicationDetailsCache.get(tenantId, c.getIds().get(j));
                    }

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Completion info " + item + ": communication details for communication " + c
                                + " = " + cd);
                    }

                    if (cd != null) {
                        processCommunication(tenantId, item, c, cd);

                        // Remove communication from remaining list to process
                        item.getCommunications().remove(i);
                        i--;
                    }
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

    protected void processCommunication(String tenantId, TraceCompletionInformation item,
            Communication c, CommunicationDetails cd) {
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
            newc.setIds(ob.getLinkIds());
            newc.setMultipleConsumers(ob.isMultiConsumer());
            newc.setBaseDuration(targetFragmentBaseDuration + ob.getProducerOffset());

            // TODO: Need to derive expiration based on knowledge about the time
            // different between the source and target linked fragments being
            // reported
            long baseTimeStamp = ob.isMultiConsumer() ?
                    TimeUnit.MICROSECONDS.toMillis(cd.getTimestamp()) :
                    System.currentTimeMillis();
            newc.setExpire(baseTimeStamp + Communication.DEFAULT_EXPIRY_WINDOW_MILLIS);

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Completion info " + item + ": new communication = " + newc);
            }

            item.getCommunications().add(newc);
        }
    }
}
