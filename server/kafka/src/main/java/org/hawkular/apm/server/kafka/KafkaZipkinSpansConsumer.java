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

package org.hawkular.apm.server.kafka;

import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.server.api.cdi.Eager;
import org.hawkular.apm.server.api.services.SpanPublisher;
import org.hawkular.apm.server.api.utils.zipkin.ZipkinSpanConvertor;
import org.jboss.logging.Logger;

import zipkin.collector.kafka.KafkaCollector;
import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.AsyncSpanStore;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageAdapters;
import zipkin.storage.StorageComponent;


/**
 * @author Pavol Loffay
 */
@Eager
@ApplicationScoped
public class KafkaZipkinSpansConsumer {

    private static final Logger log = Logger.getLogger(KafkaZipkinSpansConsumer.class);

    @Inject
    private SpanPublisher spanPublisher;


    @PostConstruct
    public void initializeZipkinKafkaCollector() {
        /**
         * If the env variable is not set continue without kafka collector.
         */
        if (PropertyUtil.getKafkaZookeeper() == null) {
            return;
        }

        if (spanPublisher == null) {
            throw new IllegalStateException("Span publisher is null!");
        }

        log.infof("Initializing Zipkin kafka collector");

        KafkaCollector kafkaCollector = KafkaCollector.builder()
                .zookeeper(PropertyUtil.getKafkaZookeeper())
                .storage(new APMZipkinSpanStorage(spanPublisher))
                .build();

        kafkaCollector.start();
    }

    private static class APMZipkinSpanStorage implements StorageComponent {

        private final AsyncSpanConsumer asyncSpanConsumer;
        private final Executor callingThread = command -> command.run();

        public APMZipkinSpanStorage(SpanPublisher spanPublisher) {
            asyncSpanConsumer = StorageAdapters.blockingToAsync(spans -> {
                try {
                    spanPublisher.publish(null, ZipkinSpanConvertor.spans(spans));
                } catch (Exception e) {
                    log.errorf("Could not publish spans, reason: %s", e.getMessage());
                }
            }, callingThread);
        }

        @Override
        public SpanStore spanStore() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncSpanStore asyncSpanStore() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AsyncSpanConsumer asyncSpanConsumer() {
            return asyncSpanConsumer;
        }

        @Override
        public CheckResult check() {
            return CheckResult.OK;
        }

        @Override
        public void close() {
        }
    }
}
