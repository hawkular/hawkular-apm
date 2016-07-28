/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.ClientTestBase;
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
public class JBossRESTEasyClientTest extends ClientTestBase {

    /**  */
    private static final String SAY_HELLO_URL = "http://localhost:8180/sayHello";

    /**  */
    private static final String QUERY_STRING = "to=me";

    /**  */
    private static final String SAY_HELLO_URL_WITH_QS = SAY_HELLO_URL + "?" + QUERY_STRING;

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
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send(HELLO_WORLD);
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
    public void testJaxRSClientGET() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, true, false, false);
    }

    @Test
    public void testJaxRSClientGETWithQS() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL_WITH_QS);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, true, false, true);
    }

    @Test
    public void testJaxRSClientGETWithData() {
        setProcessContent(true);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, true, false, false);
    }

    @Test
    public void testJaxRSClientPOST() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").post(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false, false);
    }

    @Test
    public void testJaxRSClientPOSTWithData() {
        setProcessContent(true);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").post(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false, false);
    }

    @Test
    public void testJaxRSClientPUT() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").put(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false, false);
    }

    @Test
    public void testJaxRSClientPUTWithData() {
        setProcessContent(true);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").put(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false, false);
    }

    @Test
    public void testJaxRSClientGETWithFault() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-fault", "true").get();

        processResponse(response, true, true, false);
    }

    protected void processResponse(Response response, boolean get, boolean fault, boolean qs) {
        String value = response.readEntity(String.class);
        response.close();

        if (!fault) {
            assertEquals(HELLO_WORLD, value);
        }

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        for (Trace trace : getApmMockServer().getTraces()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = new ArrayList<Producer>();
        NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        String path=URI.create(SAY_HELLO_URL).getPath();

        assertEquals(path, testProducer.getUri());

        if (qs) {
            assertEquals(QUERY_STRING, testProducer.getDetails().get("http_query"));
        }

        // Check headers
        assertFalse("testProducer has no headers", testProducer.getIn().getHeaders().isEmpty());

        if (fault) {
            assertEquals("401", testProducer.getFault());

            // TODO: Awaitin fix for HWKBTM-151
            //assertEquals("Unauthorized", testProducer.getFaultDescription());
        } else {

            if (isProcessContent()) {
                // Check request value
                if (!get) {
                    assertTrue(testProducer.getIn().getContent().containsKey("all"));
                    assertEquals(SAY_HELLO, testProducer.getIn().getContent().get("all").getValue());
                }
                // Check response value
                assertTrue(testProducer.getOut().getContent().containsKey("all"));
                assertEquals(HELLO_WORLD, testProducer.getOut().getContent().get("all").getValue());
            } else {
                assertFalse(testProducer.getIn().getContent().containsKey("all"));
            }
        }
    }

}
