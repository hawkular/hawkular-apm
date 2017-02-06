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
package org.hawkular.apm.tests.client.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ClientCamelRestletITest extends ClientCamelITestBase {

    private static final String ORDER_CREATED = "Order created";

    @Override
    public RouteBuilder getRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration().component("restlet").host("localhost").
                        port(8180).bindingMode(RestBindingMode.auto);

                rest("/orders")
                        .get("/createOrder").to("direct:createOrder");

                rest("/inventory")
                        .get("/checkStock").to("seda:checkStock");

                rest("/creditagency")
                        .get("/checkCredit").to("vm:checkCredit");

                from("direct:createOrder")
                        .to("language:simple:" + URLEncoder.encode("Hello", "UTF-8"))
                        .to("restlet:http://localhost:8180/inventory/checkStock")
                        .choice()
                        .when(body().isEqualTo(true))
                        .to("direct:processOrder")
                        .otherwise()
                        .transform().constant("Order NOT created: Out of Stock");

                from("direct:processOrder")
                        .to("restlet:http://localhost:8180/creditagency/checkCredit")
                        .choice()
                        .when(body().isEqualTo(true))
                        .transform().constant(ORDER_CREATED).endChoice()
                        .otherwise()
                        .transform().constant("Order NOT created: Insufficient Credit");

                from("seda:checkStock")
                        .transform().constant(true);

                from("vm:checkCredit")
                        .transform().constant(true);
            }
        };
    }

    protected void placeOrder() throws IOException {
        URL url = new URL("http://localhost:8180/orders/createOrder");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type",
                "application/json");

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

        assertEquals(ORDER_CREATED, builder.toString());
    }

    @Test
    public void testCreateOrder() throws IOException {
        placeOrder();

        Wait.until(() -> getApmMockServer().getTraces().size() == 6);

        // Check stored traces (including 1 for test client)
        assertEquals(6, getApmMockServer().getTraces().size());

        Consumer creditCheck = null;
        Consumer checkStock = null;
        Consumer createOrder = null;

        for (Trace trace : getApmMockServer().getTraces()) {
            if (!trace.getNodes().isEmpty()
                    && trace.getNodes().get(0).getClass() == Consumer.class) {
                Consumer consumer = (Consumer) trace.getNodes().get(0);

                if (consumer.getUri().equals("/inventory/checkStock")) {
                    checkStock = consumer;
                } else if (consumer.getUri().equals("/creditagency/checkCredit")) {
                    creditCheck = consumer;
                } else if (consumer.getUri().equals("/orders/createOrder")) {
                    createOrder = consumer;
                }
            }
        }

        assertNotNull("creditCheck null", creditCheck);
        assertNotNull("checkStock null", checkStock);
        assertNotNull("createOrder null", createOrder);

        // Check if operation specified
        assertEquals("GET", creditCheck.getOperation());
        assertEquals("GET", checkStock.getOperation());
        assertEquals("GET", createOrder.getOperation());

        List<Producer> producers = NodeUtil.findNodes(createOrder.getNodes(), Producer.class);

        assertEquals("Expecting 2 producers", 2, producers.size());

        Producer creditCheckProducer = null;
        Producer checkStockProducer = null;
        for (Producer p : producers) {
            if (p.getUri().equals("/inventory/checkStock")) {
                checkStockProducer = p;
            } else if (p.getUri().equals("/creditagency/checkCredit")) {
                creditCheckProducer = p;
            }
        }

        assertNotNull("creditCheckProducer null", creditCheckProducer);
        assertNotNull("checkStockProducer null", checkStockProducer);

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(creditCheckProducer, creditCheck);
        checkInteractionCorrelationIdentifiers(checkStockProducer, checkStock);
    }

    @Test
    public void testTraceIdPropagated() throws IOException {
        placeOrder();

        Wait.until(() -> getApmMockServer().getTraces().size() == 6);

        // Check stored traces - one btxn represents the test sender
        assertEquals(6, getApmMockServer().getTraces().size());

        // Check only one trace id used for all trace fragments
        assertEquals(1, getApmMockServer().getTraces().stream().map(t -> {
            assertNotNull(t.getTraceId());
            return t.getTraceId();
        }).distinct().count());
    }
}
