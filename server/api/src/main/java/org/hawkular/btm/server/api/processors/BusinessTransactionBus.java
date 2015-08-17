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
package org.hawkular.btm.server.api.processors;

import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.trace.BusinessTransactionTrace;

/**
 * This interface represents a bus used to distribute business transaction
 * fragments and traces to modules interested in processing the information.
 *
 * @author gbrown
 */
public interface BusinessTransactionBus {

    /**
     * This method is invoked to process a list of business transaction
     * fragments.
     *
     * @param tenantId The tenant
     * @param btxns The business transaction fragments
     */
    void processFragments(String tenantId, List<BusinessTransaction> btxns);

    /**
     * This method is invoked to process a list of business transaction
     * traces.
     *
     * @param tenantId The tenant
     * @param traces The business transaction traces
     */
    void processTraces(String tenantId, List<BusinessTransactionTrace> traces);

}
