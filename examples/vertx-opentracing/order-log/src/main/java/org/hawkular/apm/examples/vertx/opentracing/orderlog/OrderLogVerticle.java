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
package org.hawkular.apm.examples.vertx.opentracing.orderlog;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.hawkular.apm.client.opentracing.APMTracer;
import org.hawkular.apm.examples.vertx.opentracing.common.VertxMessageExtractAdapter;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author gbrown
 * @author Juraci Paixão Kröhling
 */
public class OrderLogVerticle extends AbstractVerticle {
    private static final Logger logger = Logger.getLogger(OrderLogVerticle.class.getName());

    private Tracer tracer = new APMTracer();
    private Map<String, JsonArray> orders = new HashMap<>();

    public static void main(String[] args) {
        logger.info("Starting [OrderLogVerticle] from its `main` method. Consider starting it from vertx.");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OrderLogVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("Starting Order Log");
        setupConsumers();
    }

    private void setupConsumers() {
        logger.info("Setting up consumers");
        getVertx().eventBus().consumer("joined").handler(message -> logger.info(String.format("Acknowledging that %s just joined", message.body())));

        MessageConsumer<JsonObject> ordersConfirmedConsumer = getVertx().eventBus().consumer("Orders.confirmed");
        MessageConsumer<JsonObject> getOrdersConsumer = getVertx().eventBus().consumer("OrderLog.getOrders");

        getOrdersConsumer.handler(message -> {
            JsonObject order = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(order));

            try (Span getOrdersSpan = tracer.buildSpan("GetOrders")
                    .asChildOf(spanCtx)
                    .start()) {

                try (Span ignored = tracer.buildSpan("RetrieveOrders")
                        .asChildOf(getOrdersSpan)
                        .withTag("database.url", "OrdersDB")
                        .withTag("database.statement", "SELECT order FROM Orders WHERE accountId = ?")
                        .start()) {
                    String acctId = order.getString("accountId");
                    JsonArray myOrders = orders.get(acctId);
                    if (myOrders == null) {
                        sendError(1, "Account not found", message, getOrdersSpan);
                    } else {
                        message.reply(myOrders);
                    }
                }
            }
        }).completionHandler(result -> {
            if (result.succeeded()) {
                getVertx().eventBus().send("joined", "OrderLog.getOrders");
                logger.info("Registration has completed.");
            } else {
                logger.warning("Could not register: " + result.cause().getMessage());
            }
        });

        ordersConfirmedConsumer.handler(message -> {
            JsonObject order = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(order));

            try (Span orderConfirmedSpan = tracer.buildSpan("StoreOrder")
                    .asChildOf(spanCtx)
                    .start()) {

                try (Span ignored = tracer.buildSpan("WriteOrder")
                        .asChildOf(orderConfirmedSpan)
                        .withTag("database.url", "OrdersDB")
                        .withTag("database.statement", "UPDATE Orders SET order=?")
                        .start()) {

                    String acctId = order.getString("accountId");
                    JsonArray myOrders = orders.get(acctId);
                    if (myOrders == null) {
                        myOrders = new JsonArray();
                        orders.put(acctId, myOrders);
                    }
                    myOrders.add(order);
                }
            }
        }).completionHandler(result -> {
            if (result.succeeded()) {
                getVertx().eventBus().send("joined", "Orders.confirmed");
                logger.info("Registration has completed.");
            } else {
                logger.warning("Could not register: " + result.cause().getMessage());
            }
        });
    }

    private void sendError(int statusCode, String text, Message<JsonObject> message, Span span) {
        message.fail(statusCode, text);
        if (span != null) {
            span.setTag("fault", text == null ? Integer.toString(statusCode) : text);
        }
    }
}
