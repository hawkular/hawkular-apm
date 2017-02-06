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

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

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

        processResponse(response, false, false);
    }

    @Test
    public void testJaxRSClientGETWithQS() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL_WITH_QS);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, false, true);
    }

    @Test
    public void testJaxRSClientGETWithData() {
        setProcessContent(true);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").get();

        processResponse(response, false, false);
    }

    @Test
    public void testJaxRSClientPOST() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").post(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false);
    }

    @Test
    public void testJaxRSClientPOSTWithData() {
        setProcessContent(true);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").post(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false);
    }

    @Test
    public void testJaxRSClientPUT() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").put(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false);
    }

    @Test
    public void testJaxRSClientPUTWithData() {
        setProcessContent(true);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-header", "test-value").put(Entity.<String> text(SAY_HELLO));

        processResponse(response, false, false);
    }

    @Test
    public void testJaxRSClientGETWithFault() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(SAY_HELLO_URL);
        Response response = target.request().header("test-fault", "true").get();

        processResponse(response, true, false);
    }

    protected void processResponse(Response response, boolean fault, boolean qs) {
        String value = response.readEntity(String.class);
        response.close();

        if (!fault) {
            assertEquals(HELLO_WORLD, value);
        }

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        String path=URI.create(SAY_HELLO_URL).getPath();

        assertEquals(path, testProducer.getUri());

        if (qs) {
            assertEquals(QUERY_STRING, testProducer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
        }

        if (fault) {
            assertEquals("401", testProducer.getProperties(Tags.HTTP_STATUS.getKey())
                    .iterator().next().getValue());
        }
    }

}
