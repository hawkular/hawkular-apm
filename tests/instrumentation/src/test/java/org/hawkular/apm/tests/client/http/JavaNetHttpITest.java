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
package org.hawkular.apm.tests.client.http;

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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

import io.opentracing.tag.Tags;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * @author gbrown
 */
public class JavaNetHttpITest extends ClientTestBase {

    private static final String SAY_HELLO_URL = "http://localhost:8180/sayHello";
    private static final String QUERY_STRING = "to=me";
    private static final String SAY_HELLO_URL_WITH_QS = SAY_HELLO_URL + "?" + QUERY_STRING;
    private static final String SAY_HELLO = "Say Hello";
    private static final String HELLO_WORLD = "Hello World";

    private Undertow server = null;

    @Override
    public void init() {
        server = Undertow.builder()
                .addHttpListener(8180, "localhost")
                .setHandler(path().addPrefixPath("sayHello", new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (!exchange.getRequestHeaders().contains(Constants.HAWKULAR_APM_TRACEID)) {
                            exchange.setResponseCode(400);
                        } else if (!exchange.getRequestHeaders().contains("test-fault")) {
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
    public void testHttpURLConnectionGET() throws Exception {
        testHttpURLConnection("GET", SAY_HELLO_URL, null, false, true, false);
    }

    @Test
    public void testHttpURLConnectionGETWithQS() throws Exception {
        testHttpURLConnection("GET", SAY_HELLO_URL_WITH_QS, null, false, true, false);
    }

    @Test
    public void testHttpURLConnectionGETNoData1() throws Exception {
        testHttpURLConnection("GET", SAY_HELLO_URL, null, false, false, false);
    }

    @Test
    @org.junit.Ignore("Awaiting solution for HWKBTM-158")
    public void testHttpURLConnectionGETNoData2() throws Exception {
        // Although no content actually passed, set this flag to simulate
        // a user accidently defines a content processing definition
        setProcessContent(true);
        testHttpURLConnection("GET", SAY_HELLO_URL, null, false, false, false);
    }

    @Test
    public void testHttpURLConnectionGETAsync() throws Exception {
        testHttpURLConnection("GET", SAY_HELLO_URL, null, false, true, true);
    }

    @Test
    public void testHttpURLConnectionGETWithContent() throws Exception {
        setProcessContent(true);
        testHttpURLConnection("GET", SAY_HELLO_URL, null, false, true, false);
    }

    @Test
    public void testHttpURLConnectionGETWithContentAsync() throws Exception {
        setProcessContent(true);
        testHttpURLConnection("GET", SAY_HELLO_URL, null, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPUT() throws Exception {
        testHttpURLConnection("PUT", SAY_HELLO_URL, SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPUTAsync() throws Exception {
        testHttpURLConnection("PUT", SAY_HELLO_URL, SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPUTWithContent() throws Exception {
        setProcessContent(true);
        testHttpURLConnection("PUT", SAY_HELLO_URL, SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPUTWithContentAsync() throws Exception {
        setProcessContent(true);
        testHttpURLConnection("PUT", SAY_HELLO_URL, SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPOST() throws Exception {
        testHttpURLConnection("POST", SAY_HELLO_URL, SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPOSTAsync() throws Exception {
        testHttpURLConnection("POST", SAY_HELLO_URL, SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionPOSTWithContent() throws Exception {
        setProcessContent(true);
        testHttpURLConnection("POST", SAY_HELLO_URL, SAY_HELLO, false, true, false);
    }

    @Test
    public void testHttpURLConnectionPOSTWithContentAsync() throws Exception {
        setProcessContent(true);
        testHttpURLConnection("POST", SAY_HELLO_URL, SAY_HELLO, false, true, true);
    }

    @Test
    public void testHttpURLConnectionGETWithFault() throws Exception {
        testHttpURLConnection("GET", SAY_HELLO_URL, null, true, true, false);
    }

    protected void testHttpURLConnection(String method, String urlstr, String reqdata, boolean fault,
            boolean respexpected, boolean async) throws Exception {
        Thread testThread = Thread.currentThread();

        URL url = new URL(urlstr);
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

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        String path = new URL(SAY_HELLO_URL).getPath();

        assertEquals(path, testProducer.getUri());

        assertEquals("Hello World", testProducer.getProperties("hello").iterator().next().getValue());

        if (urlstr.endsWith(QUERY_STRING)) {
            assertEquals(QUERY_STRING, testProducer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
        }

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getIn().getHeaders().isEmpty());

        if (fault) {
            assertEquals("401", testProducer.getProperties(Tags.HTTP_STATUS.getKey())
                    .iterator().next().getValue());
        } else {

            if (isProcessContent()) {
                // Check request value
                if (!method.equals("GET")) {
                    assertTrue(testProducer.getIn().getContent().containsKey("all"));
                    assertEquals(SAY_HELLO, testProducer.getIn().getContent().get("all").getValue());
                }
                // Check response value
                if (respexpected) {
                    assertTrue(testProducer.getOut().getContent().containsKey("all"));
                    assertEquals(HELLO_WORLD, testProducer.getOut().getContent().get("all").getValue());
                }
            } else {
                assertFalse(testProducer.getIn().getContent().containsKey("all"));
            }
        }
    }

}
