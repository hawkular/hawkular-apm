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
package org.hawkular.apm.api.internal.actions;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.txn.AddContentAction;
import org.hawkular.apm.api.model.config.txn.AddCorrelationIdAction;
import org.hawkular.apm.api.model.config.txn.EvaluateURIAction;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.config.txn.SetPropertyAction;

/**
 * This class provides a factory for creating handlers associated with processor actions.
 *
 * @author gbrown
 */
public class ProcessorActionHandlerFactory {

    private static final Logger log = Logger.getLogger(ProcessorActionHandlerFactory.class.getName());

    private static Map<Class<? extends ProcessorAction>, Class<? extends ProcessorActionHandler>> handlers;

    static {
        handlers = new HashMap<Class<? extends ProcessorAction>, Class<? extends ProcessorActionHandler>>();

        handlers.put(AddContentAction.class, AddContentActionHandler.class);
        handlers.put(AddCorrelationIdAction.class, AddCorrelationIdActionHandler.class);
        handlers.put(EvaluateURIAction.class, EvaluateURIActionHandler.class);
        handlers.put(SetPropertyAction.class, SetPropertyActionHandler.class);
    }

    /**
     * This method returns an action handler for the supplied action.
     *
     * @param action The action
     * @return The handler
     */
    public static ProcessorActionHandler getHandler(ProcessorAction action) {
        ProcessorActionHandler ret = null;
        Class<? extends ProcessorActionHandler> cls = handlers.get(action.getClass());
        if (cls != null) {
            try {
                Constructor<? extends ProcessorActionHandler> con = cls.getConstructor(ProcessorAction.class);
                ret = con.newInstance(action);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to instantiate handler for action '" + action + "'", e);
            }
        }
        return ret;
    }

}
