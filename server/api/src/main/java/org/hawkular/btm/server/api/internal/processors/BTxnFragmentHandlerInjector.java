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
package org.hawkular.btm.server.api.internal.processors;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hawkular.btm.server.api.processors.BusinessTransactionFragmentHandler;

/**
 * This class produces the list of business transaction fragment handlers.
 *
 * @author gbrown
 */
public class BTxnFragmentHandlerInjector {

    @Inject
    private Instance<BusinessTransactionFragmentHandler> injectedProcessors;

    @Produces
    public List<BusinessTransactionFragmentHandler> getBTxnFragmentProcessors() {
        List<BusinessTransactionFragmentHandler> ret = new ArrayList<BusinessTransactionFragmentHandler>();

        for (BusinessTransactionFragmentHandler processor : injectedProcessors) {
            ret.add(processor);
        }

        return ret;
    }
}
