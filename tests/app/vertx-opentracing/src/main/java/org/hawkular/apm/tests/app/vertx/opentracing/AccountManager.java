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
public class AccountManager {

    private Map<String, JsonObject> accounts = new HashMap<>();
    private MessageConsumer<JsonObject> getAccountConsumer;

    private AccountManager(EventBus eb, Tracer tracer) {
        init(eb, tracer);
    }

    public static AccountManager create(EventBus eb, Tracer tracer) {
        return new AccountManager(eb, tracer);
    }

    protected void init(EventBus eb, Tracer tracer) {
        accounts.put("fred", new JsonObject().put("name", "fred"));
        accounts.put("joe", new JsonObject().put("name", "joe"));
        accounts.put("jane", new JsonObject().put("name", "jane"));
        accounts.put("steve", new JsonObject().put("name", "steve"));
        accounts.put("brian", new JsonObject().put("name", "brian"));

        getAccountConsumer = eb.consumer("AccountManager.getAccount");
        getAccountConsumer.handler(message -> {
            JsonObject req = message.body();

            SpanContext spanCtx = tracer.extract(Format.Builtin.TEXT_MAP,
                    new VertxMessageExtractAdapter(req));

            try (Span getAccountSpan = tracer.buildSpan("GetAccount")
                    .asChildOf(spanCtx)
                    .withTag("service", "AccountManager")
                    .start()) {

                if (!req.containsKey("accountId")) {
                    sendError(1, "Account id missing", message, getAccountSpan);
                } else {
                    try (Span retrieveAccountSpan = tracer.buildSpan("RetrieveAccount")
                            .asChildOf(getAccountSpan)
                            .withTag("database.url", "AccountsDB")
                            .withTag("database.statement", "SELECT account FROM Accounts WHERE id = ?")
                            .start()) {
                        JsonObject acct = accounts.get(req.getString("accountId"));
                        if (acct == null) {
                            sendError(2, "Not account found", message, getAccountSpan);
                        } else {
                            message.reply(acct);
                        }
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
