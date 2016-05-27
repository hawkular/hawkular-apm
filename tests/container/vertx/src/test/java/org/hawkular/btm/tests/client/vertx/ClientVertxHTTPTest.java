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
package org.hawkular.btm.tests.client.vertx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.trace.Consumer;
import org.hawkular.btm.api.model.trace.CorrelationIdentifier;
import org.hawkular.btm.api.model.trace.Producer;
import org.hawkular.btm.api.model.trace.Trace;
import org.hawkular.btm.tests.common.ClientTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * @author gbrown
 */
public class ClientVertxHTTPTest extends ClientTestBase {

    /**  */
    private static final String QUERY_STRING = "to=me";
    /**  */
    private static final String HELLO_PATH = "/hello_http";
    /**  */
    private static final String RESPONSE = "<html><body><h1>Hello from vert.x!</h1></body></html>";
    /**  */
    private static final String MY_REQUEST = "My request";
    private static HttpServer server;

    @Override
    public int getPort() {
        return 8180;
    }

    @BeforeClass
    public static void initClass() {
        server = Vertx.vertx().createHttpServer();

        server.requestHandler(req -> {
            req.bodyHandler(body -> {
                System.out.println("Received message [" + body.toString() + "]");

                req.response().putHeader("content-type", "text/html")
                    .end(RESPONSE);
            });
        }).listen(8080);
    }

    @AfterClass
    public static void closeClass() {
        server.close();
    }

    @Test
    public void testHTTPGET() {
        Vertx.vertx().createHttpClient().getNow(8080, "localhost", HELLO_PATH, resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
        });

        evaluateBTxnFragments(true, false);
    }

    @Test
    public void testHTTPGETWithQS() {
        Vertx.vertx().createHttpClient().getNow(8080, "localhost", HELLO_PATH + "?" + QUERY_STRING, resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
        });

        evaluateBTxnFragments(true, true);
    }

    @Test
    public void testHTTPPUT() {
        Vertx.vertx().createHttpClient().put(8080, "localhost", HELLO_PATH, resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
        }).end(MY_REQUEST);

        evaluateBTxnFragments(false, false);
    }

    @Test
    public void testHTTPPOST() {
        Vertx.vertx().createHttpClient().post(8080, "localhost", HELLO_PATH, resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> {
                System.out.println("Got data " + body.toString("ISO-8859-1"));
            });
        }).end(MY_REQUEST);

        evaluateBTxnFragments(false, false);
    }

    protected void evaluateBTxnFragments(boolean get, boolean qs) {
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Check stored business transactions (including 1 for test client)
        assertEquals(2, getTestTraceServer().getTraces().size());

        Consumer consumer = null;
        Producer producer = null;

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

        // Paths
        assertEquals(HELLO_PATH, consumer.getUri());
        assertEquals(HELLO_PATH, producer.getUri());

        if (qs) {
            assertEquals(QUERY_STRING, consumer.getDetails().get("http_query"));
            assertEquals(QUERY_STRING, producer.getDetails().get("http_query"));
        }

        // Details
        assertEquals("Hello World", consumer.getDetails().get("hello"));
        assertEquals("Hello World", producer.getDetails().get("hello"));

        assertTrue(consumer.getOut().getContent().containsKey("all"));
        assertEquals(RESPONSE, consumer.getOut().getContent().get("all").getValue());

        assertTrue(producer.getOut().getContent().containsKey("all"));
        assertEquals(RESPONSE, producer.getOut().getContent().get("all").getValue());

        // If not 'get' then check message content has been included
        if (!get) {
            // Verify content
            assertTrue(consumer.getIn().getContent().containsKey("all"));
            assertEquals(MY_REQUEST, consumer.getIn().getContent().get("all").getValue());

            assertTrue(producer.getIn().getContent().containsKey("all"));
            assertEquals(MY_REQUEST, producer.getIn().getContent().get("all").getValue());
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
