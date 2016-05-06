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
package org.hawkular.btm.api.services.internal;

import java.util.Collection;

import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics.ConnectionStatistics;

/**
 * The class analyses the communication summary to determine the severity values
 * for the nodes and links.
 *
 * @author gbrown
 */
public class CommunicationSeverityAnalyser {

    // NOTE: May want to provide a pluggable algorithm, but for now just implement
    // a basic approach

    /**  */
    private static final int MAX_SEVERITY = 5;

    /**
     * This method evaluates the severity of nodes/links within a supplied set of
     * communication summary stats trees.
     *
     * @param rootNodes The root nodes for the communication summary trees
     */
    public void evaluateCommunicationSummarySeverity(Collection<CommunicationSummaryStatistics> rootNodes) {
        long max = 0;

        for (CommunicationSummaryStatistics css : rootNodes) {
            long cssMax = maxAverageDuration(css);
            if (cssMax > max) {
                max = cssMax;
            }
        }

        for (CommunicationSummaryStatistics css : rootNodes) {
            deriveSeverity(css, max);
        }
    }

    protected static long maxAverageDuration(CommunicationSummaryStatistics css) {
        long max = css.getAverageDuration();
        for (ConnectionStatistics cs : css.getOutbound().values()) {
            if (cs.getNode() != null) {
                long nodeMax = maxAverageDuration(cs.getNode());
                if (nodeMax > max) {
                    max = nodeMax;
                }
            }
        }
        return max;
    }

    protected static void deriveSeverity(CommunicationSummaryStatistics css, long max) {
        int relative = (int) (css.getAverageDuration() * 10.0 / max);

        if (relative >= 10) {
            css.setSeverity(MAX_SEVERITY);
        } else if (relative <= 3) {
            css.setSeverity(0);
        } else {
            relative -= 3;
            css.setSeverity(((int) (relative * 0.7)) + 1);
            if (css.getSeverity() > MAX_SEVERITY) {
                css.setSeverity(MAX_SEVERITY);
            }
        }
        for (ConnectionStatistics cs : css.getOutbound().values()) {
            if (cs.getNode() != null) {
                deriveSeverity(cs.getNode(), max);
            }
        }
    }

}
