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
package org.hawkular.apm.examples.vertx.opentracing;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * @author gbrown
 */
public class OrderLog {

    private Map<String, JsonArray> orders = new HashMap<>();
    private MessageConsumer<JsonObject> ordersConfirmedConsumer;
    private MessageConsumer<JsonObject> getOrdersConsumer;

    private OrderLog(EventBus eb, Tracer tracer) {
        initOrdersConfirmedConsumer(eb, tracer);
        initGetOrdersConsumer(eb, tracer);
    }

    public static OrderLog create(EventBus eb, Tracer tracer) {
        return new OrderLog(eb, tracer);
    }

    protected void initOrdersConfirmedConsumer(EventBus eb, Tracer tracer) {
        ordersConfirmedConsumer = eb.consumer("Orders.confirmed");
        ordersConfirmedConsumer.handler(message -> {
            JsonObject order = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(order));

            try (Span orderConfirmedSpan = tracer.buildSpan("StoreOrder")
                    .asChildOf(spanCtx)
                    .withTag("service", "OrderLog")
                    .start()) {

                try (Span storeOrderSpan = tracer.buildSpan("WriteOrder")
                        .asChildOf(orderConfirmedSpan)
                        .withTag("database.url", "OrdersDB")
                        .withTag("sql", "UPDATE .....")
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
        });
    }

    protected void initGetOrdersConsumer(EventBus eb, Tracer tracer) {
        getOrdersConsumer = eb.consumer("OrderLog.getOrders");
        getOrdersConsumer.handler(message -> {
            JsonObject order = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(order));

            try (Span getOrdersSpan = tracer.buildSpan("GetOrders")
                    .asChildOf(spanCtx)
                    .withTag("service", "OrderLog")
                    .start()) {

                try (Span retrieveOrdersSpan = tracer.buildSpan("RetrieveOrders")
                        .asChildOf(getOrdersSpan)
                        .withTag("database.url", "OrdersDB")
                        .withTag("sql", "SELECT .....")
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
        });
    }

    private void sendError(int statusCode, String text, Message<JsonObject> message, Span span) {
        message.fail(statusCode, text);
        if (span != null) {
            span.setTag("fault", text == null ? Integer.toString(statusCode) : text);
        }
    }
}
