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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.hawkular.apm.api.services.Publisher;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.server.api.task.ProcessingUnit;
import org.hawkular.apm.server.api.task.Processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 *
 * @param <S> Source event type
 * @param <T> Target event type
 */
public class AbstractConsumerKafka<S, T> implements KafkaProcessor {

    private static final Logger log = Logger.getLogger(AbstractConsumerKafka.class.getName());

    /** Default polling interval in milliseconds */
    private static final int DEFAULT_POLLING_INTERVAL = 100;

    private static ObjectMapper mapper = new ObjectMapper();

    private KafkaConsumer<String, String> consumer;

    private TypeReference<S> typeReference;

    private Publisher<S> retryPublisher;

    private Processor<S, T> processor;

    private Publisher<T> publisher;

    private long pollingInterval = DEFAULT_POLLING_INTERVAL;

    /**
     * This constructor initialises the topic.
     *
     * @param topic The topic
     * @param groupId The consumer group id
     */
    public AbstractConsumerKafka(String topic, String groupId) {
        init(topic, groupId);
    }

    /**
     * This method initialises the consumer.
     *
     * @param topic The topic
     * @param groupId The consumer group id
     */
    protected void init(String topic, String groupId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_KAFKA_SERVERS,
                "localhost:9092"));
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms",
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_KAFKA_CONSUMER_AUTO_COMMIT_INTERVAL, "1000"));
        props.put("session.timeout.ms",
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_KAFKA_CONSUMER_SESSION_TIMEOUT, "30000"));
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        String maxPollRecords = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_KAFKA_MAX_POLL_RECORDS);
        if (maxPollRecords != null) {
            props.put("max.poll.records", maxPollRecords);
        }

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));

        pollingInterval = PropertyUtil.getPropertyAsInteger(PropertyUtil.HAWKULAR_APM_KAFKA_POLLING_INTERVAL,
                DEFAULT_POLLING_INTERVAL);
    }

    /**
     * @return the typeReference
     */
    public TypeReference<S> getTypeReference() {
        return typeReference;
    }

    /**
     * @param typeReference the typeReference to set
     */
    public void setTypeReference(TypeReference<S> typeReference) {
        this.typeReference = typeReference;
    }

    /**
     * @return the processor
     */
    public Processor<S, T> getProcessor() {
        return processor;
    }

    /**
     * @param processor the processor to set
     */
    public void setProcessor(Processor<S, T> processor) {
        this.processor = processor;
    }

    /**
     * @return the publisher
     */
    public Publisher<T> getPublisher() {
        return publisher;
    }

    /**
     * @param publisher the publisher to set
     */
    public void setPublisher(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    /**
     * @return the retryPublisher
     */
    public Publisher<S> getRetryPublisher() {
        return retryPublisher;
    }

    /**
     * @param retryPublisher the retryPublisher to set
     */
    public void setRetryPublisher(Publisher<S> retryPublisher) {
        this.retryPublisher = retryPublisher;
    }

    /**
     * This method returns the polling interval for the processor.
     *
     * @return The polling interval
     */
    protected long getPollingInterval() {
        return pollingInterval;
    }

    @Override
    public void run() {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(getPollingInterval());

            if (!records.isEmpty()) {
                List<S> items = new ArrayList<S>();
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        items.add(mapper.readValue(record.value(), typeReference));
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "Failed to deserialise json", e);
                    }
                }

                try {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest(getClass().getSimpleName()+": received " + items.size() +
                                " records after polling " + getPollingInterval() + "ms");
                    }
                    process(null, items, 1);
                } catch (Throwable e) {
                    log.log(Level.SEVERE, "Failed to process records", e);
                }
            }
        }
    }

    /**
     * This method processes the received list of items.
     *
     * @param tenantId The optional tenant id
     * @param items The items
     * @param retryCount The remaining retry count
     * @throws Failed to process items
     */
    protected void process(String tenantId, List<S> items, int retryCount) throws Exception {
        ProcessingUnit<S, T> pu = new ProcessingUnit<S, T>();

        pu.setProcessor(getProcessor());
        pu.setRetryCount(retryCount);

        pu.setResultHandler(
                (tid, events) -> getPublisher().publish(tid, events, getPublisher().getInitialRetryCount(),
                        getProcessor().getDeliveryDelay(events)));

        pu.setRetryHandler(
                (tid, events) -> getRetryPublisher().retry(tid, events, pu.getRetrySubscriber(),
                        pu.getRetryCount() - 1, getProcessor().getRetryDelay(events, pu.getRetryCount() - 1)));

        pu.handle(tenantId, items);
    }

}
