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
package org.hawkular.apm.tests.client.jav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ClientJavaMainITest {

    private static String baseUrl = System.getProperty("hawkular-apm.testapp.uri");
    private static String testAPMServerUri = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI);

    private static final String TEST_PASSWORD = "password";
    private static final String TEST_USERNAME = "jdoe";

    @BeforeClass
    public static void waitForServer() throws MalformedURLException {
        Wait.until(() -> {
            URL url = new URL(baseUrl);
            URLConnection connection = url.openConnection();
            try {
                connection.connect();
            } catch (ConnectException ignored) {
                return false;
            }
            return true;
        });
    }

    @Test
    public void testInvokeTestOp() throws IOException {
        long startTime = System.currentTimeMillis();

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

        TraceServiceRESTClient service = new TraceServiceRESTClient(TEST_USERNAME, TEST_PASSWORD, testAPMServerUri);

        // Retrieve stored traces
        Criteria criteria = new Criteria();
        criteria.setStartTime(startTime);

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);

        List<Trace> result = service.searchFragments(null, criteria);

        assertNotNull(result);
        assertEquals("Only expecting 1 trace", 1, result.size());

        Trace trace = result.get(0);

        assertNotNull(trace.getTraceId());

        // Should be one top level Component node with another single Component node contained
        assertEquals("Expecting single top level node", 1, trace.getNodes().size());

        assertEquals("Expecting top node to be Component", Component.class, trace.getNodes().get(0).getClass());
        assertEquals("Top level node operation incorrect", "testOp",
                ((Component) trace.getNodes().get(0)).getOperation());
        assertEquals("Top level node service type incorrect", "TopLevelService",
                ((Component) trace.getNodes().get(0)).getUri());

        assertEquals("Expecting single child node", 1, ((Component) trace.getNodes().get(0)).getNodes().size());

        assertEquals("Expecting single child node to be Service", Component.class,
                ((Component) trace.getNodes().get(0)).getNodes().get(0).getClass());
        assertEquals("Inner node operation incorrect", "join",
                ((Component) ((Component) trace.getNodes().get(0)).getNodes().get(0)).getOperation());
        assertEquals("Inner node service type incorrect", "InnerService",
                ((Component) ((Component) trace.getNodes().get(0)).getNodes().get(0)).getUri());
    }

    @AfterClass
    public static void shutdown() throws IOException {
        // Shutdown test standalone app
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

        // Shutdown Test BTxn Service
        url = new URL(testAPMServerUri + "/hawkular/apm/shutdown");
        connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type",
                "application/json");

        is = connection.getInputStream();

        b = new byte[is.available()];

        is.read(b);

        is.close();

        assertEquals("Failed to shutdown", 200, connection.getResponseCode());
    }
}
