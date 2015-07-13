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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Producer;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class ClientCamelRestletTest extends ClientCamelTestBase {

    /**  */
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

    @Test
    public void testCreateOrder() {
        try {
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

        } catch (Exception e) {
            fail("Failed to perform testOp: " + e);
        }

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions (including 1 for test client)
        assertEquals(6, getBtxnService().getBusinessTransactions().size());

        Consumer creditCheck = null;
        Consumer checkStock = null;
        Consumer createOrder = null;

        for (BusinessTransaction btxn : getBtxnService().getBusinessTransactions()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(btxn));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (!btxn.getNodes().isEmpty()
                    && btxn.getNodes().get(0).getClass() == Consumer.class) {
                Consumer consumer = (Consumer) btxn.getNodes().get(0);

                if (consumer.getUri().equals("http://localhost:8180/inventory/checkStock")) {
                    checkStock = consumer;
                } else if (consumer.getUri().equals("http://localhost:8180/creditagency/checkCredit")) {
                    creditCheck = consumer;
                } else if (consumer.getUri().equals("http://localhost:8180/orders/createOrder")) {
                    createOrder = consumer;
                }
            }
        }

        assertNotNull("creditCheck null", creditCheck);
        assertNotNull("checkStock null", checkStock);
        assertNotNull("createOrder null", createOrder);

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(createOrder.getNodes(), Producer.class, producers);

        assertEquals("Expecting 2 producers", 2, producers.size());

        Producer creditCheckProducer = null;
        Producer checkStockProducer = null;
        for (Producer p : producers) {
            if (p.getUri().equals("http://localhost:8180/inventory/checkStock")) {
                checkStockProducer = p;
            } else if (p.getUri().equals("http://localhost:8180/creditagency/checkCredit")) {
                creditCheckProducer = p;
            }
        }

        assertNotNull("creditCheckProducer null", creditCheckProducer);
        assertNotNull("checkStockProducer null", checkStockProducer);

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(creditCheckProducer, creditCheck);
        checkInteractionCorrelationIdentifiers(checkStockProducer, checkStock);
    }
}
