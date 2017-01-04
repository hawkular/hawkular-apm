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
package org.hawkular.apm.api.internal.actions.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author gbrown
 */
public class JSONTest {

    @Test
    public void testEvaluateStringToField() {
        String json = "{ \"orderId\": \"1\" }";
        String expr = "$.orderId";

        String result = JSON.evaluate(expr, json);

        assertNotNull(result);
        assertEquals("1", result);
    }

    @Test
    public void testEvaluateStringToElement() {
        String json = "{ \"order\": { \"orderId\": \"1\" } }";
        String expr = "$.order";

        String result = JSON.evaluate(expr, json);

        assertNotNull(result);
        assertEquals("{\"orderId\":\"1\"}", result);
    }

    @Test
    public void testEvaluateObjectToField() {
        Order order = new Order();
        order.setOrderId("1");
        String expr = "$.orderId";

        String result = JSON.evaluate(expr, order);

        assertNotNull(result);
        assertEquals("1", result);
    }

    @Test
    public void testEvaluateNavigation() {
        String show = "The Lion King";
        String json = "{ \"performance\": { \"name\": \"" + show + "\" }}";
        String expression = "$.performance.name";

        String result = JSON.evaluate(expression, json.getBytes());

        assertNotNull(result);
        assertEquals(show, result);
    }

    @Test
    public void testEvaluateFunctionLength() {
        String expected = "2";
        String json = "{\"id\":1,\"tickets\":[{\"id\":1,\"seat\":{\"rowNumber\":1,\"number\":2,"
                + "\"section\":{\"id\":1,\"name\":\"A\",\"description\":\"Premier platinum reserve\","
                + "\"numberOfRows\":20,\"rowCapacity\":100,\"capacity\":2000}},\"ticketCategory\":"
                + "{\"id\":1,\"description\":\"Adult\"},\"price\":219.5},{\"id\":2,\"seat\":"
                + "{\"rowNumber\":1,\"number\":1,\"section\":{\"id\":1,\"name\":\"A\",\"description\":"
                + "\"Premier platinum reserve\",\"numberOfRows\":20,\"rowCapacity\":100,\"capacity\":2000}},"
                + "\"ticketCategory\":{\"id\":1,\"description\":\"Adult\"},\"price\":219.5}],\"performance\":"
                + "{\"id\":1,\"date\":1442858400000},\"cancellationCode\":\"abc\",\"createdOn\":1463756307069,"
                + "\"contactEmail\":\"gbrown@redhat.com\",\"totalTicketPrice\":439.0}";
        String expression = "$.tickets.length()";

        String result = JSON.evaluate(expression, json.getBytes());

        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    public void testPredicateString() {
        String json = "{ \"enabled\": \"true\" }";
        String expr = "$.enabled";

        boolean result = JSON.predicate(expr, json);

        assertTrue(result);
    }

    @Test
    public void testPredicateBoolean() {
        String json = "{ \"enabled\": true }";
        String expr = "$.enabled";

        boolean result = JSON.predicate(expr, json);

        assertTrue(result);
    }

    @Test
    public void testPredicateExpression() {
        String json = "{ \"orders\": [{ \"orderId\": \"1\" }] }";
        String expr = "$.orders[?(@.orderId)]";

        boolean result = JSON.predicate(expr, json);

        assertTrue(result);
    }

    @Test
    public void testPredicateObject() {
        Order order = new Order();
        order.setEnabled(true);
        String expr = "$.enabled";

        boolean result = JSON.predicate(expr, order);

        assertTrue(result);
    }

    public static class Order {
        private String orderId;
        private boolean enabled;

        /**
         * @return the enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @param enabled the enabled to set
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * @return the orderId
         */
        public String getOrderId() {
            return orderId;
        }

        /**
         * @param orderId the orderId to set
         */
        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

    }
}
