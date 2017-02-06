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
package org.hawkular.apm.tests.agent.opentracing.client.http;

import static io.undertow.Handlers.path;

import org.hawkular.apm.agent.opentracing.OpenTracingManager;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.junit.Before;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * @author gbrown
 */
public abstract class AbstractBaseHttpITest extends ClientTestBase {

    private static final String HELLO_WORLD = "Hello World";

    private Undertow server = null;

    @Override
    public void init() {
        server = Undertow.builder()
                .addHttpListener(8180, "localhost")
                .setHandler(path().addPrefixPath("sayHello", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (!exchange.getRequestHeaders().contains("test-fault")) {
                            if (!exchange.getRequestHeaders().contains("test-no-data")) {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                exchange.getResponseSender().send(HELLO_WORLD);
                            }
                        } else {
                            exchange.setResponseCode(401);
                        }
                    }
                })).build();

        server.start();

        super.init();
    }

    @Override
    public void close() {
        server.stop();

        super.close();
    }

    @Before
    public void resetState() {
        // Reset the state before each test, to avoid side effects from one failing test
        // affecting others
        OpenTracingManager.reset();
    }

}
