/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.tests.client.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.tests.btxn.TestBTxnService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
public class ClientVertxEventBusTest {

    private static TestBTxnService btxnService = new TestBTxnService();

    @BeforeClass
    public static void init() {
        try {
            btxnService.setPort(8180);
            btxnService.setShutdownTimer(-1); // Disable timer
            btxnService.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void close() {
        try {
            btxnService.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            synchronized (btxnService) {
                btxnService.wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait after test close");
        }
    }

    @After
    public void afterTest() {
        System.out.println("Clearing previous business transactions: count="
                + btxnService.getBusinessTransactions().size());
        btxnService.getBusinessTransactions().clear();
        System.out.println("Cleared: count=" + btxnService.getBusinessTransactions().size());
    }

    @Test
    public void testEBPointToPointWithReply() {
        EventBus eb = Vertx.vertx().eventBus();

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
                    }
                });

            } else {
                System.out.println("Registration failed!");
            }
        });

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mc.unregister();

        checkBTxnFragments();
    }

    @Test
    public void testEBPointToPointWithoutReply() {
        EventBus eb = Vertx.vertx().eventBus();

        MessageConsumer<String> mc = eb.consumer("news.uk.sport");
        mc.handler(message -> {
            System.out.println("I have received a message: " + message.body());
        });

        mc.completionHandler(res -> {
            if (res.succeeded()) {
                System.out.println("The handler registration has reached all nodes");

                eb.send("news.uk.sport", "Yay! Someone kicked a ball");

            } else {
                System.out.println("Registration failed!");
            }
        });

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mc.unregister();

        checkBTxnFragments();
    }

    protected void checkBTxnFragments() {
        // Check stored business transactions (including 1 for test client)
        assertEquals(2, btxnService.getBusinessTransactions().size());

        Consumer consumer = null;
        Producer producer = null;

        for (BusinessTransaction btxn : btxnService.getBusinessTransactions()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(btxn));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (!btxn.getNodes().isEmpty()) {
                if (btxn.getNodes().get(0).getClass() == Producer.class) {
                    producer = (Producer) btxn.getNodes().get(0);
                } else if (btxn.getNodes().get(0).getClass() == Consumer.class) {
                    consumer = (Consumer) btxn.getNodes().get(0);
                }
            }
        }

        assertNotNull("consumer null", consumer);
        assertNotNull("producer null", producer);

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(producer, consumer);
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
}
