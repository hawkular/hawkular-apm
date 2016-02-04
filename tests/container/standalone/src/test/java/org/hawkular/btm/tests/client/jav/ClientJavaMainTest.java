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
package org.hawkular.btm.tests.client.jav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.btxn.service.rest.client.BusinessTransactionServiceRESTClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ClientJavaMainTest {

    private static String baseUrl = System.getProperty("hawkular-btm.testapp.base-uri");
    private static String testBTxnServerBaseUrl = System.getProperty("hawkular-btm.base-uri");

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    @BeforeClass
    public static void waitForServer() {
        try {
            synchronized (baseUrl) {
                baseUrl.wait(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvokeTestOp() {
        long startTime = System.currentTimeMillis();

        try {
            String mesg = "hello";
            String num = "12";

            URL url = new URL(baseUrl + "/testOp?mesg=" + mesg + "&num=" + num);
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

            assertEquals("Failed to shutdown", 200, connection.getResponseCode());

            assertEquals(mesg + ":" + num, new String(b));

        } catch (Exception e) {
            fail("Failed to perform testOp: " + e);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        // Retrieve stored business transaction
        Criteria criteria = new Criteria();
        criteria.setStartTime(startTime);
        List<BusinessTransaction> result = service.query(null, criteria);

        assertNotNull(result);
        assertEquals("Only expecting 1 business txn", 1, result.size());

        BusinessTransaction btxn = result.get(0);

        // Should be one top level Component node with another single Component node contained
        assertEquals("Expecting single top level node", 1, btxn.getNodes().size());

        assertEquals("Expecting top node to be Component", Component.class, btxn.getNodes().get(0).getClass());
        assertEquals("Top level node operation incorrect", "testOp",
                ((Component) btxn.getNodes().get(0)).getOperation());
        assertEquals("Top level node service type incorrect", "TopLevelService",
                ((Component) btxn.getNodes().get(0)).getUri());

        assertEquals("Expecting single child node", 1, ((Component) btxn.getNodes().get(0)).getNodes().size());

        assertEquals("Expecting single child node to be Service", Component.class,
                ((Component) btxn.getNodes().get(0)).getNodes().get(0).getClass());
        assertEquals("Inner node operation incorrect", "join",
                ((Component) ((Component) btxn.getNodes().get(0)).getNodes().get(0)).getOperation());
        assertEquals("Inner node service type incorrect", "InnerService",
                ((Component) ((Component) btxn.getNodes().get(0)).getNodes().get(0)).getUri());
    }

    @AfterClass
    public static void shutdown() {
        // Shutdown test standalone app
        try {
            URL url = new URL(baseUrl + "/shutdown");
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

            assertEquals("Failed to shutdown", 200, connection.getResponseCode());

        } catch (Exception e) {
            fail("Failed to shutdown: " + e);
        }

        // Shutdown Test BTxn Service
        try {
            URL url = new URL(testBTxnServerBaseUrl + "/shutdown");
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

            assertEquals("Failed to shutdown", 200, connection.getResponseCode());

        } catch (Exception e) {
            fail("Failed to shutdown: " + e);
        }
    }
}
