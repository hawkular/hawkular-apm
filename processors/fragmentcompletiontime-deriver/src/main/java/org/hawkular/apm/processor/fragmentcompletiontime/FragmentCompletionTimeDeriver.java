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
package org.hawkular.apm.processor.fragmentcompletiontime;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the fragment completion time deriver.
 *
 * @author gbrown
 */
public class FragmentCompletionTimeDeriver extends AbstractProcessor<Trace, CompletionTime> {

    private static final Logger log = Logger.getLogger(FragmentCompletionTimeDeriver.class.getName());

    /**
     * The default constructor.
     */
    public FragmentCompletionTimeDeriver() {
        super(ProcessorType.OneToOne);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public CompletionTime processOneToOne(String tenantId, Trace item) throws RetryAttemptException {
        // Check fragment has top level node
        if (!item.getNodes().isEmpty()) {
            Node n = item.getNodes().get(0);

            CompletionTime ct = new CompletionTime();
            ct.setId(item.getId());
            ct.setUri(n.getUri());
            ct.setOperation(n.getOperation());

            if (n.getClass() == Consumer.class) {
                ct.setEndpointType(((Consumer)n).getEndpointType());
                ct.setInternal(((Consumer)n).getEndpointType() == null
                        || ((Consumer) n).getEndpointType().trim().isEmpty());
            }

            ct.setBusinessTransaction(item.getBusinessTransaction());
            ct.setDuration(item.calculateDuration());
            ct.setPrincipal(item.getPrincipal());
            ct.setFault(n.getFault());
            ct.setHostName(item.getHostName());
            ct.setProperties(item.getProperties());
            ct.setTimestamp(item.getStartTime());

            if (log.isLoggable(Level.FINEST)) {
                log.finest("FragmentCompletionTimeDeriver ret=" + ct);
            }
            return ct;
        }
        return null;
    }

}
