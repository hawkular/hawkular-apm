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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Consumer;
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
public class JavaxServletSyncServerITest extends ClientTestBase {

    private static final String TEST_USER = "jdoe";
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

        LoginService loginService = new HashLoginService("MyRealm",
                "src/test/resources/realm.properties");
        server.addBean(loginService);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);

        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "user", "admin" });

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(EmbeddedServlet.class, "/hello");
        security.setHandler(context);

        server.start();
    }

    @AfterClass
    public static void closeClass() throws Exception {
        server.stop();
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
    public void testGetWithFault() {
        testJettyServlet("GET", HELLO_URL, null, true, true);
    }

    @Test
    public void testPut() {
        testJettyServlet("PUT", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPutWithFault() {
        testJettyServlet("PUT", HELLO_URL, GREETINGS_REQUEST, true, true);
    }

    @Test
    public void testPost() {
        testJettyServlet("POST", HELLO_URL, GREETINGS_REQUEST, false, true);
    }

    @Test
    public void testPostWithFault() {
        testJettyServlet("POST", HELLO_URL, GREETINGS_REQUEST, true, true);
    }

    protected void testJettyServlet(String method, String urlstr, String reqdata, boolean fault,
            boolean respexpected) {
        String path = null;

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

            String authString = TEST_USER + ":" + "password";
            String encoded = Base64.getEncoder().encodeToString(authString.getBytes());

            connection.setRequestProperty("Authorization", "Basic " + encoded);

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

        List<Consumer> consumers = new ArrayList<Consumer>();
        NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Consumer.class, consumers);
        NodeUtil.findNodes(getApmMockServer().getTraces().get(1).getNodes(), Consumer.class, consumers);

        assertEquals("Expecting 1 consumers", 1, consumers.size());

        Consumer testConsumer = consumers.get(0);

        assertEquals(path, testConsumer.getUri());

        if (urlstr.endsWith(QUERY_STRING)) {
            assertEquals(QUERY_STRING,
                    testConsumer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
        }

        assertEquals(method, testConsumer.getOperation());

        assertEquals(fault ? "401" : "200",
                    testConsumer.getProperties(Tags.HTTP_STATUS.getKey()).iterator().next().getValue());
    }

    public static class EmbeddedServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void service(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

            InputStream is = request.getInputStream();

            byte[] b = new byte[is.available()];
            is.read(b);

            is.close();

            System.out.println("REQUEST(INPUTSTREAM) RECEIVED: " + new String(b));

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
        }
    }

}
