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

package org.hawkular.apm.server.processor.zipkin;

import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;
import org.jboss.logging.Logger;

/**
 * @author Pavol Loffay
 */
public class CompletionTimeDeriver extends AbstractProcessor<CompletionTimeProcessing, CompletionTime> {

    private static final Logger log = Logger.getLogger(CompletionTimeDeriver.class);


    public CompletionTimeDeriver() {
        super(ProcessorType.OneToOne);
    }

    @Override
    public CompletionTime processOneToOne(String tenantId, CompletionTimeProcessing completionTimeProcessing)
            throws RetryAttemptException {

        if (completionTimeProcessing.getCompletionTime() == null) {
            return null;
        }

        log.debugf("SpanTraceCompletionTimeDeriver completionTime[%s]",
                completionTimeProcessing.getCompletionTime());

        return completionTimeProcessing.getCompletionTime();
    }
}
