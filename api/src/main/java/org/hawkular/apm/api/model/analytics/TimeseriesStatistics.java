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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class represents a set of statistical values.
 *
 * @author gbrown
 */
public class TimeseriesStatistics {

    /**
     * Timestamp in microseconds
     */
    @JsonInclude
    private long timestamp = 0;

    @JsonInclude
    private long count = 0;

    @JsonInclude
    private long faultCount = 0;

    /**
     * Minimal duration in microseconds
     */
    @JsonInclude
    private long min = 0;

    /**
     * Average duration in microseconds
     */
    @JsonInclude
    private long average = 0;

    /**
     * Maximal duration in microseconds
     */
    @JsonInclude
    private long max = 0;

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
     * @return the faultCount
     */
    public long getFaultCount() {
        return faultCount;
    }

    /**
     * @param faultCount the faultCount to set
     */
    public void setFaultCount(long faultCount) {
        this.faultCount = faultCount;
    }

    /**
     * @return the min
     */
    public long getMin() {
        return min;
    }

    /**
     * @param min the min to set
     */
    public void setMin(long min) {
        this.min = min;
    }

    /**
     * @return the average
     */
    public long getAverage() {
        return average;
    }

    /**
     * @param average the average to set
     */
    public void setAverage(long average) {
        this.average = average;
    }

    /**
     * @return the max
     */
    public long getMax() {
        return max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(long max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "TimeseriesStatistics [timestamp=" + timestamp + ", count=" + count + ", faultCount="
                + faultCount + ", min=" + min + ", average=" + average + ", max=" + max + "]";
    }

}
