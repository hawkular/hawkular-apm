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
package org.hawkular.apm.server.processor.tracecompletiontime;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.events.CompletionTime;

/**
 * This class represents the intermediate information required to calculate
 * the completion time for a trace instance.
 *
 * @author gbrown
 */
public class TraceCompletionInformation {

    private CompletionTime completionTime;
    private List<Communication> communications = new ArrayList<Communication>();

    /**
     * @return the completionTime
     */
    public CompletionTime getCompletionTime() {
        return completionTime;
    }

    /**
     * @param completionTime the completionTime to set
     */
    public void setCompletionTime(CompletionTime completionTime) {
        this.completionTime = completionTime;
    }

    /**
     * @return the communications
     */
    public List<Communication> getCommunications() {
        return communications;
    }

    /**
     * @param communications the communications to set
     */
    public void setCommunications(List<Communication> communications) {
        this.communications = communications;
    }

    @Override
    public String toString() {
        return "TraceCompletionInformation [completionTime=" + completionTime + ", communications=" + communications
                + "]";
    }

    /**
     * This class represents information about an outbound communication
     * of interest.
     *
     * @author gbrown
     */
    public static class Communication {

        public static final int DEFAULT_EXPIRY_WINDOW_MILLIS = 4000;

        private List<String> ids = new ArrayList<String>();
        private boolean multipleConsumers = false;
        /**
         * Duration in microseconds
         */
        private long baseDuration = 0;
        /**
         * Expire in milliseconds
         */
        private long expire = 0;

        /**
         * @return the ids
         */
        public List<String> getIds() {
            return ids;
        }

        /**
         * @param ids the ids to set
         */
        public void setIds(List<String> ids) {
            this.ids = ids;
        }

        /**
         * @return the multipleConsumers
         */
        public boolean isMultipleConsumers() {
            return multipleConsumers;
        }

        /**
         * @param multipleConsumers the multipleConsumers to set
         */
        public void setMultipleConsumers(boolean multipleConsumers) {
            this.multipleConsumers = multipleConsumers;
        }

        /**
         * @return the base duration in microseconds
         */
        public long getBaseDuration() {
            return baseDuration;
        }

        /**
         * @param baseDuration the base duration in microseconds
         */
        public void setBaseDuration(long baseDuration) {
            this.baseDuration = baseDuration;
        }

        /**
         * @return the expire in microseconds
         */
        public long getExpire() {
            return expire;
        }

        /**
         * @param expire the expire in microseconds
         */
        public void setExpire(long expire) {
            this.expire = expire;
        }

        @Override
        public String toString() {
            return "Communication [ids=" + ids + ", multipleConsumers=" + multipleConsumers + ", baseDuration="
                    + baseDuration + ", expire=" + expire + "]";
        }

    }
}
