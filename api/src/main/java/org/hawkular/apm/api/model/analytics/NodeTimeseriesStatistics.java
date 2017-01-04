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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class represents a set of node summary statistical values.
 *
 * @author gbrown
 */
public class NodeTimeseriesStatistics {

    /**
     * Timestamp in microseconds
     */
    @JsonInclude
    private long timestamp = 0;

    @JsonInclude
    private Map<String, NodeComponentTypeStatistics> componentTypes =
            new HashMap<String, NodeComponentTypeStatistics>();

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the componentTypes
     */
    public Map<String, NodeComponentTypeStatistics> getComponentTypes() {
        return componentTypes;
    }

    /**
     * @param componentTypes the componentTypes to set
     */
    public void setComponentTypes(Map<String, NodeComponentTypeStatistics> componentTypes) {
        this.componentTypes = componentTypes;
    }

    @Override
    public String toString() {
        return "NodeTimeseriesStatistics [timestamp=" + timestamp + ", componentTypes=" + componentTypes + "]";
    }

    /**
     * This class represents the stats associated with a node's component type.
     *
     * @author gbrown
     */
    public static class NodeComponentTypeStatistics {

        /**
         * Duration in microseconds
         */
        private long duration;

        private long count;

        /**
         * The default constructor.
         */
        public NodeComponentTypeStatistics() {
        }

        /**
         * This constructor initialises the stats.
         *
         * @param duration The duration
         * @param count The count
         */
        public NodeComponentTypeStatistics(long duration, long count) {
            this.duration = duration;
            this.count = count;
        }

        /**
         * @return the duration
         */
        public long getDuration() {
            return duration;
        }

        /**
         * @param duration the duration to set
         */
        public void setDuration(long duration) {
            this.duration = duration;
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

        @Override
        public String toString() {
            return "NodeComponentTypeStatistics [duration=" + duration + ", count=" + count + "]";
        }

    }
}
