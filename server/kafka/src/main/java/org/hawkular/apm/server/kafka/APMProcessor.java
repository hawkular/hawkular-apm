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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * This class provides a processing server using Kafka to receive and send events.
 *
 * @author gbrown
 */
public class APMProcessor {

    public static void main(String[] args) {
        APMProcessor server = new APMProcessor(args);

        server.run();
    }

    /**
     * This constructor initialises the server with the supplied
     * arguments.
     *
     * @param args The arguments
     */
    public APMProcessor(String... args) {
    }

    public void run() {

        // Initialise logging
        Logger logger = Logger.getLogger("org.hawkular.apm");
        FileHandler fh;

        try {
            Level logLevel = Level.parse(PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_LOG_LEVEL, "INFO"));

            // This block configure the logger with handler and formatter
            fh = new FileHandler("apmprocessor.log");
            fh.setLevel(logLevel);
            logger.setLevel(logLevel);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start each of the Kafka processors in their own thread
        List<KafkaProcessor> processors = ServiceResolver.getServices(KafkaProcessor.class);
        for (KafkaProcessor processor : processors) {
            new Thread(processor).start();
        }
    }
}
