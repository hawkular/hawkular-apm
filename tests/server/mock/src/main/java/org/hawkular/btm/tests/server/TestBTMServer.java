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
package org.hawkular.btm.tests.server;

import static io.undertow.Handlers.path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.services.ConfigurationLoader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * This class represents a test business transaction service.
 *
 * @author gbrown
 */
public class TestBTMServer {

    /**  */
    private static final String HAWKULAR_BTM_TEST_SERVER_HOST = "hawkular-btm.test.server.host";

    /**  */
    private static final String HAWKULAR_BTM_TEST_SERVER_PORT = "hawkular-btm.test.server.port";

    /**  */
    private static final String HAWKULAR_BTM_TEST_SERVER_SHUTDOWN = "hawkular-btm.test.server.shutdown";

    /**  */
    private static final int DEFAULT_SHUTDOWN_TIMER = 30000;

    private static final Logger log = Logger.getLogger(TestBTMServer.class.getName());

    private Undertow server = null;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
            };

    private List<BusinessTransaction> businessTransactions = new ArrayList<BusinessTransaction>();

    private int port = 8080;
    private String host = "localhost";
    private int shutdown = DEFAULT_SHUTDOWN_TIMER;

    private CollectorConfiguration testConfig;

    {
        if (System.getProperties().containsKey(HAWKULAR_BTM_TEST_SERVER_HOST)) {
            host = System.getProperty(HAWKULAR_BTM_TEST_SERVER_HOST);
        }
        if (System.getProperties().containsKey(HAWKULAR_BTM_TEST_SERVER_PORT)) {
            port = Integer.parseInt(System.getProperty(HAWKULAR_BTM_TEST_SERVER_PORT));
        }
        if (System.getProperties().containsKey(HAWKULAR_BTM_TEST_SERVER_SHUTDOWN)) {
            shutdown = Integer.parseInt(System.getProperty(HAWKULAR_BTM_TEST_SERVER_SHUTDOWN));
        }
    }

    /**
     * Main for the test app.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        TestBTMServer main = new TestBTMServer();
        main.run();
    }

    /**
     * @return the businessTransactions
     */
    public List<BusinessTransaction> getBusinessTransactions() {
        return businessTransactions;
    }

    /**
     * @param businessTransactions the businessTransactions to set
     */
    public void setBusinessTransactions(List<BusinessTransaction> businessTransactions) {
        this.businessTransactions = businessTransactions;
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
     * This method sets the test configuration.
     *
     * @param testConfig The test collector configuration
     */
    public void setTestConfig(CollectorConfiguration testConfig) {
        this.testConfig = testConfig;
    }

    public void run() {
        log.info("************** STARTED TEST BTXN SERVICE: host=" + host + " port=" + port + " shutdownTimer="
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

                    log.severe("************** ABORTING TEST BTXN SERVICE");
                    System.exit(1);
                }
            });
            t.setDaemon(true);
            t.start();
        }

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(path().addPrefixPath("hawkular/btm/shutdown", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        log.info("Shutdown called");

                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("ok");
                        shutdown();
                    }
                }).addPrefixPath("hawkular/btm/fragments", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }

                        log.info("Transactions request received: " + exchange);

                        if (exchange.getRequestMethod() == Methods.POST) {
                            exchange.startBlocking();

                            java.io.InputStream is = exchange.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                            StringBuilder builder = new StringBuilder();
                            String str = null;

                            while ((str = reader.readLine()) != null) {
                                builder.append(str);
                            }

                            is.close();

                            List<BusinessTransaction> btxns = mapper.readValue(builder.toString(), BUSINESS_TXN_LIST);

                            synchronized (businessTransactions) {
                                businessTransactions.addAll(btxns);
                            }

                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("");
                        } else if (exchange.getRequestMethod() == Methods.GET) {
                            // TODO: Currently returns all - support proper query
                            synchronized (businessTransactions) {
                                String btxns = mapper.writeValueAsString(businessTransactions);
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                exchange.getResponseSender().send(btxns);
                            }
                        }
                    }
                }).addPrefixPath("hawkular/btm/config/collector", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.isInIoThread()) {
                            exchange.dispatch(this);
                            return;
                        }

                        log.info("Config request received: " + exchange);

                        if (exchange.getRequestMethod() == Methods.GET) {
                            CollectorConfiguration config=ConfigurationLoader.getConfiguration();

                            if (testConfig != null) {
                                config.merge(testConfig, true);
                            }

                            String cc = mapper.writeValueAsString(config);
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send(cc);
                        }
                    }
                })).build();

        server.start();
    }

    public void shutdown() {
        log.info("************ TEST BTXN SERVICE EXITING");
        server.stop();
    }
}
