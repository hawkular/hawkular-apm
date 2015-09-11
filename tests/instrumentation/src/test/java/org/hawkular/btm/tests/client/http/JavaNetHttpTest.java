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
package org.hawkular.btm.tests.client.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static io.undertow.Handlers.path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.tests.common.ClientTestBase;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * @author gbrown
 */
public class JavaNetHttpTest extends ClientTestBase {

    /**  */
    private static final String SAY_HELLO_URL = "http://localhost:8180/sayHello";

    /**  */
    private static final String SAY_HELLO = "Say Hello";

    /**  */
    private static final String HELLO_WORLD = "Hello World";

    private Undertow server = null;

    @Override
    public void init() {
        server = Undertow.builder()
                .addHttpListener(8180, "localhost")
                .setHandler(path().addPrefixPath("sayHello", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (!exchange.getRequestHeaders().contains("test-fault")) {
                            if (!exchange.getRequestHeaders().contains("test-no-data")) {
                                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                                exchange.getResponseSender().send(HELLO_WORLD);
                            }
                        } else {
                            exchange.setResponseCode(401);
                        }
                    }
                })).build();

        server.start();

        super.init();
    }

    @Override
    public void close() {
        server.stop();

        super.close();
    }

    @Test
    public void testHttpURLConnectionGET() {
        testHttpURLConnection("GET", null, false, true, false);
    }

    @Test
    public void testHttpURLConnectionGETNoData1() {
        testHttpURLConnection("GET", null, false, false, false);
    }

    @Test
    @org.junit.Ignore("Awaiting solution for HWKBTM-158")
    public void testHttpURLConnectionGETNoData2() {
        // Although no content actually passed, set this flag to simulate
        // a user accidently defines a content processing definition
        setProcessContent(true);
        testHttpURLConnection("GET", null, false, false, false);
    }

    @Test
    public void testHttpURLConnectionGETAsync() {
        testHttpURLConnection("GET", null, false, true, true);
    }

    @Test
    public void testHttpURLConnectionGETWithContent() {
        setProcessContent(true);
        testHttpURLConnection("GET", null, false, true, false);
    }

    @Test
    public void testHttpURLConnectionGETWithContentAsync() {
        setProcessContent(true);
        testHttpURLConnection("GET", null, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPUT() {
        testHttpURLConnection("PUT", SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPUTAsync() {
        testHttpURLConnection("PUT", SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPUTWithContent() {
        setProcessContent(true);
        testHttpURLConnection("PUT", SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPUTWithContentAsync() {
        setProcessContent(true);
        testHttpURLConnection("PUT", SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPOST() {
        testHttpURLConnection("POST", SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPOSTAsync() {
        testHttpURLConnection("POST", SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPOSTWithContent() {
        setProcessContent(true);
        testHttpURLConnection("POST", SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPOSTWithContentAsync() {
        setProcessContent(true);
        testHttpURLConnection("POST", SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionGETWithFault() {
        testHttpURLConnection("GET", null, true, true, false);
    }

    protected void testHttpURLConnection(String method, String reqdata, boolean fault, boolean respexpected,
            boolean async) {
        Thread testThread=Thread.currentThread();

        try {
            URL url = new URL(SAY_HELLO_URL);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            connection.setRequestProperty("test-header", "test-value");
            if (fault) {
                connection.setRequestProperty("test-fault", "true");
            }
            if (!respexpected) {
                connection.setRequestProperty("test-no-data", "true");
            }

            if (reqdata != null) {
                OutputStream os = connection.getOutputStream();

                os.write(reqdata.getBytes());

                os.flush();
                os.close();
            } else if (fault || !respexpected) {
                connection.connect();
            }

            Callable<Void> task = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    java.io.InputStream is = null;

                    if (async && testThread == Thread.currentThread()) {
                        fail("Async response should be received in different thread");
                    }

                    if (!fault) {
                        if (respexpected) {
                            is = connection.getInputStream();
                        }

                        assertEquals("Unexpected response code", 200, connection.getResponseCode());

                        if (respexpected) {
                            if (is == null) {
                                is = connection.getInputStream();
                            }

                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                            StringBuilder builder = new StringBuilder();
                            String str = null;

                            while ((str = reader.readLine()) != null) {
                                builder.append(str);
                            }
                            is.close();

                            assertEquals(HELLO_WORLD, builder.toString());
                        }
                    } else {
                        assertEquals("Unexpected fault response code", 401, connection.getResponseCode());
                    }
                    return null;
                }
            };

            if (async) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Void> future = executor.submit(task);
                future.get();
            } else {
                task.call();
            }
        } catch (Exception e) {
            fail("Failed to perform '" + method + "': " + e);
        }

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        for (BusinessTransaction btxn : getTestBTMServer().getBusinessTransactions()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(btxn));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        assertEquals("Hello World", testProducer.getDetails().get("hello"));

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());

        if (fault) {
            assertEquals("401", testProducer.getFault());
            assertEquals("Unauthorized", testProducer.getFaultDescription());
        } else {

            if (isProcessContent()) {
                // Check request value
                if (!method.equals("GET")) {
                    assertTrue(testProducer.getRequest().getContent().containsKey("all"));
                    assertEquals(SAY_HELLO, testProducer.getRequest().getContent().get("all").getValue());
                }
                // Check response value
                if (respexpected) {
                    assertTrue(testProducer.getResponse().getContent().containsKey("all"));
                    assertEquals(HELLO_WORLD, testProducer.getResponse().getContent().get("all").getValue());
                }
            } else {
                assertFalse(testProducer.getRequest().getContent().containsKey("all"));
            }
        }
    }

}
