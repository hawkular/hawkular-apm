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
package org.hawkular.btm.processor.faults.internal;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hawkular.btm.processor.faults.FaultHandler;
import org.hawkular.btm.processor.faults.log.MsgLogger;

/**
 * This class produces the list of fault handlers.
 *
 * @author gbrown
 */
public class FaultHandlerInjector {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    @Inject
    private Instance<FaultHandler> faultHandlers;

    @Produces
    public List<FaultHandler> getFaultHandlers() {
        List<FaultHandler> ret = new ArrayList<FaultHandler>();

        for (FaultHandler handler : faultHandlers) {
            ret.add(handler);
        }

        if (ret.isEmpty()) {
            msgLog.warnNoFaultHandlers();
        }

        return ret;
    }
}
