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
package org.hawkular.apm.server.kafka;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.services.Publisher;
import org.hawkular.apm.api.services.PublisherMetricHandler;

/**
 * @author gbrown
 *
 * @param <S> Source event type
 * @param <T> Target event type
 */
public abstract class AbstractRetryConsumerKafka<S, T> extends AbstractConsumerKafka<S, T> {

    private static final Logger log = Logger.getLogger(AbstractRetryConsumerKafka.class.getName());

    private List<S> retryItems;

    /**
     * This constructor initialises the abstract retry consumer with the
     * topic and group id.
     *
     * @param topic The topic
     * @param groupId The group id
     */
    public AbstractRetryConsumerKafka(String topic, String groupId) {
        super(topic, groupId);

        this.setRetryPublisher(new Publisher<S>() {

            @Override
            public int getInitialRetryCount() {
                return 0;
            }

            @Override
            public void publish(String tenantId, List<S> items) throws Exception {
            }

            @Override
            public void publish(String tenantId, List<S> items, int retryCount, long delay)
                    throws Exception {
            }

            @Override
            public void retry(String tenantId, List<S> items, String subscriber,
                    int retryCount, long delay)
                    throws Exception {
                retryItems = items;
            }

            @Override
            public void setMetricHandler(PublisherMetricHandler<S> handler) {
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void process(String tenantId, List<S> items, int retryCount) throws Exception {
        // Check if retry items exist
        if (retryItems != null && !retryItems.isEmpty()) {
            long curTime = System.currentTimeMillis();

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Retrying items: " + items);
            }

            // Filter out any expired items
            for (int i = items.size() - 1; i >= 0; i--) {
                if (isExpired(items.get(i), curTime)) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Retrying expiring item: " + items.get(i));
                    }

                    items.remove(i);
                }
            }
            if (items.isEmpty()) {
                items = retryItems;
            } else {
                items.addAll(retryItems);
            }
            retryItems = null;
        }
        super.process(tenantId, items, retryCount);
    }

    /**
     * This method determines whether the supplied item has expired and should not
     * be retried.
     *
     * @param item The item
     * @param currentTime The current time in milliseconds
     * @return Whether the item has expired
     */
    protected abstract boolean isExpired(S item, long currentTime);

}
