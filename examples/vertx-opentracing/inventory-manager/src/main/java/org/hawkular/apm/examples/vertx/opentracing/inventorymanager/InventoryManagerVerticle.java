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
package org.hawkular.apm.examples.vertx.opentracing.inventorymanager;

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
import io.vertx.core.json.JsonObject;

/**
 * @author gbrown
 * @author Juraci Paixão Kröhling
 */
public class InventoryManagerVerticle extends AbstractVerticle {
    private static final Logger logger = Logger.getLogger(InventoryManagerVerticle.class.getName());
    private Tracer tracer = new APMTracer();
    private Map<String, JsonObject> items = new HashMap<>();

    public static void main(String[] args) {
        logger.info("Starting [InventoryManagerVerticle] from its `main` method. Consider starting it from vertx.");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new InventoryManagerVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("Starting Inventory Manager");
        items.put("laptop", new JsonObject().put("itemId", "laptop").put("quantity", 5));
        items.put("car", new JsonObject().put("itemId", "car").put("quantity", 8));
        items.put("book", new JsonObject().put("itemId", "book").put("quantity", 9));
        items.put("chair", new JsonObject().put("itemId", "chair").put("quantity", 7));
        items.put("dvd", new JsonObject().put("itemId", "dvd").put("quantity", 6));

        setupConsumers();
    }

    private void setupConsumers() {
        logger.info("Setting up consumers");
        getVertx().eventBus().consumer("joined").handler(message -> logger.info(String.format("Acknowledging that %s just joined", message.body())));

        MessageConsumer<JsonObject> getItemConsumer = getVertx().eventBus().consumer("InventoryManager.getItem");
        MessageConsumer<JsonObject> ordersConfirmedConsumer = getVertx().eventBus().consumer("Orders.confirmed");

        getItemConsumer.handler(message -> {
            JsonObject req = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP, new VertxMessageExtractAdapter(req));

            try (Span getItemSpan = tracer.buildSpan("GetItem")
                    .asChildOf(spanCtx)
                    .start()) {

                if (!req.containsKey("itemId")) {
                    message.fail(1, "Item id missing");
                } else {
                    try (Span queryInventorySpan = tracer.buildSpan("QueryInventory")
                            .asChildOf(getItemSpan)
                            .withTag("database.url", "InventoryDB")
                            .withTag("database.statement", "SELECT item FROM Inventory WHERE id = ?")
                            .start()) {

                        JsonObject acct = items.get(req.getString("itemId"));
                        if (acct == null) {
                            sendError(2, "Item not found", message, getItemSpan);
                        } else {
                            message.reply(acct);
                        }
                    }
                }
            }
        }).completionHandler(result -> {
            if (result.succeeded()) {
                getVertx().eventBus().send("joined", "InventoryManager.getItem");
                logger.info("Registration has completed.");
            } else {
                logger.warning("Could not register: " + result.cause().getMessage());
            }
        });

        ordersConfirmedConsumer.handler(message -> {
            JsonObject order = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(order));

            try (Span orderConfirmedSpan = tracer.buildSpan("UpdateQuantity")
                    .asChildOf(spanCtx)
                    .start()) {

                try (Span queryInventorySpan = tracer.buildSpan("WriteInventory")
                        .asChildOf(orderConfirmedSpan)
                        .withTag("database.url", "InventoryDB")
                        .withTag("database.statement", "UPDATE Inventory SET item=?")
                        .start()) {
                    // Update quantity and write to db
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

    void sendError(int statusCode, String text, Message<JsonObject> message, Span span) {
        message.fail(statusCode, text);
        if (span != null) {
            span.setTag("fault", text == null ? Integer.toString(statusCode) : text);
        }
    }
}
