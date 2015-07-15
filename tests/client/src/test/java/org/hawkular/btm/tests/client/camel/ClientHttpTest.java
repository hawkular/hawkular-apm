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
package org.hawkular.btm.tests.client.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import static io.undertow.Handlers.path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
public class ClientHttpTest extends ClientTestBase {

    /**  */
    private static final String SAY_HELLO_URL = "http://localhost:8180/sayHello";

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
        testHttpURLConnection("GET");
    }

    @Test
    public void testHttpURLConnectionPUT() {
        testHttpURLConnection("PUT");
    }

    @Test
    public void testHttpURLConnectionPOST() {
        testHttpURLConnection("POST");
    }

    protected void testHttpURLConnection(String method) {
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

            connection.connect();

            java.io.InputStream is = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder builder = new StringBuilder();
            String str = null;

            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }

            is.close();

            assertEquals("Unexpected response code", 200, connection.getResponseCode());

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

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

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

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGET() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL));
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPUT() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpPut(SAY_HELLO_URL));
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPOST() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpPost(SAY_HELLO_URL));
    }

    protected void testHttpClientWithoutResponseHandler(HttpUriRequest request) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            request.addHeader("test-header", "test-value");

            HttpResponse response = httpclient.execute(request);

            int status = response.getStatusLine().getStatusCode();

            assertEquals("Unexpected response code", 200, status);

            HttpEntity entity = response.getEntity();

            assertNotNull(entity);

            assertEquals(HELLO_WORLD, EntityUtils.toString(entity));

        } catch (Exception e) {
            fail("Failed to perform get: " + e);
        } finally {
            httpclient.close();
        }

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

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

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
    }

    @Test
    public void testHttpClientWithResponseHandlerGET() throws IOException {
        testHttpClientWithResponseHandler(new HttpGet(SAY_HELLO_URL));
    }

    @Test
    public void testHttpClientWithResponseHandlerPUT() throws IOException {
        testHttpClientWithResponseHandler(new HttpPut(SAY_HELLO_URL));
    }

    @Test
    public void testHttpClientWithResponseHandlerPOST() throws IOException {
        testHttpClientWithResponseHandler(new HttpPost(SAY_HELLO_URL));
    }

    protected void testHttpClientWithResponseHandler(HttpUriRequest request) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            request.addHeader("test-header", "test-value");

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();

                    assertEquals("Unexpected response code", 200, status);

                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                }

            };

            String responseBody = httpclient.execute(request, responseHandler);

            assertEquals(HELLO_WORLD, responseBody);

        } catch (Exception e) {
            fail("Failed to perform get: " + e);
        } finally {
            httpclient.close();
        }

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

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

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
    }

    @Test
    public void testJaxRSClientGET() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").get();
        String value = response.readEntity(String.class);
        response.close();

        assertEquals(HELLO_WORLD, value);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

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

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
    }

    @Test
    public void testJaxRSClientPOST() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").post(Entity.<String> text("Hello"));
        String value = response.readEntity(String.class);
        response.close();

        assertEquals(HELLO_WORLD, value);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

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

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
    }

    @Test
    public void testJaxRSClientPUT() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").put(Entity.<String> text("Hello"));
        String value = response.readEntity(String.class);
        response.close();

        assertEquals(HELLO_WORLD, value);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getTestBTMServer().getBusinessTransactions().size());

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

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestBTMServer().getBusinessTransactions().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(SAY_HELLO_URL, testProducer.getUri());

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());
    }
}
