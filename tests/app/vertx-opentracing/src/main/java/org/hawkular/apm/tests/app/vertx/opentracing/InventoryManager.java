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
package org.hawkular.apm.tests.app.vertx.opentracing;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

/**
 * @author gbrown
 */
public class InventoryManager {

    private Map<String, JsonObject> items = new HashMap<>();
    private MessageConsumer<JsonObject> getItemConsumer;
    private MessageConsumer<JsonObject> ordersConfirmedConsumer;

    private InventoryManager(EventBus eb, Tracer tracer) {
        initItems();
        initGetItemConsumer(eb, tracer);
        initOrdersConfirmedConsumer(eb, tracer);
    }

    public static InventoryManager create(EventBus eb, Tracer tracer) {
        return new InventoryManager(eb, tracer);
    }

    protected void initItems() {
        items.put("laptop", new JsonObject().put("itemId", "laptop").put("quantity", 5));
        items.put("car", new JsonObject().put("itemId", "car").put("quantity", 8));
        items.put("book", new JsonObject().put("itemId", "book").put("quantity", 9));
        items.put("chair", new JsonObject().put("itemId", "chair").put("quantity", 7));
        items.put("dvd", new JsonObject().put("itemId", "dvd").put("quantity", 6));
    }

    protected void initGetItemConsumer(EventBus eb, Tracer tracer) {
        getItemConsumer = eb.consumer("InventoryManager.getItem");
        getItemConsumer.handler(message -> {
            JsonObject req = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(req));

            try (Span getItemSpan = tracer.buildSpan("GetItem")
                    .asChildOf(spanCtx)
                    .withTag("service", "InventoryManager")
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
        });
    }

    protected void initOrdersConfirmedConsumer(EventBus eb, Tracer tracer) {
        ordersConfirmedConsumer = eb.consumer("Orders.confirmed");
        ordersConfirmedConsumer.handler(message -> {
            JsonObject order = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(order));

            try (Span orderConfirmedSpan = tracer.buildSpan("UpdateQuantity")
                    .asChildOf(spanCtx)
                    .withTag("service", "InventoryManager")
                    .start()) {

                try (Span queryInventorySpan = tracer.buildSpan("WriteInventory")
                        .asChildOf(orderConfirmedSpan)
                        .withTag("database.url", "InventoryDB")
                        .withTag("database.statement", "UPDATE Inventory SET item=?")
                        .start()) {
                    // Update quantity and write to db
                }
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
