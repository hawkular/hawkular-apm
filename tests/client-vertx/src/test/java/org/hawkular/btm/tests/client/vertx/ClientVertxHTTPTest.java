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

//import static org.junit.Assert.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.tests.btxn.TestBTxnService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.vertx.core.Vertx;

/**
 * @author gbrown
 */
public class ClientVertxHTTPTest {

    private static TestBTxnService btxnService = new TestBTxnService();

    @BeforeClass
    public static void init() {
        Vertx.vertx().createHttpServer().requestHandler(req -> {
            req.response().putHeader("content-type", "text/html")
                .end("<html><body><h1>Hello from vert.x!</h1></body></html>");
          }).listen(8080);

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
                        +btxnService.getBusinessTransactions().size());
        btxnService.getBusinessTransactions().clear();
        System.out.println("Cleared: count="+btxnService.getBusinessTransactions().size());
    }

    @Test
    public void testHTTPGET() {
        Vertx.vertx().createHttpClient().getNow(8080, "localhost", "/hello_get", resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
              System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
          });

        evaluateBTxnFragments();
    }

    @Test
    public void testHTTPPUT() {
        Vertx.vertx().createHttpClient().put(8080, "localhost", "/hello_put", resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
              System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
          }).end();

        evaluateBTxnFragments();
    }

    @Test
    public void testHTTPPOST() {
        Vertx.vertx().createHttpClient().post(8080, "localhost", "/hello_put", resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
              System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
          }).end();

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
        assertEquals(2, btxnService.getBusinessTransactions().size());

        Consumer consumer=null;
        Producer producer=null;

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
                    producer = (Producer)btxn.getNodes().get(0);
                } else if (btxn.getNodes().get(0).getClass() == Consumer.class) {
                    consumer = (Consumer)btxn.getNodes().get(0);
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

}
