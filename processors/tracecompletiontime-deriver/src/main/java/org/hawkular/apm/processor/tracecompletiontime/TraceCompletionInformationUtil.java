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
package org.hawkular.apm.processor.tracecompletiontime;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;

/**
 * This class represents utility functions to help calculate completion time for
 * trace instances.
 *
 * @author gbrown
 */
public class TraceCompletionInformationUtil {

    private static final Logger log = Logger.getLogger(TraceCompletionInformationUtil.class.getName());

    /**
     * This method initialises the completion time information for a trace
     * instance.
     *
     * @param ci The information
     * @param fragmentBaseTime The base time for the fragment (ns)
     * @param baseDuration The base duration (ms)
     * @param n The node
     */
    public static void initialiseLinks(TraceCompletionInformation ci, long fragmentBaseTime,
            long baseDuration, Node n) {
        if (n.getClass() == Producer.class) {
            // Get interaction id
            List<CorrelationIdentifier> cids = n.findCorrelationIds(Scope.Interaction, Scope.Association);

            if (!cids.isEmpty()) {
                TraceCompletionInformation.Communication c = new TraceCompletionInformation.Communication();

                for (int i = 0; i < cids.size(); i++) {
                    c.getIds().add(cids.get(i).getValue());
                }

                c.setMultipleConsumers(((Producer) n).multipleConsumers());

                // Calculate the base duration for the communication
                c.setBaseDuration(baseDuration + TimeUnit.MILLISECONDS.convert((n.getBaseTime() - fragmentBaseTime),
                        TimeUnit.NANOSECONDS));

                c.setExpire(System.currentTimeMillis() + TraceCompletionInformation.Communication.DEFAULT_EXPIRY_WINDOW);

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Adding communication to completion information: ci=" + ci + " comms=" + c);
                }

                ci.getCommunications().add(c);
            }
        } else if (n.containerNode()) {
            ContainerNode cn = (ContainerNode) n;
            for (int i = 0; i < cn.getNodes().size(); i++) {
                initialiseLinks(ci, fragmentBaseTime, baseDuration, cn.getNodes().get(i));
            }
        }
    }

}
