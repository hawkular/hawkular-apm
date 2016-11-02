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
import java.util.UUID;

import org.hawkular.apm.client.opentracing.APMTracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * @author gbrown
 */
public class OrderManager {

    private Map<String, JsonObject> orders = new HashMap<>();

    private Vertx vertx;
    private EventBus eb;

    private Tracer tracer = new APMTracer();

    public static void main(String[] args) {
        OrderManager om = new OrderManager();
        om.run();
    }

    public void run() {
        vertx = Vertx.vertx();
        eb = vertx.eventBus();

        // Create other 'services'
        AccountManager.create(eb, tracer);
        OrderLog.create(eb, tracer);
        InventoryManager.create(eb, tracer);

        // Register the REST API for the Order Manager
        Router router = Router.router(vertx);

        router.route(HttpMethod.POST, "/orders").handler(this::handlePlaceOrder);

        router.route(HttpMethod.GET, "/orders").handler(this::handleListOrders);

        vertx.createHttpServer().requestHandler(router::accept).listen(8180);
    }

    private void handlePlaceOrder(RoutingContext routingContext) {
        routingContext.request().bodyHandler(buf -> {
            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new HttpHeadersExtractAdapter(routingContext.request().headers()));

            Span ordersConsumerSpan = tracer.buildSpan("POST")
                    .asChildOf(spanCtx)
                    .withTag("http.url", "/orders")
                    .withTag("service", "OrderManager")
                    .withTag("transaction", "Place Order")
                    .start();

            JsonObject order = buf.toJsonObject();
            HttpServerResponse response = routingContext.response();

            if (orders.containsKey(order.getValue("id"))) {
                sendError(400, "Order id must not be defined", response, ordersConsumerSpan);
            } else {
                checkAccount(order, response, ordersConsumerSpan);
            }
        });
    }

    private void checkAccount(JsonObject order, HttpServerResponse response, Span parentSpan) {
        Span getAccountSpan = tracer.buildSpan("GetAccount")
                .asChildOf(parentSpan)
                .start();
        tracer.inject(getAccountSpan.context(), Format.Builtin.TEXT_MAP,
                new VertxMessageInjectAdapter(order));

        eb.send("AccountManager.getAccount", order, acctresp -> {

            getAccountSpan.finish();

            if (acctresp.succeeded()) {
                checkStock(order, response, parentSpan);
            } else {
                sendError(500, acctresp.cause().getMessage(), response, parentSpan);
            }
        });
    }

    protected void checkStock(JsonObject order, HttpServerResponse response, Span parentSpan) {
        Span getItemSpan = tracer.buildSpan("GetItem")
                .asChildOf(parentSpan)
                .start();
        tracer.inject(getItemSpan.context(), Format.Builtin.TEXT_MAP,
                new VertxMessageInjectAdapter(order));

        // Check stock
        eb.send("InventoryManager.getItem", order, invresp -> {

            getItemSpan.finish();

            if (invresp.succeeded()) {
                JsonObject item = (JsonObject) invresp.result().body();
                if (order.getInteger("quantity", 1) <= item.getInteger("quantity", 1)) {

                    // Remove internal headers - necessary because we have had
                    // to piggyback the APM state as a top level field in the
                    // order JSON document - which now needs to be removed before
                    // returning it to the end user.
                    VertxMessageInjectAdapter.cleanup(order);

                    // Assign id
                    order.put("id", UUID.randomUUID().toString());
                    orders.put(order.getString("id"), order);
                    response.putHeader("content-type", "application/json")
                            .setStatusCode(202).end(order.encodePrettily());

                    parentSpan.setTag("orderId", order.getString("id"));
                    parentSpan.setTag("itemId", order.getString("itemId"));
                    parentSpan.setTag("accountId", order.getString("accountId"));

                    parentSpan.finish();

                    orderConfirmed(order, parentSpan);
                } else {
                    sendError(500, "Out of stock", response, parentSpan);
                }
            } else {
                sendError(500, invresp.cause().getMessage(), response, parentSpan);
            }
        });
    }

    protected void orderConfirmed(JsonObject order, Span parentSpan) {
        try (Span orderConfirmedSpan = tracer.buildSpan("OrderConfirmed")
                .addReference(io.opentracing.References.FOLLOWS_FROM,
                        parentSpan.context())
                .start()) {
            tracer.inject(orderConfirmedSpan.context(), Format.Builtin.TEXT_MAP,
                    new VertxMessageInjectAdapter(order));

            // Publish confirmed order
            eb.publish("Orders.confirmed", order);
        }
    }
    private void handleListOrders(RoutingContext routingContext) {
        routingContext.request().bodyHandler(buf -> {
            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new HttpHeadersExtractAdapter(routingContext.request().headers()));

            Span listOrdersSpan = tracer.buildSpan("GET")
                    .asChildOf(spanCtx)
                    .withTag("http.url", "/orders")
                    .withTag("service", "OrderManager")
                    .withTag("transaction", "List My Orders")
                    .start();

            JsonObject acct = buf.toJsonObject();
            HttpServerResponse response = routingContext.response();

            Span getOrdersSpan = tracer.buildSpan("GetOrdersFromLog")
                    .asChildOf(listOrdersSpan)
                    .start();
            tracer.inject(getOrdersSpan.context(), Format.Builtin.TEXT_MAP,
                    new VertxMessageInjectAdapter(acct));

            eb.send("OrderLog.getOrders", acct, logresp -> {

                getOrdersSpan.finish();

                if (logresp.succeeded()) {
                    JsonArray orders = (JsonArray) logresp.result().body();
                    response.putHeader("content-type", "application/json")
                            .setStatusCode(200).end(orders.encodePrettily());

                    listOrdersSpan.finish();
                } else {
                    sendError(500, logresp.cause().getMessage(), response, listOrdersSpan);
                }
            });
        });
    }

    private void sendError(int statusCode, String message, HttpServerResponse response, Span span) {
        response.setStatusCode(statusCode).end(message);
        if (span != null) {
            span.setTag("fault", message == null ? Integer.toString(statusCode) : message);
            span.finish();
        }
    }
}
