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
package org.hawkular.apm.api.services.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;

/**
 * The class analyses the communication summary to determine the severity values
 * for the nodes and links.
 *
 * @author gbrown
 */
public class CommunicationSeverityAnalyser {

    // NOTE: May want to provide a pluggable algorithm, but for now just implement
    // a basic approach

    private static final int MAX_SEVERITY = 5;

    /**
     * This method evaluates the severity of nodes/links within a supplied set of
     * communication summary stats.
     *
     * @param nodes The nodes for the communication summary stats
     */
    public void evaluateCommunicationSummarySeverity(Collection<CommunicationSummaryStatistics> nodes) {
        long max = 0;
        Map<String, CommunicationSummaryStatistics> nodeMap = new HashMap<String, CommunicationSummaryStatistics>();

        for (CommunicationSummaryStatistics css : nodes) {
            // Calculate maximum average duration over the list of nodes
            if (css.getAverageDuration() > max) {
                max = css.getAverageDuration();
            }
            nodeMap.put(css.getId(), css);
        }

        for (CommunicationSummaryStatistics css : nodes) {
            deriveSeverity(css, max, nodeMap);
        }
    }

    protected static void deriveSeverity(CommunicationSummaryStatistics css, long max,
            Map<String, CommunicationSummaryStatistics> nodeMap) {
        max = findParentsMaxAvgDuration(css, max, nodeMap);

        int relative = (int) (css.getAverageDuration() * 10.0 / max);

        if (relative >= 10) {
            css.setSeverity(MAX_SEVERITY);
        } else if (relative <= 1) {
            css.setSeverity(0);
        } else {
            relative -= 1;
            css.setSeverity(((int) (relative * 0.5)) + 1);
            if (css.getSeverity() > MAX_SEVERITY) {
                css.setSeverity(MAX_SEVERITY);
            }
        }
    }

    protected static long findParentsMaxAvgDuration(CommunicationSummaryStatistics css, long max,
            Map<String, CommunicationSummaryStatistics> nodeMap) {
        long parentMax = max;
        List<CommunicationSummaryStatistics> parents = new ArrayList<CommunicationSummaryStatistics>();
        for (CommunicationSummaryStatistics parent : nodeMap.values()) {
            if (!parent.getId().equals(css.getId())) {
                for (String id : parent.getOutbound().keySet()) {
                    if (id.equals(css.getId())) {
                        parents.add(parent);
                    }
                }
            }
        }

        if (!parents.isEmpty()) {
            if (parents.size() == 1) {
                parentMax = parents.get(0).getAverageDuration();
            } else {
                for (CommunicationSummaryStatistics parent : parents) {
                    parentMax += parent.getAverageDuration();
                }
                parentMax /= parents.size();
            }
        }

        return parentMax;
    }
}
