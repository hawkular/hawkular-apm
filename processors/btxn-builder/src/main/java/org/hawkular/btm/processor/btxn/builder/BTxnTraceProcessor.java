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
package org.hawkular.btm.processor.btxn.builder;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;

/**
 * This class provides the capabilities for building an end to end trace from
 * business transaction fragments.
 *
 * @author gbrown
 */
public class BTxnTraceProcessor {

    /**
     * This method examines the supplied business transaction to determine if it
     * represents the start of a trace, and if so, returns the initialised trace.
     *
     * @param fragment The business transaction fragment
     * @return The trace, or null if the fragment does not initiate a trace
     * @throws Exception Failed to initialise the trace
     */
    public BusinessTransactionTrace init(BusinessTransaction fragment) throws Exception {
        BusinessTransactionTrace trace = new BusinessTransactionTrace();
        trace.setTrace(fragment);
        return trace;
    }

    /**
     * This method evaluates the pending correlations that need to be resolved, and
     * attempts to locate the relevant business transaction fragments.
     *
     * @param trace The trace to be processed
     * @return The updated trace
     * @throws Exception Failed to process the trace
     */
    public BusinessTransactionTrace process(BusinessTransactionTrace trace) throws Exception {
        return trace;
    }

    /**
     * This method identifies the interval to wait until processing the supplied
     * trace again.
     *
     * @param trace The trace
     * @return The schedule interval
     */
    public long getScheduleInterval(BusinessTransactionTrace trace) {
        return (100);
    }

}
