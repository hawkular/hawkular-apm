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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class JavaxServletAsyncServerITest extends JavaxServletTestBase {

    private static final String GREETINGS_REQUEST = "Greetings";
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
