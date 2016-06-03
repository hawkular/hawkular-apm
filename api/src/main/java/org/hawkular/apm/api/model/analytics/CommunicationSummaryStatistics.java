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
package org.hawkular.apm.api.model.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents summary statistics for communications
 * associated with a particular URI.
 *
 * @author gbrown
 */
public class CommunicationSummaryStatistics {

    private String id;

    private long minimumDuration;

    private long averageDuration;

    private long maximumDuration;

    private long count;

    private int severity = 0;

    private Map<String, ConnectionStatistics> outbound = new HashMap<String, ConnectionStatistics>();

    /**
     * The default constructor.
     */
    public CommunicationSummaryStatistics() {
    }

    /**
     * The copy constructor.
     *
     * @param node The node to copy
     */
    public CommunicationSummaryStatistics(CommunicationSummaryStatistics node) {
        this.id = node.id;
        this.minimumDuration = node.minimumDuration;
        this.averageDuration = node.averageDuration;
        this.maximumDuration = node.maximumDuration;
        this.count = node.count;
        this.severity = node.severity;
        for (String id : node.getOutbound().keySet()) {
            this.outbound.put(id, new ConnectionStatistics(node.getOutbound().get(id)));
        }
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the minimumDuration
     */
    public long getMinimumDuration() {
        return minimumDuration;
    }

    /**
     * @param minimumDuration the minimumDuration to set
     */
    public void setMinimumDuration(long minimumDuration) {
        this.minimumDuration = minimumDuration;
    }

    /**
     * @return the averageDuration
     */
    public long getAverageDuration() {
        return averageDuration;
    }

    /**
     * @param averageDuration the averageDuration to set
     */
    public void setAverageDuration(long averageDuration) {
        this.averageDuration = averageDuration;
    }

    /**
     * @return the maximumDuration
     */
    public long getMaximumDuration() {
        return maximumDuration;
    }

    /**
     * @param maximumDuration the maximumDuration to set
     */
    public void setMaximumDuration(long maximumDuration) {
        this.maximumDuration = maximumDuration;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * @return the severity
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * @param severity the severity to set
     */
    public void setSeverity(int severity) {
        this.severity = severity;
    }

    /**
     * @return the outbound
     */
    public Map<String, ConnectionStatistics> getOutbound() {
        return outbound;
    }

    /**
     * @param outbound the outbound to set
     */
    public void setOutbound(Map<String, ConnectionStatistics> outbound) {
        this.outbound = outbound;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CommunicationSummaryStatistics [id=" + id + ", minimumDuration=" + minimumDuration
                + ", averageDuration=" + averageDuration + ", maximumDuration=" + maximumDuration + ", count=" + count
                + ", severity=" + severity + ", outbound=" + outbound + "]";
    }

    /**
     * This class represents the stats associated with an outbound communication
     * channel to another node.
     *
     * @author gbrown
     */
    public static class ConnectionStatistics {

        private long minimumLatency;

        private long averageLatency;

        private long maximumLatency;

        private long count;

        private int severity = 0;

        private CommunicationSummaryStatistics node;

        /**
         * The default constructor.
         */
        public ConnectionStatistics() {
        }

        /**
         * The copy constructor.
         *
         * @param cs The object to copy
         */
        public ConnectionStatistics(ConnectionStatistics cs) {
            this.minimumLatency = cs.minimumLatency;
            this.averageLatency = cs.averageLatency;
            this.maximumLatency = cs.maximumLatency;
            this.count = cs.count;
            this.severity = cs.severity;
            if (cs.node != null) {
                this.node = new CommunicationSummaryStatistics(cs.node);
            }
        }

        /**
         * @return the minimumLatency
         */
        public long getMinimumLatency() {
            return minimumLatency;
        }

        /**
         * @param minimumLatency the minimumLatency to set
         */
        public void setMinimumLatency(long minimumLatency) {
            this.minimumLatency = minimumLatency;
        }

        /**
         * @return the averageLatency
         */
        public long getAverageLatency() {
            return averageLatency;
        }

        /**
         * @param averageLatency the averageLatency to set
         */
        public void setAverageLatency(long averageLatency) {
            this.averageLatency = averageLatency;
        }

        /**
         * @return the maximumLatency
         */
        public long getMaximumLatency() {
            return maximumLatency;
        }

        /**
         * @param maximumLatency the maximumLatency to set
         */
        public void setMaximumLatency(long maximumLatency) {
            this.maximumLatency = maximumLatency;
        }

        /**
         * @return the count
         */
        public long getCount() {
            return count;
        }

        /**
         * @param count the count to set
         */
        public void setCount(long count) {
            this.count = count;
        }

        /**
         * @return the severity
         */
        public int getSeverity() {
            return severity;
        }

        /**
         * @param severity the severity to set
         */
        public void setSeverity(int severity) {
            this.severity = severity;
        }

        /**
         * @return the node
         */
        public CommunicationSummaryStatistics getNode() {
            return node;
        }

        /**
         * @param node the node to set
         */
        public void setNode(CommunicationSummaryStatistics node) {
            this.node = node;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "ConnectionStatistics [minimumLatency=" + minimumLatency + ", averageLatency=" + averageLatency
                    + ", maximumLatency=" + maximumLatency + ", count=" + count + ", severity=" + severity + ", node="
                    + node + "]";
        }

    }
}
