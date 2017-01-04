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

package org.hawkular.apm.example.swarm.zipkin;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.github.kristofa.brave.Brave;

import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

/**
 * @author Pavol Loffay
 */
public class BraveProducer {

    @Produces
    @Singleton
    public Brave getBrave() {
        String port = System.getenv("TRACING_PORT");
        if (port == null) {
            throw new IllegalStateException("Environmental variable TRACING_PORT is not set!");
        }

        return new Brave.Builder("wildfly-swarm")
                .reporter(AsyncReporter.builder(OkHttpSender.builder()
                        .endpoint("http://tracing-server:" + port + "/api/v1/spans")
                        .build())
                    .build())
                .build();
    }
}

