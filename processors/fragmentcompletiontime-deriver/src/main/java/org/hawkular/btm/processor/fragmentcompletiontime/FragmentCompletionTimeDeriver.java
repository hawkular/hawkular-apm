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
package org.hawkular.btm.processor.fragmentcompletiontime;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.server.api.task.AbstractProcessor;

/**
 * This class represents the fragment completion time deriver.
 *
 * @author gbrown
 */
public class FragmentCompletionTimeDeriver extends AbstractProcessor<BusinessTransaction, CompletionTime> {

    private static final Logger log = Logger.getLogger(FragmentCompletionTimeDeriver.class.getName());

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public CompletionTime processSingle(String tenantId, BusinessTransaction item) throws Exception {
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
                        || ((Consumer)n).getEndpointType().trim().length() == 0);
            }

            ct.setBusinessTransaction(item.getName());
            ct.setDuration(item.calculateDuration());
            ct.setPrincipal(item.getPrincipal());
            ct.setFault(n.getFault());
            ct.setProperties(item.getProperties());
            ct.setTimestamp(item.getStartTime());

            if (log.isLoggable(Level.FINEST)) {
                log.finest("FragmentCompletionTimeDeriver ret=" + ct);
            }
            return ct;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.Object)
     */
    @Override
    public List<CompletionTime> processMultiple(String tenantId, BusinessTransaction item) throws Exception {
        return null;
    }
}
