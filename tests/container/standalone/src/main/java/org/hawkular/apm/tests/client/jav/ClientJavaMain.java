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
package org.hawkular.apm.tests.client.jav;

import static io.undertow.Handlers.path;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * This class represents a test (standalone) Java application that will be instrumented.
 *
 * @author gbrown
 */
public class ClientJavaMain {

    private static final int SHUTDOWN_TIMER = 30000;

    private static final Logger log = Logger.getLogger(ClientJavaMain.class.getName());

    private Undertow server = null;

    private String host = System.getProperty("hawkular-apm.testapp.host");
    private int port = Integer.parseInt(System.getProperty("hawkular-apm.testapp.port"));

    /**
     * Main for the test app.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        ClientJavaMain main = new ClientJavaMain();

        try {
            main.run();
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Failed to run client java main", t);
        }
    }

    public void run() {
        log.info("************ TEST CLIENT JAVA APP STARTED: host=" + host + " port=" + port);

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

                log.severe("************** ABORTING TEST CLIENT JAVA APP");
                System.exit(1);
            }
        });
        t.setDaemon(true);
        t.start();

        TopLevelService main = new TopLevelService();

        server = Undertow.builder()
                .addHttpListener(port, host)
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
                })).build();

        server.start();
    }

    public void shutdown() {
        log.info("************ TEST CLIENT JAVA APP EXITING");
        server.stop();

        System.exit(0);
    }
}
