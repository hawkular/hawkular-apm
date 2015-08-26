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
package org.hawkular.btm.tests.client.jetty;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.tests.common.ClientTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class ClientJettyReaderWriterTest extends ClientTestBase {

    /**  */
    private static final String GREETINGS_REQUEST = "Greetings";
    /**  */
    private static final String TEST_HEADER = "test-header";
    /**  */
    private static final String HELLO_URL = "http://localhost:8180/hello";
    /**  */
    private static final String HELLO_WORLD_RESPONSE = "<h1>HELLO WORLD</h1>";

    private static Server server = null;

    @BeforeClass
    public static void initClass() {
        server = new Server(8180);

        server.setHandler(new AbstractHandler() {

            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                    HttpServletResponse response)
                    throws IOException, ServletException {

                BufferedReader reader = request.getReader();

                String str = reader.readLine();
                System.out.println("REQUEST(READER) RECEIVED: " + str);

                response.setContentType("text/html; charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);

                PrintWriter out = response.getWriter();

                out.println(HELLO_WORLD_RESPONSE);

                baseRequest.setHandled(true);
            }
        });

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
    public void test() {
        try {
            URL url = new URL(HELLO_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            connection.setRequestProperty(TEST_HEADER, "test-value");

            java.io.OutputStream os = connection.getOutputStream();

            os.write(GREETINGS_REQUEST.getBytes());

            os.flush();
            os.close();

            java.io.InputStream is = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder builder = new StringBuilder();
            String str = null;

            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }

            is.close();

            assertEquals("Unexpected response code", 200, connection.getResponseCode());

            assertEquals(HELLO_WORLD_RESPONSE, builder.toString());

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
        assertEquals(2, getTestBTMServer().getBusinessTransactions().size());

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);
        findNodes(getTestBTMServer().getBusinessTransactions().get(1).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
        assertTrue("testProducer does not have test header",
                testProducer.getRequest().getHeaders().containsKey(TEST_HEADER));

        List<Consumer> consumers = new ArrayList<Consumer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Consumer.class, consumers);
        findNodes(getTestBTMServer().getBusinessTransactions().get(1).getNodes(), Consumer.class, consumers);

        assertEquals("Expecting 1 consumers", 1, consumers.size());

        Consumer testConsumer = consumers.get(0);

        assertEquals(HELLO_URL, testConsumer.getUri());

        assertNotNull(testConsumer.getRequest());

        // Check headers
        assertFalse("testConsumer has no headers", testConsumer.getRequest().getHeaders().isEmpty());
        assertTrue("testConsumer does not have test header",
                testConsumer.getRequest().getHeaders().containsKey(TEST_HEADER));

        // Check contents
        assertTrue(testConsumer.getRequest().getContent().containsKey("all"));
        assertEquals(GREETINGS_REQUEST, testConsumer.getRequest().getContent().get("all").getValue());

        /* TODO: Awaiting solution for HWKBTM-137
        assertNotNull(testConsumer.getResponse());
        assertTrue(testConsumer.getResponse().getContent().containsKey("all"));
        assertEquals(HELLO_WORLD_RESPONSE, testConsumer.getResponse().getContent().get("all").getValue());
         */
    }

}
