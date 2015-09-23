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
package org.hawkular.btm.processor.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.analytics.ResponseTime;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.InteractionNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.server.api.task.Processor;

/**
 * This class represents the response time deriver.
 *
 * @author gbrown
 */
public class ResponseTimeDeriver implements Processor<BusinessTransaction, ResponseTime> {

    private static final Logger log = Logger.getLogger(ResponseTimeDeriver.class.getName());

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#isMultiple()
     */
    @Override
    public boolean isMultiple() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processSingle(java.lang.Object)
     */
    @Override
    public ResponseTime processSingle(BusinessTransaction item) throws Exception {
        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.task.Processor#processMultiple(java.lang.Object)
     */
    @Override
    public List<ResponseTime> processMultiple(BusinessTransaction item) throws Exception {
        List<ResponseTime> ret = new ArrayList<ResponseTime>();

        long baseTime = 0;
        if (!item.getNodes().isEmpty()) {
            baseTime = item.getNodes().get(0).getBaseTime();
        }

        deriveResponseTimes(item, baseTime, item.getNodes(), ret);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("ResponseTimeDeriver [" + ret.size() + "] ret=" + ret);
        }

        return ret;
    }

    /**
     * This method recursively derives the response time metrics for the supplied
     * nodes.
     *
     * @param btxn The business transaction
     * @param baseTime The base time, in nanoseconds, for the business transaction
     * @param nodes The nodes
     * @param rts The list of response times
     */
    protected void deriveResponseTimes(BusinessTransaction btxn, long baseTime,
            List<Node> nodes, List<ResponseTime> rts) {
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            long diffns = n.getBaseTime() - baseTime;
            long diffms = TimeUnit.MILLISECONDS.convert(diffns, TimeUnit.NANOSECONDS);

            ResponseTime rt = new ResponseTime();
            rt.setCorrelationIds(n.getCorrelationIds());
            rt.setDetails(n.getDetails());
            rt.setDuration(n.getDuration());
            rt.setFault(n.getFault());
            rt.setProperties(btxn.getProperties());
            rt.setTimestamp(btxn.getStartTime() + diffms);
            rt.setType(n.getType());
            rt.setUri(n.getUri());

            rts.add(rt);

            if (n.interactionNode()) {
                deriveResponseTimes(btxn, baseTime, ((InteractionNode) n).getNodes(), rts);
            }
        }
    }
}
