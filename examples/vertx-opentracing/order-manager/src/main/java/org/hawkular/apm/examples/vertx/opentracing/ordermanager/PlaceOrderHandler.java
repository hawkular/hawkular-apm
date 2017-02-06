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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.hawkular.apm.client.opentracing.APMTracer;
import org.hawkular.apm.examples.vertx.opentracing.common.HttpHeadersExtractAdapter;
import org.hawkular.apm.examples.vertx.opentracing.common.VertxMessageInjectAdapter;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @author gbrown
 * @author Juraci Paixão Kröhling
 */
class PlaceOrderHandler extends BaseHandler implements Handler<RoutingContext> {
    private static final Logger logger = Logger.getLogger(ListOrdersHandler.class.getName());
    private Map<String, JsonObject> orders = new HashMap<>();
    private Tracer tracer = new APMTracer();

    @Override
    public void handle(RoutingContext routingContext) {
        logger.finest("Handling request on PlaceOrderHandler");
        routingContext.request().bodyHandler(buf -> {
            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP, new HttpHeadersExtractAdapter(routingContext.request().headers()));

            Span ordersConsumerSpan = tracer.buildSpan("POST")
                    .asChildOf(spanCtx)
                    .withTag("http.url", "/orders")
                    .withTag("transaction", "Place Order")
                    .start();

            JsonObject order = buf.toJsonObject();
            HttpServerResponse response = routingContext.response();

            if (orders.containsKey(order.getString("id"))) {
                logger.warning("Order ID must not be defined");
                sendError(400, "Order id must not be defined", response, ordersConsumerSpan);
            } else {
                logger.finest("Checking Account");
                checkAccount(routingContext, order, response, ordersConsumerSpan);
                logger.finest("Checked Account");
            }
        });

    }

    private void checkAccount(RoutingContext routingContext, JsonObject order, HttpServerResponse response, Span parentSpan) {
        Span getAccountSpan = tracer.buildSpan("GetAccount")
                .asChildOf(parentSpan)
                .start();
        tracer.inject(getAccountSpan.context(), Format.Builtin.TEXT_MAP, new VertxMessageInjectAdapter(order));

        logger.finest("Sending message to AccountManager.getAccount");
        routingContext.vertx().eventBus().send("AccountManager.getAccount", order, acctresp -> {
            logger.finest("Setting the span as finished");
            getAccountSpan.finish();

            if (acctresp.succeeded()) {
                logger.finest("Checking stock");
                checkStock(routingContext, order, response, parentSpan);
            } else {
                logger.warning("Account check failed");
                sendError(500, acctresp.cause().getMessage(), response, parentSpan);
            }
        });
        logger.info("Sent message to AccountManager.getAccount");
    }

    private void checkStock(RoutingContext routingContext, JsonObject order, HttpServerResponse response, Span parentSpan) {
        Span getItemSpan = tracer.buildSpan("GetItem").asChildOf(parentSpan).start();
        tracer.inject(getItemSpan.context(), Format.Builtin.TEXT_MAP, new VertxMessageInjectAdapter(order));

        // Check stock
        routingContext.vertx().eventBus().send("InventoryManager.getItem", order, invresp -> {
            logger.fine("Getting inventory item");

            getItemSpan.finish();
            if (invresp.succeeded()) {
                logger.finest("Inventory response succeeded");
                JsonObject item = (JsonObject) invresp.result().body();
                if (order.getInteger("quantity", 1) <= item.getInteger("quantity", 1)) {
                    logger.fine("Will put order");

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

                    orderConfirmed(routingContext, order, parentSpan);
                } else {
                    logger.info("Out of stock");
                    sendError(500, "Out of stock", response, parentSpan);
                }
            } else {
                logger.warning("Application failed");
                sendError(500, invresp.cause().getMessage(), response, parentSpan);
            }
        });
    }

    private void orderConfirmed(RoutingContext routingContext, JsonObject order, Span parentSpan) {
        logger.fine("Order confirmed");
        try (Span orderConfirmedSpan = tracer.buildSpan("OrderConfirmed").addReference(io.opentracing.References.FOLLOWS_FROM, parentSpan.context()).start()) {
            tracer.inject(orderConfirmedSpan.context(), Format.Builtin.TEXT_MAP, new VertxMessageInjectAdapter(order));

            // Publish confirmed order
            routingContext.vertx().eventBus().publish("Orders.confirmed", order);
        }
    }
}
