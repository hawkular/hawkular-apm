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
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send(HELLO_WORLD);
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
        testHttpURLConnection("GET", null);
    }

    @Test
    public void testHttpURLConnectionPUT() {
        testHttpURLConnection("PUT", SAY_HELLO);
    }

    @Test
    public void testHttpURLConnectionPOST() {
        testHttpURLConnection("POST", SAY_HELLO);
    }

    @Test
    public void testHttpURLConnectionGETWithContent() {
        setProcessContent(true);
        testHttpURLConnection("GET", null);
    }

    @Test
    public void testHttpURLConnectionPUTWithContent() {
        setProcessContent(true);
        testHttpURLConnection("PUT", SAY_HELLO);
    }

    @Test
    public void testHttpURLConnectionPOSTWithContent() {
        setProcessContent(true);
        testHttpURLConnection("POST", SAY_HELLO);
    }

    protected void testHttpURLConnection(String method, String data) {
        try {
            URL url = new URL(SAY_HELLO_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            connection.setRequestProperty("test-header", "test-value");

            if (data == null) {
                connection.connect();
            } else {
                OutputStream os = connection.getOutputStream();

                os.write(data.getBytes());

                os.flush();
                os.close();
            }

            assertEquals("Unexpected response code", 200, connection.getResponseCode());

            java.io.InputStream is = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder builder = new StringBuilder();
            String str = null;

            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }

            is.close();

            assertEquals(HELLO_WORLD, builder.toString());

        } catch (Exception e) {
            fail("Failed to perform get: " + e);
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

        if (isProcessContent()) {
            // Check request value
            if (!method.equals("GET")) {
                assertTrue(testProducer.getRequest().getContent().containsKey("all"));
                assertEquals(SAY_HELLO, testProducer.getRequest().getContent().get("all").getValue());
            }
            // Check response value
            assertTrue(testProducer.getResponse().getContent().containsKey("all"));
            assertEquals(HELLO_WORLD, testProducer.getResponse().getContent().get("all").getValue());
        } else {
            assertFalse(testProducer.getRequest().getContent().containsKey("all"));
        }
    }

}
