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
package org.hawkular.apm.tests.agent.opentracing.client.http;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

/**
 * @author gbrown
 */
public class JBossRESTEasyClientITest extends AbstractBaseHttpITest {

    private static final String SAY_HELLO_URL = "http://localhost:8180/sayHello";
    private static final String QUERY_STRING = "to=me";
    private static final String SAY_HELLO_URL_WITH_QS = SAY_HELLO_URL + "?" + QUERY_STRING;
    private static final String SAY_HELLO = "Say Hello";
    private static final String HELLO_WORLD = "Hello World";

    @Test
    public void testJaxRSClientGET() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, false, false, "GET", SAY_HELLO_URL);
    }

    @Test
    public void testJaxRSClientGETWithQS() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL_WITH_QS);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, false, true, "GET", SAY_HELLO_URL_WITH_QS);
    }

    @Test
    public void testJaxRSClientPOST() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").post(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false, "POST", SAY_HELLO_URL);
    }

    @Test
    public void testJaxRSClientPUT() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").put(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false, "PUT", SAY_HELLO_URL);
    }

    @Test
    public void testJaxRSClientGETWithFault() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-fault", "true").get();

        processResponse(response, true, false, "GET", SAY_HELLO_URL);
    }

    protected void processResponse(Response response, boolean fault, boolean qs, String method, String url) {
        String value = response.readEntity(String.class);
        response.close();

        if (!fault) {
            assertEquals(HELLO_WORLD, value);
        }

        Wait.until(() -> getTracer().finishedSpans().size() == 1);

        List<MockSpan> spans = getTracer().finishedSpans();
        assertEquals(1, spans.size());
        assertEquals(Tags.SPAN_KIND_CLIENT, spans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
        assertEquals(method, spans.get(0).operationName());
        assertEquals(url, spans.get(0).tags().get(Tags.HTTP_URL.getKey()));
        if (fault) {
            assertEquals("401", spans.get(0).tags().get(Tags.HTTP_STATUS.getKey()));
        }
    }

}
