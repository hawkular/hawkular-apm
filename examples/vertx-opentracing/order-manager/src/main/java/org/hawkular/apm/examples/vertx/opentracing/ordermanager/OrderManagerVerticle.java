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
package org.hawkular.apm.examples.vertx.opentracing.ordermanager;

import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;

/**
 * @author gbrown
 * @author Juraci Paixão Kröhling
 */
public class OrderManagerVerticle extends AbstractVerticle {
    private static final Logger logger = Logger.getLogger(OrderManagerVerticle.class.getName());

    public static void main(String[] args) {
        logger.info("Starting [OrderManagerVerticle] from its `main` method. Consider starting it from vertx.");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OrderManagerVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("Starting Order Manager");
        getVertx().eventBus().consumer("joined").handler(message -> logger.info(String.format("Acknowledging that %s just joined", message.body())));

        setupHttpServer();
    }

    private void setupHttpServer() {
        int port = config().getInteger("http.port", 8080);
        Router router = Router.router(getVertx());
        router.route(HttpMethod.GET, "/status").handler(event -> event.response().setStatusCode(200).end("OK"));
        router.route(HttpMethod.POST, "/orders").handler(new PlaceOrderHandler());
        router.route(HttpMethod.GET, "/orders").handler(new ListOrdersHandler());
        getVertx()
                .createHttpServer()
                .requestHandler(router::accept)
                .listen(port, result -> logger.info("HTTP Server started at " + port ));
    }
}
