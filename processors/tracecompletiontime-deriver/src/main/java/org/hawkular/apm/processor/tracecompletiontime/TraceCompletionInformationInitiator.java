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

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the function for initiating completion time calculation based on
 * detecting an initial trace fragment.
 *
 * @author gbrown
 */
public class TraceCompletionInformationInitiator extends
        AbstractProcessor<Trace, TraceCompletionInformation> {

    private static final Logger log = Logger.getLogger(TraceCompletionInformationInitiator.class.getName());

    /**
     * The default constructor.
     */
    public TraceCompletionInformationInitiator() {
        super(ProcessorType.OneToOne);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.String,java.lang.Object)
     */
    @Override
    public TraceCompletionInformation processOneToOne(String tenantId,
            Trace item) throws RetryAttemptException {
        // Check whether the trace fragment is an initial fragment
        if (!item.getNodes().isEmpty()) {
            Node n = item.getNodes().get(0);
            if (n.getClass() != Consumer.class || n.getCorrelationIds().isEmpty()) {
                TraceCompletionInformation ci = new TraceCompletionInformation();

                // Create the initial version of the completion time
                CompletionTime ct = new CompletionTime();
                ct.setId(item.getId());
                ct.setUri(n.getUri());
                ct.setOperation(n.getOperation());

                if (n.getClass() == Consumer.class) {
                    ct.setEndpointType(((Consumer) n).getEndpointType());
                }

                ct.setBusinessTransaction(item.getBusinessTransaction());
                ct.setDuration(item.calculateDuration());
                ct.setPrincipal(item.getPrincipal());
                ct.setFault(n.getFault());
                ct.setProperties(item.allProperties());
                ct.setTimestamp(item.getStartTime());

                ci.setCompletionTime(ct);

                // Initialise any communications that need to be further processed
                TraceCompletionInformationUtil.initialiseLinks(ci, n.getBaseTime(), n);

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Creating initial completion time information = " + ci);
                }

                return ci;
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("No completion information initiated for trace fragment = " + item);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processMultiple(java.lang.String,java.lang.Object)
     */
    @Override
    public List<TraceCompletionInformation> processOneToMany(String tenantId,
            Trace item) throws RetryAttemptException {
        return null;
    }
}
