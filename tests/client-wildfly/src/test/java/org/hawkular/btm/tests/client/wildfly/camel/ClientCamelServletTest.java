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
package org.hawkular.btm.tests.client.wildfly.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.btxn.service.rest.client.BusinessTransactionServiceRESTClient;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * These tests invoke a Camel based REST service to cause business transaction information
 * to be reported to the BusinessTransactionService.
 *
 * @author gbrown
 */
public class ClientCamelServletTest {

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    @Test
    public void testInvokeCamelRESTService() {

        // Delay to avoid picking up previously reported txns
        try {
            synchronized (this) {
                wait(500);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        long startTime = System.currentTimeMillis();

        try {
            URL url = new URL(System.getProperty("hawkular.base-uri")
                    + "/camel-example-servlet-rest-tomcat/rest" + "/user/123");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            java.io.InputStream is = connection.getInputStream();

            byte[] b = new byte[is.available()];

            is.read(b);

            is.close();

            assertEquals(200, connection.getResponseCode());

            String user = new String(b);

            assertTrue("Response should contain user with name 'John Doe'", user.contains("John Doe"));
        } catch (Exception e) {
            fail("Failed to get user details: " + e);
        }

        // Need to wait for business transaction fragment to be reported to server
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Check if business transaction fragments have been reported
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria().setStartTime(startTime);

        List<BusinessTransaction> btxns = service.query(null, criteria);

        assertEquals(1, btxns.size());

        for (BusinessTransaction btxn : btxns) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(btxn));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        // Check top level node is a Consumer associated with the servlet
        assertEquals(Consumer.class, btxns.get(0).getNodes().get(0).getClass());
    }

}
