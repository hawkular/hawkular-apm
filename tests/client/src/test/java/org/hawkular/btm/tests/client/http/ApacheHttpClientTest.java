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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static io.undertow.Handlers.path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
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
public class ApacheHttpClientTest extends ClientTestBase {

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
    public void testHttpClientWithoutResponseHandlerGET() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETNoData() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, false, false);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPUT() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpPut(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPUTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithoutResponseHandler(new HttpPut(SAY_HELLO_URL), SAY_HELLO, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPOST() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpPost(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPOSTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithoutResponseHandler(new HttpPost(SAY_HELLO_URL), SAY_HELLO, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETWithFault() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, true, true);
    }

    protected void testHttpClientWithoutResponseHandler(HttpUriRequest request, String data,
            boolean fault, boolean respexpected) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            request.addHeader("test-header", "test-value");
            if (fault) {
                request.addHeader("test-fault", "true");
            }
            if (!respexpected) {
                request.addHeader("test-no-data", "true");
            }

            if (data != null && request instanceof HttpEntityEnclosingRequest) {
                StringEntity entity = new StringEntity(data,
                        ContentType.create("text/plain", "UTF-8"));
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            }

            HttpResponse response = httpclient.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (!fault) {
                assertEquals("Unexpected response code", 200, status);

                if (respexpected) {
                    HttpEntity entity = response.getEntity();

                    assertNotNull(entity);

                    assertEquals(HELLO_WORLD, EntityUtils.toString(entity));
                }
            } else {
                assertEquals("Unexpected fault response code", 401, status);
            }

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

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getRequest().getHeaders().isEmpty());

        if (fault) {
            assertEquals("401", testProducer.getFault());
            assertEquals("Unauthorized", testProducer.getFaultDescription());
        } else {

            if (isProcessContent()) {
                // Check request value
                if (!request.getMethod().equals("GET")) {
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

    @Test
    public void testHttpClientWithResponseHandlerGET() throws IOException {
        testHttpClientWithResponseHandler(new HttpGet(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerGETWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithResponseHandler(new HttpGet(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPUT() throws IOException {
        testHttpClientWithResponseHandler(new HttpPut(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPUTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithResponseHandler(new HttpPut(SAY_HELLO_URL), SAY_HELLO, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPOST() throws IOException {
        testHttpClientWithResponseHandler(new HttpPost(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPOSTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithResponseHandler(new HttpPost(SAY_HELLO_URL), SAY_HELLO, false);
    }

    protected void testHttpClientWithResponseHandler(HttpUriRequest request, String data,
            boolean fault) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            request.addHeader("test-header", "test-value");
            if (fault) {
                request.addHeader("test-fault", "true");
            }

            if (data != null && request instanceof HttpEntityEnclosingRequest) {
                StringEntity entity = new StringEntity(data,
                        ContentType.create("text/plain", "UTF-8"));
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            }

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

}
