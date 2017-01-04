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
package org.hawkular.apm.tests.client.wildfly.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * These tests invoke a Camel based REST service to cause transaction information
 * to be reported to the TraceService.
 *
 * @author gbrown
 */
public class ClientCamelServletITest extends ClientTestBase {

    @Override
    public int getPort() {
        return 8180;
    }

    @Test
    public void testInvokeCamelRESTService() throws IOException {
        // Delay to avoid picking up previously reported txns
        Wait.until(() -> getApmMockServer().getTraces().size() == 0);

        URL url = new URL(System.getProperty("test.uri")
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

        // Need to wait for trace fragment to be reported to server
        Wait.until(() -> getApmMockServer().getTraces().size() == 1, 2, TimeUnit.SECONDS);

        // Check if trace fragments have been reported
        List<Trace> traces = getApmMockServer().getTraces();

        assertEquals(1, traces.size());
        assertNotNull(traces.get(0).getTraceId());

        // Check top level node is a Consumer associated with the servlet
        assertEquals(Consumer.class, traces.get(0).getNodes().get(0).getClass());
    }

}
