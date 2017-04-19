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
package org.hawkular.apm.examples.vertx.opentracing.common;

import java.util.logging.Logger;

import org.hawkular.apm.client.opentracing.APMTracer;

import com.uber.jaeger.Tracer;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UDPSender;

/**
 * @author Juraci Paixão Kröhling
 */
public class TracerResolver {
    private static final Logger logger = Logger.getLogger(TracerResolver.class.getName());
    private static final String JAEGER_SERVER_URL = System.getenv("JAEGER_SERVER_URL");
    private static final boolean USE_JAEGER = Boolean.parseBoolean(System.getenv("USE_JAEGER"));

    public static io.opentracing.Tracer get(String serviceName) {
        if (!USE_JAEGER) {
            logger.info("Using Hawkular APM Tracer");
            return new APMTracer();
        }

        logger.info(String.format("Using Jaeger Tracer at '%s'", JAEGER_SERVER_URL));
        Sender sender = new UDPSender(JAEGER_SERVER_URL, 0, 0);
        Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
        Reporter reporter = new RemoteReporter(sender,100,50, metrics);
        return new Tracer.Builder(serviceName, reporter, new ProbabilisticSampler(1.0)).build();
    }
}
