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
package org.hawkular.btm.processor.btxncompletiontime;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.server.api.task.AbstractProcessor;

/**
 * This class represents the function for initiating completion time calculation based on
 * detecting an initial business transaction fragment.
 *
 * @author gbrown
 */
public class BTxnCompletionInformationInitiator extends
                    AbstractProcessor<BusinessTransaction, BTxnCompletionInformation> {

    private static final Logger log = Logger.getLogger(BTxnCompletionInformationInitiator.class.getName());

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processSingle(java.lang.String,java.lang.Object)
     */
    @Override
    public BTxnCompletionInformation processSingle(String tenantId,
                            BusinessTransaction item) throws Exception {
        // Check whether the business transaction fragment is an initial fragment
        if (!item.getNodes().isEmpty()) {
            Node n = item.getNodes().get(0);
            if (n.getClass() != Consumer.class || n.getCorrelationIds(Scope.Interaction).isEmpty()) {
                BTxnCompletionInformation ci = new BTxnCompletionInformation();

                // Create the initial version of the completion time
                CompletionTime ct = new CompletionTime();
                ct.setId(item.getId());
                ct.setUri(n.getUri());
                ct.setOperation(n.getOperation());

                if (n.getClass() == Consumer.class) {
                    ct.setEndpointType(((Consumer)n).getEndpointType());
                }

                ct.setBusinessTransaction(item.getName());
                ct.setDuration(item.calculateDuration());
                ct.setPrincipal(item.getPrincipal());
                ct.setFault(n.getFault());
                ct.setProperties(item.getProperties());
                ct.setTimestamp(item.getStartTime());

                ci.setCompletionTime(ct);

                // Initialise any communications that need to be further processed
                BTxnCompletionInformationUtil.initialiseCommunications(ci, item.getNodes().get(0).getBaseTime(),
                        0, n);

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Creating initial completion time information = " + ci);
                }

                return ci;
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("No completion information initiated for business txn fragment = " + item);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.String,java.lang.Object)
     */
    @Override
    public List<BTxnCompletionInformation> processMultiple(String tenantId,
                                BusinessTransaction item) throws Exception {
        return null;
    }
}
