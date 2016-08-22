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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tests.common.Wait;
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

/**
 * @author gbrown
 */
public class ClientVertxEventBusTest extends ClientTestBase {

    @Override
    public int getPort() {
        return 8180;
    }

    @Test
    public void testEBPointToPointWithReply() throws InterruptedException {
        EventBus eb = Vertx.vertx().eventBus();
        CountDownLatch latch = new CountDownLatch(1);

        MessageConsumer<String> mc = eb.consumer("news.uk.sport");
        mc.handler(message -> {
            System.out.println("I have received a message: " + message.body());
            message.reply("Thanks for the message");
        });

        mc.completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("The handler registration has reached all nodes");

                eb.send("news.uk.sport", "Yay! Someone kicked a ball", new Handler<AsyncResult<Message<String>>>() {
                    @Override
                    public void handle(AsyncResult<Message<String>> message) {
                        System.out.println("Received reply: " + message.result().body());
                        latch.countDown();
                    }
                });
            } else {
                System.out.println("Registration failed!");
            }
        });

        latch.await(2, TimeUnit.SECONDS);

        mc.unregister();

        checkBTxnFragments();
    }

    @Test
    public void testEBPointToPointWithoutReply() throws InterruptedException {
        EventBus eb = Vertx.vertx().eventBus();
        CountDownLatch latch = new CountDownLatch(1);

        MessageConsumer<String> mc = eb.consumer("news.uk.sport");
        mc.handler(message -> {
            System.out.println("I have received a message: " + message.body());
        });

        mc.completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("The handler registration has reached all nodes");
                eb.send("news.uk.sport", "Yay! Someone kicked a ball");
                latch.countDown();
            } else {
                System.out.println("Registration failed!");
            }
        });

        latch.await(2, TimeUnit.SECONDS);

        mc.unregister();

        checkBTxnFragments();
    }

    @Test
    public void testEBPubSub() throws InterruptedException {
        EventBus eb = Vertx.vertx().eventBus();
        CountDownLatch latch = new CountDownLatch(1);

        MessageConsumer<String> mc = eb.consumer("news.uk.sport.pubsub");
        mc.handler(message -> {
            System.out.println("I have received a message: " + message.body());
        });

        mc.completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("The handler registration has reached all nodes");
                eb.publish("news.uk.sport.pubsub", "Yay! Someone kicked a ball");
                latch.countDown();
            } else {
                System.out.println("Registration failed!");
            }
        });

        latch.await(2, TimeUnit.SECONDS);

        mc.unregister();

        checkBTxnFragments();
    }

    protected void checkBTxnFragments() {
        // Check stored business transactions (including 1 for test client)
        Wait.until(() -> getApmMockServer().getTraces().size() == 2);
        assertEquals(2, getApmMockServer().getTraces().size());

        Consumer consumer = null;
        Producer producer = null;

        for (Trace trace : getApmMockServer().getTraces()) {
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
                    producer = (Producer) trace.getNodes().get(0);
                } else if (trace.getNodes().get(0).getClass() == Consumer.class) {
                    consumer = (Consumer) trace.getNodes().get(0);
                }
            }
        }

        assertNotNull("consumer null", consumer);
        assertNotNull("producer null", producer);

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(producer, consumer);
    }

}
