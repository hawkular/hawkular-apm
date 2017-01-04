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

    /**
     * Minimum duration in microseconds
     */
    private long minimumDuration;

    /**
     * Average duration in microseconds
     */
    private long averageDuration;

    /**
     * Maximum duration in microseconds
     */
    private long maximumDuration;

    private long count;

    private int severity = 0;

    private String uri;

    private String operation;

    /**
     * Service name reported by instrumentation agent
     */
    private String serviceName;

    private Map<String, ConnectionStatistics> outbound = new HashMap<>();

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
        this.uri = node.uri;
        this.operation = node.operation;
        this.serviceName = node.getServiceName();
        for (Map.Entry<String, ConnectionStatistics> entry: node.getOutbound().entrySet()) {
            this.outbound.put(entry.getKey(), new ConnectionStatistics(entry.getValue()));
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
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
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

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "CommunicationSummaryStatistics [id=" + id + ", minimumDuration=" + minimumDuration
                + ", averageDuration=" + averageDuration + ", maximumDuration=" + maximumDuration + ", count=" + count
                + ", severity=" + severity + ", uri=" + uri + ", operation=" + operation + " , serviceName="
                + serviceName + ", outbound=" + outbound + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (averageDuration ^ (averageDuration >>> 32));
        result = prime * result + (int) (count ^ (count >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (int) (maximumDuration ^ (maximumDuration >>> 32));
        result = prime * result + (int) (minimumDuration ^ (minimumDuration >>> 32));
        result = prime * result + ((operation == null) ? 0 : operation.hashCode());
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        result = prime * result + ((outbound == null) ? 0 : outbound.hashCode());
        result = prime * result + severity;
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommunicationSummaryStatistics other = (CommunicationSummaryStatistics) obj;
        if (averageDuration != other.averageDuration)
            return false;
        if (count != other.count)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (maximumDuration != other.maximumDuration)
            return false;
        if (minimumDuration != other.minimumDuration)
            return false;
        if (operation == null) {
            if (other.operation != null)
                return false;
        } else if (!operation.equals(other.operation))
            return false;
        if (serviceName == null) {
            if (other.serviceName != null)
                return false;
        } else if (!serviceName.equals(other.serviceName))
            return false;
        if (outbound == null) {
            if (other.outbound != null)
                return false;
        } else if (!outbound.equals(other.outbound))
            return false;
        if (severity != other.severity)
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
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

        @Override
        public String toString() {
            return "ConnectionStatistics [minimumLatency=" + minimumLatency + ", averageLatency=" + averageLatency
                    + ", maximumLatency=" + maximumLatency + ", count=" + count + ", severity=" + severity + ", node="
                    + node + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (averageLatency ^ (averageLatency >>> 32));
            result = prime * result + (int) (count ^ (count >>> 32));
            result = prime * result + (int) (maximumLatency ^ (maximumLatency >>> 32));
            result = prime * result + (int) (minimumLatency ^ (minimumLatency >>> 32));
            result = prime * result + ((node == null) ? 0 : node.hashCode());
            result = prime * result + severity;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ConnectionStatistics other = (ConnectionStatistics) obj;
            if (averageLatency != other.averageLatency)
                return false;
            if (count != other.count)
                return false;
            if (maximumLatency != other.maximumLatency)
                return false;
            if (minimumLatency != other.minimumLatency)
                return false;
            if (node == null) {
                if (other.node != null)
                    return false;
            } else if (!node.equals(other.node))
                return false;
            if (severity != other.severity)
                return false;
            return true;
        }

    }
}
