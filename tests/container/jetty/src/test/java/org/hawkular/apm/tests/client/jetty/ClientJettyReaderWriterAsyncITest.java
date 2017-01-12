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
package org.hawkular.apm.tests.client.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.opentracing.tag.Tags;

/**
 * @author gbrown
 */
public class ClientJettyReaderWriterAsyncITest extends ClientTestBase {

    private static final String GREETINGS_REQUEST = "Greetings";
    private static final String TEST_HEADER = "test-header";
    private static final String HELLO_URL = "http://localhost:8180/hello";
    private static final String QUERY_STRING = "to=me";
    private static final String HELLO_URL_WITH_QS = HELLO_URL + "?" + QUERY_STRING;
    private static final String HELLO_WORLD_RESPONSE = "<h1>HELLO WORLD</h1>";

    private static Server server = null;

    @BeforeClass
    public static void initClass() {
        server = new Server(8180);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(EmbeddedAsyncServlet.class, "/hello");
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
            fail("Failed to start server: " + e);
        }
    }

    @AfterClass
    public static void closeClass() {
        try {
            server.stop();
        } catch (Exception e) {
            fail("Failed to stop server: " + e);
        }
    }

    @Test
    public void testGet() {
        testJettyServlet("GET", HELLO_URL, null, false, true);
    }

    @Test
    public void testGetWithQS() {
        testJettyServlet("GET", HELLO_URL_WITH_QS, null, false, true);
    }

    @Test
    public void testGetNoResponse() {
        testJettyServlet("GET", HELLO_URL, null, false, false);
    }

    @Test
    public void testGetWithContent() {
        setProcessContent(true);
        testJettyServlet("GET", HELLO_URL, null, false, true);
    }

    @Test
    public void testPut() {
        testJettyServlet("PUT", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPutWithContent() {
        setProcessContent(true);
        testJettyServlet("PUT", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPost() {
        testJettyServlet("POST", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPostWithContent() {
        setProcessContent(true);
        testJettyServlet("POST", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    protected void testJettyServlet(String method, String urlstr, String reqdata, boolean fault,
                            boolean respexpected) {
        String path=null;

        try {
            URL url = new URL(urlstr);
            path = url.getPath();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod(method);

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            connection.setRequestProperty(TEST_HEADER, "test-value");
            if (fault) {
                connection.setRequestProperty("test-fault", "true");
            }
            if (!respexpected) {
                connection.setRequestProperty("test-no-data", "true");
            }

            java.io.InputStream is = null;

            if (reqdata != null) {
                java.io.OutputStream os = connection.getOutputStream();

                os.write(reqdata.getBytes());

                os.flush();
                os.close();
            } else if (fault || !respexpected) {
                connection.connect();
            } else if (respexpected) {
                is = connection.getInputStream();
            }

            if (!fault) {
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

                    assertEquals(HELLO_WORLD_RESPONSE, builder.toString());
                }
            } else {
                assertEquals("Unexpected fault response code", 401, connection.getResponseCode());
            }

        } catch (Exception e) {
            fail("Failed to perform get: " + e);
        }

        Wait.until(() -> getApmMockServer().getTraces().size() == 2);

        // Check stored traces (including 1 for the test client)
        assertEquals(2, getApmMockServer().getTraces().size());

        List<Producer> producers = new ArrayList<Producer>();
        NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class, producers);
        NodeUtil.findNodes(getApmMockServer().getTraces().get(1).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(path, testProducer.getUri());

        if (urlstr.endsWith(QUERY_STRING)) {
            assertEquals(QUERY_STRING, testProducer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
        }

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getIn().getHeaders().isEmpty());
        assertTrue("testProducer does not have test header",
                testProducer.getIn().getHeaders().containsKey(TEST_HEADER));

        List<Consumer> consumers = new ArrayList<Consumer>();
        NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Consumer.class, consumers);
        NodeUtil.findNodes(getApmMockServer().getTraces().get(1).getNodes(), Consumer.class, consumers);

        assertEquals("Expecting 1 consumers", 1, consumers.size());

        Consumer testConsumer = consumers.get(0);

        assertEquals(path, testConsumer.getUri());

        if (urlstr.endsWith(QUERY_STRING)) {
            assertEquals(QUERY_STRING, testConsumer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
        }

        assertEquals(method, testConsumer.getOperation());
        assertEquals(method, testConsumer.getProperties("http_method").iterator().next().getValue());

        // Check headers
        assertFalse("testConsumer has no headers", testConsumer.getIn().getHeaders().isEmpty());
        assertTrue("testConsumer does not have test header",
                testConsumer.getIn().getHeaders().containsKey(TEST_HEADER));

        if (fault) {
            assertEquals("401", testProducer.getProperties(Tags.HTTP_STATUS.getKey())
                                .iterator().next().getValue());
        } else {

            if (isProcessContent()) {
                // Check request value
                if (!method.equals("GET")) {
                    assertTrue(testConsumer.getIn().getContent().containsKey("all"));
                    assertEquals(GREETINGS_REQUEST, testConsumer.getIn().getContent().get("all").getValue());
                }
                // Check response value
                if (respexpected) {
                    assertTrue(testConsumer.getOut().getContent().containsKey("all"));
                    assertEquals(HELLO_WORLD_RESPONSE, testConsumer.getOut().getContent().get("all").getValue());
                }
            } else {
                assertFalse(testProducer.getIn().getContent().containsKey("all"));
            }
        }

        // Check only one trace id used for all trace fragments
        assertEquals(1, getApmMockServer().getTraces().stream().map(t -> {
            assertNotNull(t.getTraceId());
            return t.getTraceId();
        }).distinct().count());
    }

    public static class EmbeddedAsyncServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

            final AsyncContext ctxt = request.startAsync();
            ctxt.start(new Runnable() {
                @Override
                public void run() {
                    try {
                        BufferedReader reader = request.getReader();

                        reader.readLine();

                        response.setContentType("text/html; charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);

                        if (request.getHeader("test-no-data") == null) {
                            PrintWriter out = response.getWriter();

                            out.println(HELLO_WORLD_RESPONSE);
                        }
                    } catch (Exception e) {
                        fail("Failed: " + e);
                    }

                    ctxt.complete();
                }
            });
        }
    }

}
