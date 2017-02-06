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
package org.hawkular.apm.client.kafka;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.hawkular.apm.api.services.Publisher;
import org.hawkular.apm.api.services.PublisherMetricHandler;
import org.hawkular.apm.api.services.ServiceStatus;
import org.hawkular.apm.api.utils.PropertyUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class represents an abstract Kafka producer.
 *
 * @author gbrown
 *
 * @param <T> The event type being published.
 */
public abstract class AbstractPublisherKafka<T> implements Publisher<T>, ServiceStatus {

    private static ObjectMapper mapper = new ObjectMapper();

    private Producer<String, String> producer;

    private String topic;

    private PublisherMetricHandler<T> handler = null;

    /**
     * This constructor initialises the topic.
     *
     * @param topic The topic
     */
    public AbstractPublisherKafka(String topic) {
        this.topic = topic;
        if (isAvailable()) {
            init();
        }
    }

    @Override
    public boolean isAvailable() {
        String uri = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI_PUBLISHER,
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI));
        return uri != null && uri.startsWith(PropertyUtil.KAFKA_PREFIX);
    }

    /**
     * This method initialises the publisher.
     */
    protected void init() {
        Properties props = new Properties();
        props.put("bootstrap.servers", PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI_PUBLISHER,
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI))
                .substring(PropertyUtil.KAFKA_PREFIX.length()));
        props.put("acks", "all");
        props.put("retries", PropertyUtil.getPropertyAsInteger(PropertyUtil.HAWKULAR_APM_KAFKA_PRODUCER_RETRIES, 3));
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        producer = new KafkaProducer<>(props);
    }

    @Override
    public int getInitialRetryCount() {
        return 0;
    }

    @Override
    public void publish(String tenantId, List<T> items) throws Exception {
        publish(tenantId, items, getInitialRetryCount(), 0);
    }

    @Override
    public void publish(String tenantId, List<T> items, int retryCount, long delay) throws Exception {
        // Check if delay is required
        // TODO: May need to check if delay is excessive and schedule message publish in separate thread
        if (delay > 0) {
            try {
                synchronized (this) {
                    wait(delay);
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        long startTime = 0;
        if (handler != null) {
            startTime = System.nanoTime();
        }

        for (int i = 0; i < items.size(); i++) {
            String data = mapper.writeValueAsString(items.get(i));
            // Sending record asynchronously without waiting for response. Failures
            // should be handled by Kafka's own retry mechanism, which has been
            // configured for 3 attempts (at 100ms intervals) by default
            producer.send(new ProducerRecord<String, String>(topic, data));
        }

        if (handler != null) {
            handler.published(tenantId, items, (TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startTime)));
        }
    }

    @Override
    public void retry(String tenantId, List<T> items, String subscriber, int retryCount, long delay) throws Exception {
        throw new UnsupportedOperationException("Retry not supported for this publisher");
    }

    @Override
    public void setMetricHandler(PublisherMetricHandler<T> handler) {
        this.handler = handler;
    }

}
