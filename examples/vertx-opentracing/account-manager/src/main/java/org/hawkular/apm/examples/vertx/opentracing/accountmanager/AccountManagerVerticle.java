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
package org.hawkular.apm.examples.vertx.opentracing.accountmanager;

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
public class AccountManagerVerticle extends AbstractVerticle {
    private Map<String, JsonObject> accounts = new HashMap<>();
    private Tracer tracer = new APMTracer();
    private static final Logger logger = Logger.getLogger(AccountManagerVerticle.class.getName());

    public static void main(String[] args) {
        logger.info("Starting [AccountManagerVerticle] from its `main` method. Consider starting it from vertx.");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new AccountManagerVerticle());
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("Starting Account Manager");

        accounts.put("fred", new JsonObject().put("name", "fred"));
        accounts.put("joe", new JsonObject().put("name", "joe"));
        accounts.put("jane", new JsonObject().put("name", "jane"));
        accounts.put("steve", new JsonObject().put("name", "steve"));
        accounts.put("brian", new JsonObject().put("name", "brian"));

        setupConsumers();
        logger.info("Ready");
    }

    private void setupConsumers() {
        logger.info("Setting up consumers");
        getVertx().eventBus().consumer("joined").handler(message -> logger.info(String.format("Acknowledging that %s just joined", message.body())));

        MessageConsumer<JsonObject> consumer = getVertx().eventBus().consumer("AccountManager.getAccount");
        consumer.handler(message -> {
            logger.finest("Handling message");
            JsonObject req = message.body();
            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP, new VertxMessageExtractAdapter(req));

            try (Span getAccountSpan = tracer.buildSpan("GetAccount")
                    .asChildOf(spanCtx)
                    .start()) {

                if (!req.containsKey("accountId")) {
                    logger.warning("Account ID is missing");
                    sendError(1, "Account id missing", message, getAccountSpan);
                } else {
                    try (Span ignored = tracer.buildSpan("RetrieveAccount").asChildOf(getAccountSpan)
                            .withTag("database.url", "AccountsDB")
                            .withTag("database.statement", "SELECT account FROM Accounts WHERE id = ?")
                            .start()) {
                        JsonObject acct = accounts.get(req.getString("accountId"));
                        if (acct == null) {
                            logger.warning("Account not found");
                            sendError(2, "Not account found", message, getAccountSpan);
                        } else {
                            logger.fine("Account found, replying");
                            message.reply(acct);
                        }
                    }
                }
            }
        }).completionHandler(result -> {
            if (result.succeeded()) {
                getVertx().eventBus().send("joined", "AccountManager.getAccount");
                logger.info("Registration has completed.");
            } else {
                logger.warning("Could not register: " + result.cause().getMessage());
            }
        });
    }

    private void sendError(int statusCode, String text, Message<JsonObject> message, Span span) {
        logger.warning("Failing message");
        message.fail(statusCode, text);
        if (span != null) {
            span.setTag("fault", text == null ? Integer.toString(statusCode) : text);
        }
    }
}
