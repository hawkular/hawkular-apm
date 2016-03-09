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

import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.server.api.task.AbstractProcessor;

/**
 * This class represents the end to end business transaction completion time deriver.
 *
 * @author gbrown
 */
public class BTxnCompletionTimeDeriver extends AbstractProcessor<BTxnCompletionInformation, CompletionTime> {

    private static final Logger log = Logger.getLogger(BTxnCompletionTimeDeriver.class.getName());

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
    public CompletionTime processSingle(String tenantId, BTxnCompletionInformation item) throws Exception {
        // Check if named txn
        if (item.getCommunications().isEmpty()) {

            if (log.isLoggable(Level.FINEST)) {
                log.finest("CompletionTimeDeriver ret=" + item.getCompletionTime());
            }

            return item.getCompletionTime();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.String,java.lang.Object)
     */
    @Override
    public List<CompletionTime> processMultiple(String tenantId, BTxnCompletionInformation item) throws Exception {
        return null;
    }
}
