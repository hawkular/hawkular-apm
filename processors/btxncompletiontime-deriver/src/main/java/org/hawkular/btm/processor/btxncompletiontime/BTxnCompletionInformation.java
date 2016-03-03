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
package org.hawkular.btm.processor.btxncompletiontime;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.events.CompletionTime;

/**
 * This class represents the intermediate information required to calculate
 * the completion time for a business transaction instance.
 *
 * @author gbrown
 */
public class BTxnCompletionInformation {

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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BTxnCompletionInformation [completionTime=" + completionTime + ", communications=" + communications
                + "]";
    }

    /**
     * This class represents information about an outbound communication
     * of interest.
     *
     * @author gbrown
     */
    public static class Communication {

        /**  */
        public static final int DEFAULT_EXPIRY_WINDOW = 60000;

        private List<String> ids = new ArrayList<String>();
        private boolean multipleConsumers = false;
        private long baseDuration = 0;
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
         * @return the baseDuration
         */
        public long getBaseDuration() {
            return baseDuration;
        }

        /**
         * @param baseDuration the baseDuration to set
         */
        public void setBaseDuration(long baseDuration) {
            this.baseDuration = baseDuration;
        }

        /**
         * @return the expire
         */
        public long getExpire() {
            return expire;
        }

        /**
         * @param expire the expire to set
         */
        public void setExpire(long expire) {
            this.expire = expire;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Communication [ids=" + ids + ", multipleConsumers=" + multipleConsumers + ", baseDuration="
                    + baseDuration + ", expire=" + expire + "]";
        }

    }
}
