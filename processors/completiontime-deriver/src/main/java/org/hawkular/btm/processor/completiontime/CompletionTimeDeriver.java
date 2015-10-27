/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.processor.completiontime;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.analytics.CompletionTime;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.server.api.task.Processor;

/**
 * This class represents the completion time deriver.
 *
 * @author gbrown
 */
public class CompletionTimeDeriver implements Processor<BusinessTransaction, CompletionTime> {

    private static final Logger log = Logger.getLogger(CompletionTimeDeriver.class.getName());

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
    public CompletionTime processSingle(BusinessTransaction item) throws Exception {
        // Check if named txn and first node has an interaction correlaton id
        if (item.getName() != null && item.getName().trim().length() > 0 && !item.getNodes().isEmpty()) {
            Node n = item.getNodes().get(0);
            boolean interaction = false;
            for (int i = 0; !interaction && i < n.getCorrelationIds().size(); i++) {
                interaction = n.getCorrelationIds().get(i).getScope() == Scope.Interaction;
            }
            if (!interaction) {
                CompletionTime ct = new CompletionTime();
                ct.setId(item.getId());
                ct.setBusinessTransaction(item.getName());
                ct.setDuration(n.getDuration());
                ct.setFault(n.getFault());
                ct.setProperties(item.getProperties());
                ct.setTimestamp(item.getStartTime());

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("CompletionTimeDeriver ret=" + ct);
                }
                return ct;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.Object)
     */
    @Override
    public List<CompletionTime> processMultiple(BusinessTransaction item) throws Exception {
        return null;
    }
}
