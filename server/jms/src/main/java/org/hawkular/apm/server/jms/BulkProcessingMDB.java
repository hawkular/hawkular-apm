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
package org.hawkular.apm.server.jms;

import java.util.List;

/**
 * This MDB represents a bulk processor of the list of supplied items.
 *
 * @author gbrown
 */
public abstract class BulkProcessingMDB<T> extends RetryCapableMDB<T> {

    /**
    * {@inheritDoc}
    */
    @Override
    protected void process(String tenantId, List<T> items, int retryCount) throws Exception {
        try {
            bulkProcess(tenantId, items, retryCount);
        } catch (Exception e) {
            if (retryCount > 0 && getRetryPublisher() != null) {
                // HWKAPM-482 - need to consider best way to determine retry interval/delay
                getRetryPublisher().publish(tenantId, items, retryCount - 1, 1000);
            } else {
                // Rethrow the exception as no more retry attempts remain
                throw e;
            }
        }
    }

    /**
     * This method is invoked to bulk process the supplied list of items. If a failure occurs
     * and the retry count is greater than 0, then the items will be resubmitted to the retry
     * publisher. Otherwise an exception will be thrown.
     *
     * @param tenantId The tenant id
     * @param items The list of items to be bulk processed
     * @param retryCount The retry count
     * @throws Exception Failed to bulk process the items, and no retries remain
     */
    protected abstract void bulkProcess(String tenantId, List<T> items, int retryCount) throws Exception;

}
