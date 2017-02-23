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
package org.hawkular.apm.tests.agent.opentracing.server.http;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hawkular.apm.tests.agent.opentracing.common.OpenTracingAgentTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

/**
 * @author gbrown
 */
public class JavaxServletAsyncServerITest extends OpenTracingAgentTestBase {

    private static final String GREETINGS_REQUEST = "Greetings";
    private static final String TEST_HEADER = "test-header";
    private static final String HELLO_URL = "http://localhost:8180/hello";
    private static final String QUERY_STRING = "to=me";
    private static final String HELLO_URL_WITH_QS = HELLO_URL + "?" + QUERY_STRING;
    private static final String HELLO_WORLD_RESPONSE = "<h1>HELLO WORLD</h1>";

    private static Server server = null;

    @BeforeClass
    public static void initClass() throws Exception {
        server = new Server(8180);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder asyncHolder = context.addServlet(EmbeddedAsyncServlet.class, "/hello");
        asyncHolder.setAsyncSupported(true);
        server.setHandler(context);

        server.start();
    }

    @AfterClass
    public static void closeClass() throws Exception {
        server.stop();
    }

    @Test
    public void testGet() throws IOException {
        testJettyServlet("GET", HELLO_URL, null, false, true);
    }

    @Test
    public void testGetWithFault() throws IOException {
        testJettyServlet("GET", HELLO_URL, null, true, true);
    }

    @Test
    public void testGetWithQS() throws IOException {
        testJettyServlet("GET", HELLO_URL_WITH_QS, null, false, true);
    }

    @Test
    public void testGetNoResponse() throws IOException {
        testJettyServlet("GET", HELLO_URL, null, false, false);
    }

    @Test
    public void testPut() throws IOException {
        testJettyServlet("PUT", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPutWithFault() throws IOException {
        testJettyServlet("PUT", HELLO_URL, GREETINGS_REQUEST, true, true);
    }

    @Test
    public void testPost() throws IOException {
        testJettyServlet("POST", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPostWithFault() throws IOException {
        testJettyServlet("POST", HELLO_URL, GREETINGS_REQUEST, true, true);
    }

    protected void testJettyServlet(String method, String urlstr, String reqdata, boolean fault,
            boolean respexpected) throws IOException {
        URL url = new URL(urlstr);
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

        Wait.until(() -> getTracer().finishedSpans().size() == 2);

        List<MockSpan> spans = getTracer().finishedSpans();
        assertEquals(2, spans.size());

        MockSpan serverSpan=spans.stream()
                .filter(s -> s.tags().get(Tags.SPAN_KIND.getKey()).equals(Tags.SPAN_KIND_SERVER))
                .findFirst().get();
        assertEquals(method, serverSpan.operationName());
        assertEquals(toHttpURL(url), serverSpan.tags().get(Tags.HTTP_URL.getKey()));
        assertEquals(fault ? "401" : "200", serverSpan.tags().get(Tags.HTTP_STATUS.getKey()));
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
                        InputStream is = request.getInputStream();

                        byte[] b = new byte[is.available()];
                        is.read(b);

                        is.close();

                        System.out.println("REQUEST(ASYNC INPUTSTREAM) RECEIVED: " + new String(b));

                        response.setContentType("text/html; charset=utf-8");

                        if (request.getHeader("test-fault") != null) {
                            response.sendError(401, "Unauthorized");
                        } else {
                            response.setStatus(HttpServletResponse.SC_OK);

                            if (request.getHeader("test-no-data") == null) {
                                OutputStream os = response.getOutputStream();

                                byte[] resp = HELLO_WORLD_RESPONSE.getBytes();

                                os.write(resp, 0, resp.length);

                                os.flush();
                                os.close();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to handle servlet request");
                        e.printStackTrace();
                    }

                    ctxt.complete();
                }
            });
        }
    }

}
