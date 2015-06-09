/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.tests.standalone;

import static io.undertow.Handlers.path;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * This class represents a test (standalone) application that will be instrumented.
 *
 * @author gbrown
 */
public class StandaloneServerMain {

    /**  */
    private static final int SHUTDOWN_TIMER = 30000;

    private static final Logger log = Logger.getLogger(StandaloneServerMain.class.getName());

    private Undertow server = null;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
    };

    private List<BusinessTransaction> businessTransactions = new ArrayList<BusinessTransaction>();

    /**
     * Main for the test app.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        log.info("************ TEST SERVER STARTED");

        StandaloneServerMain main = new StandaloneServerMain();

        main.run();
    }

    public void run() {
        // Create shutdown thread, just in case hangs
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        wait(SHUTDOWN_TIMER);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                log.severe("************** ABORTING TEST SERVER");
                System.exit(1);
            }
        });
        t.setDaemon(true);
        t.start();

        TopLevelService main = new TopLevelService();

        server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path().addPrefixPath("testOp", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        String mesg = exchange.getQueryParameters().get("mesg").getFirst();
                        String num = exchange.getQueryParameters().get("num").getFirst();

                        log.info("Test op called with mesg=" + mesg + " and num=" + num);

                        String resp = main.testOp(mesg, Integer.parseInt(num));

                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send(resp);
                    }
                }).addPrefixPath("shutdown", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        log.info("Shutdown called");

                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("ok");
                        shutdown();
                    }
                }).addPrefixPath("hawkular/btm/transactions", new HttpHandler() {
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
                            byte[] b = new byte[is.available()];
                            is.read(b);
                            is.close();

                            List<BusinessTransaction> btxns = mapper.readValue(new String(b), BUSINESS_TXN_LIST);

                            businessTransactions.addAll(btxns);

                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("");
                        } else if (exchange.getRequestMethod() == Methods.GET) {
                            String btxns = mapper.writeValueAsString(businessTransactions);
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send(btxns);
                        }
                    }
                })).build();

        server.start();
    }

    public void shutdown() {
        log.info("************ TEST SERVER EXITING");
        server.stop();

        System.exit(0);
    }
}
