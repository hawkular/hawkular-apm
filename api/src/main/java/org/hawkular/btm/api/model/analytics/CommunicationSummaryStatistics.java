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
package org.hawkular.btm.api.model.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents summary statistics for communications
 * associated with a particular URI.
 *
 * @author gbrown
 */
public class CommunicationSummaryStatistics {

    private String uri;

    private long minimumDuration;

    private long averageDuration;

    private long maximumDuration;

    private long count;

    private Map<String, ConnectionStatistics> outbound = new HashMap<String, ConnectionStatistics>();

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
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
        return "CommunicationSummaryStatistics [uri=" + uri + ", minimumDuration=" + minimumDuration
                + ", averageDuration=" + averageDuration + ", maximumDuration=" + maximumDuration + ", count=" + count
                + ", outbound=" + outbound + "]";
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

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "ConnectionStatistics [minimumLatency=" + minimumLatency + ", averageLatency=" + averageLatency
                    + ", maximumLatency=" + maximumLatency + ", count=" + count + "]";
        }

    }
}
