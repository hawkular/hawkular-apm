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
package org.hawkular.btm.api.model.trace;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;

/**
 * This class repreents the wrapper for a business transaction trace, providing additional
 * metadata to help construct the end to end business transaction.
 *
 * @author gbrown
 */
public class BusinessTransactionTrace {

    // TODO: Add business transaction definition to provide details

    private BusinessTransaction trace;

    /**
     * @return the trace
     */
    public BusinessTransaction getTrace() {
        return trace;
    }

    /**
     * @param trace the trace to set
     */
    public void setTrace(BusinessTransaction trace) {
        this.trace = trace;
    }

    /**
     * This method determines whether the trace is complete. If
     * there are outstanding correlation details to be resolved,
     * then the trace will be considered incomplete.
     *
     * @return Whether the trace is complete
     */
    public boolean isComplete() {
        // TODO: When pending connection info recorded, use that
        // to determine if completed
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BusinessTransactionTrace [trace=" + trace + ", complete=" + isComplete() + "]";
    }
}
