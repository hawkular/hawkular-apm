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

package org.hawkular.apm.tests.common;

import static io.undertow.Handlers.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ConfigurationLoader;
import org.hawkular.apm.server.api.model.zipkin.Span;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * This class represents a test trace service.
 *
 * @author gbrown
 */
public class ApmMockServer {

    private static final String HAWKULAR_APM_TEST_SERVER_HOST = "hawkular-apm.test.server.host";
    private static final String HAWKULAR_APM_TEST_SERVER_PORT = "hawkular-apm.test.server.port";
    private static final String HAWKULAR_APM_TEST_SERVER_SHUTDOWN = "hawkular-apm.test.server.shutdown";
    private static final int DEFAULT_SHUTDOWN_TIMER = 30000;

    private static final Logger log = Logger.getLogger(ApmMockServer.class.getName());

    private Undertow server = null;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<Trace>> TRACE_LIST =
            new TypeReference<java.util.List<Trace>>() {
            };
    private static final TypeReference<java.util.List<Span>> SPAN_LIST =
            new TypeReference<java.util.List<Span>>() {
            };

    private List<Trace> traces = new ArrayList<>();
    private List<Span> zipkinSpans = new ArrayList<>();

    private int port = 8080;
    private String host = "localhost";
    private int shutdown = DEFAULT_SHUTDOWN_TIMER;

    private CollectorConfiguration testConfig;

    {
        if (System.getProperties().containsKey(HAWKULAR_APM_TEST_SERVER_HOST)) {
            host = System.getProperty(HAWKULAR_APM_TEST_SERVER_HOST);
        }
        if (System.getProperties().containsKey(HAWKULAR_APM_TEST_SERVER_PORT)) {
            port = Integer.parseInt(System.getProperty(HAWKULAR_APM_TEST_SERVER_PORT));
        }
        if (System.getProperties().containsKey(HAWKULAR_APM_TEST_SERVER_SHUTDOWN)) {
            shutdown = Integer.parseInt(System.getProperty(HAWKULAR_APM_TEST_SERVER_SHUTDOWN));
        }
    }

    /**
     * Main for the test app.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        ApmMockServer main = new ApmMockServer();
        main.run();
    }

    /**
     * @return the traces
     */
    public List<Trace> getTraces() {
        return traces;
    }

    /**
     * @param traces the traces to set
     */
    public void setTraces(List<Trace> traces) {
        this.traces = traces;
    }

    /**
     * @return the zipkin spans
     */
    public List<Span> getSpans() {
        return zipkinSpans;
    }

    /**
     * @param spans the zipkin spans to set
     */
    public void setSpans(List<Span> spans) {
        this.zipkinSpans = spans;
    }

    /**
     * This method sets the shutdown timer. If set to -1, then
     * the timer is disabled. This value must be set before the run method
     * is called.
     *
     * @param timer The shutdown timer (in milliseconds), or -1 to disable
     */
    public void setShutdownTimer(int timer) {
        shutdown = timer;
    }

    /**
     * This method sets the port.
     *
     * @param port The port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * This method sets the host to bind at.
     *
     * @param host The host to bind
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * This method sets the test configuration.
     *
     * @param testConfig The test collector configuration
     */
    public void setTestConfig(CollectorConfiguration testConfig) {
        this.testConfig = testConfig;
    }

    public void run() {
        log.info("************** STARTED TEST MOCK SERVICE: host=" + host + " port=" + port + " shutdownTimer="
                + shutdown);

        if (shutdown != -1) {
            // Create shutdown thread, just in case hangs
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        try {
                            wait(shutdown);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    log.severe("************** ABORTING TEST MOCK SERVICE");
                    System.exit(1);
                }
            });
            t.setDaemon(true);
            t.start();
        }

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(path().addPrefixPath("hawkular/apm/shutdown", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        log.info("Shutdown called");

                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("ok");
                        shutdown();
                    }
                }).addPrefixPath("hawkular/apm/traces/fragments", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }

                        log.info("Transactions request received: " + exchange);

                        if (exchange.getRequestMethod() == Methods.POST) {
                            exchange.startBlocking();

                            String body = getBody(exchange.getInputStream());
                            List<Trace> btxns = mapper.readValue(body, TRACE_LIST);

                            synchronized (traces) {
                                traces.addAll(btxns);
                            }

                            exchange.setResponseCode(204);
                        } else if (exchange.getRequestMethod() == Methods.GET) {
                            // TODO: Currently returns all - support proper query
                            synchronized (traces) {
                                String btxns = mapper.writeValueAsString(traces);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(btxns);
                            }
                        }
                    }
                }).addPrefixPath("hawkular/apm/config/collector", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }

                        log.info("Config request received: " + exchange);

                        if (exchange.getRequestMethod() == Methods.GET) {
                            CollectorConfiguration config= ConfigurationLoader.getConfiguration(null);

                            if (testConfig != null) {
                                config.merge(testConfig, true);
                            }

                            String cc = mapper.writeValueAsString(config);
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send(cc);
                        }
                    }
                }).addPrefixPath("v1/spans", new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }

                        log.info("Zipkin spans request received: " + exchange);

                        if (exchange.getRequestMethod() == Methods.POST) {
                            exchange.startBlocking();

                            String body = getBody(exchange.getInputStream());
                            List<Span> spans = mapper.readValue(body, SPAN_LIST);

                            synchronized (zipkinSpans) {
                                zipkinSpans.addAll(spans);
                            }

                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("");
                        } else if (exchange.getRequestMethod() == Methods.GET) {
                            synchronized (zipkinSpans) {
                                String zipkinSpansStr = mapper.writeValueAsString(zipkinSpans);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(zipkinSpansStr);
                            }
                        }
                    }
                })).build();
        server.start();
    }

    public void shutdown() {
        if (server!= null) {
            log.info("************ TEST MOCK SERVICE EXITING");
            server.stop();
        }
    }

    private String getBody(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder builder = new StringBuilder();

        String str;
        while ((str = reader.readLine()) != null) {
            builder.append(str);
        }

        inputStream.close();

        return builder.toString();
    }
}
