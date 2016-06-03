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
package org.hawkular.apm.tests.client.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;

/**
 * @author gbrown
 */
public class ClientVertxTest extends ClientTestBase {

    @Override
    public int getPort() {
        return 8180;
    }

    @Test
    public void testEndToEnd() {
        EventBus eb = Vertx.vertx().eventBus();

        MessageConsumer<String> serviceB = eb.consumer("serviceB");
        serviceB.handler(message -> {
            System.out.println("Service B received a message: " + message.body());
            message.reply("Service B says thanks for the message");
        });

        MessageConsumer<String> serviceA = eb.consumer("serviceA");
        serviceA.handler(message -> {
            System.out.println("Service A received a message: " + message.body() + ", sending to service B [thread=" +
                    Thread.currentThread());

            eb.send("serviceB", message.body() + " being relayed", new Handler<AsyncResult<Message<String>>>() {
                @Override
                public void handle(AsyncResult<Message<String>> message2) {
                    System.out.println("Service A received reply: " + message2.result().body());

                    message.reply("Service A relaying response: " + message2.result().body());
                }
            });
        });

        HttpServer server = Vertx.vertx().createHttpServer();

        server.requestHandler(req -> {
            req.bodyHandler(body -> {
                System.out.println("Sending received message [" + body.toString() + "] to service A");

                eb.send("serviceA", body.toString(), new Handler<AsyncResult<Message<String>>>() {
                    @Override
                    public void handle(AsyncResult<Message<String>> message3) {
                        System.out.println("Service A received reply: " + message3.result().body());

                        req.response().end("REST service relaying response: " + message3.result().body());
                    }
                });
            });
        }).listen(8080);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for vertx service startup");
        }

        Vertx.vertx().createHttpClient().post(8080, "localhost", "/hello_end_to_end", resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));

                server.close();
            });
        }).end("Hello World");

        evaluateBTxnFragments();
    }

    protected void evaluateBTxnFragments() {
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Check stored business transactions (including 1 for test client)
        assertEquals(4, getTestTraceServer().getTraces().size());

        Consumer consumerREST = null;
        Consumer consumerServiceA = null;
        Consumer consumerServiceB = null;
        Producer producerREST = null;

        for (Trace trace : getTestTraceServer().getTraces()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (!trace.getNodes().isEmpty()) {
                if (trace.getNodes().get(0).getClass() == Producer.class) {
                    producerREST = (Producer) trace.getNodes().get(0);
                } else if (trace.getNodes().get(0).getClass() == Consumer.class) {
                    Consumer consumer = (Consumer) trace.getNodes().get(0);
                    if (consumer.getUri().equals("serviceA")) {
                        consumerServiceA = consumer;
                    } else if (consumer.getUri().equals("serviceB")) {
                        consumerServiceB = consumer;
                    } else {
                        consumerREST = consumer;
                    }
                }
            }

            // Check btxn name is set
            assertEquals("Business transaction name should be 'testvertx-end-to-end'",
                                "testvertx-end-to-end", trace.getBusinessTransaction());
        }

        assertNotNull("consumerREST null", consumerREST);
        assertNotNull("consumerServiceA null", consumerServiceA);
        assertNotNull("consumerServiceB null", consumerServiceB);
        assertNotNull("producerREST null", producerREST);

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(consumerREST.getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producer in consumerREST", 1, producers.size());

        Producer producerServiceA = producers.get(0);
        producers.clear();

        findNodes(consumerServiceA.getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producer in consumerServiceA", 1, producers.size());

        Producer producerServiceB = producers.get(0);
        producers.clear();

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(producerREST, consumerREST);
        checkInteractionCorrelationIdentifiers(producerServiceA, consumerServiceA);
        checkInteractionCorrelationIdentifiers(producerServiceB, consumerServiceB);
    }

    /**
     * This method finds nodes within a hierarchy of the required type.
     *
     * @param nodes The nodes to recursively check
     * @param cls The class of interest
     * @param results The results
     */
    @SuppressWarnings("unchecked")
    protected <T extends Node> void findNodes(List<Node> nodes, Class<T> cls, List<T> results) {
        for (Node n : nodes) {
            if (n instanceof ContainerNode) {
                findNodes(((ContainerNode) n).getNodes(), cls, results);
            }

            if (cls.isAssignableFrom(n.getClass())) {
                results.add((T) n);
            }
        }
    }

    /**
     * This method checks that two correlation identifiers are equivalent.
     *
     * @param producer The producer
     * @param consumer The consumer
     */
    protected void checkInteractionCorrelationIdentifiers(Producer producer, Consumer consumer) {
        CorrelationIdentifier pcid = producer.getCorrelationIds().iterator().next();
        CorrelationIdentifier ccid = consumer.getCorrelationIds().iterator().next();

        assertEquals(pcid, ccid);
    }

}
