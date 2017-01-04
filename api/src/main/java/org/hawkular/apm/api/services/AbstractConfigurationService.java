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
package org.hawkular.apm.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.internal.actions.ProcessorActionHandler;
import org.hawkular.apm.api.internal.actions.ProcessorActionHandlerFactory;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;

/**
 * This class provides the abstract base class for the Configuration Service.
 *
 * @author gbrown
 */
public abstract class AbstractConfigurationService implements ConfigurationService {

    private static final String NO_FILTERS = "No inclusion or exclusion filters have been defined";

    @Override
    public List<ConfigMessage> validateTransaction(TransactionConfig config) {
        List<ConfigMessage> messages = new ArrayList<ConfigMessage>();

        // Check that atleast one filter has been defined
        if (config.getFilter() == null || (config.getFilter().getInclusions().isEmpty()
                && config.getFilter().getInclusions().isEmpty())) {

            ConfigMessage cm = new ConfigMessage();
            cm.setMessage(NO_FILTERS);
            messages.add(cm);
        }

        for (Processor processor : config.getProcessors()) {
            for (ProcessorAction action : processor.getActions()) {
                ProcessorActionHandler handler = ProcessorActionHandlerFactory.getHandler(action);

                if (handler != null) {
                    messages.addAll(handler.init(processor));
                }
            }
        }

        return messages;
    }

    @Override
    public List<ConfigMessage> setTransactions(String tenantId, Map<String, TransactionConfig> configs)
            throws Exception {
        List<ConfigMessage> messages = new ArrayList<ConfigMessage>();

        for (Map.Entry<String, TransactionConfig> stringBusinessTxnConfigEntry : configs.entrySet()) {
            messages.addAll(setTransaction(tenantId, stringBusinessTxnConfigEntry.getKey(), stringBusinessTxnConfigEntry.getValue()));
        }

        return messages;
    }

}
