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
package org.hawkular.apm.tests.agent.opentracing.server.http;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;

import org.hawkular.apm.tests.agent.opentracing.common.OpenTracingAgentTestBase;
import org.hawkular.apm.tests.common.Wait;

import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

/**
 * @author gbrown
 */
public class JavaxServletTestBase extends OpenTracingAgentTestBase {

    private static final String TEST_USER = "jdoe";
    private static final String TEST_HEADER = "test-header";
    private static final String HELLO_WORLD_RESPONSE = "<h1>HELLO WORLD</h1>";

    protected void testJettyServlet(String method, String urlstr, String reqdata, boolean fault,
            boolean respexpected) throws IOException {
        URL url = new URL(urlstr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type",
                "application/json");

        connection.setRequestProperty(TEST_HEADER, "test-value");
        if (fault) {
            connection.setRequestProperty("test-fault", "true");
        }
        if (!respexpected) {
            connection.setRequestProperty("test-no-data", "true");
        }

        String authString = TEST_USER + ":" + "password";
        String encoded = Base64.getEncoder().encodeToString(authString.getBytes());

        connection.setRequestProperty("Authorization", "Basic " + encoded);

        InputStream is = null;

        if (reqdata != null) {
            OutputStream os = connection.getOutputStream();

            os.write(reqdata.getBytes());

            os.flush();
            os.close();
        } else if (fault || !respexpected) {
            connection.connect();
        } else if (respexpected) {
            is = connection.getInputStream();
        }

        if (!fault) {
            assertEquals("Unexpected response code", 200, connection.getResponseCode());

            if (respexpected) {
                if (is == null) {
                    is = connection.getInputStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder builder = new StringBuilder();
                String str = null;

                while ((str = reader.readLine()) != null) {
                    builder.append(str);
                }

                is.close();

                assertEquals(HELLO_WORLD_RESPONSE, builder.toString());
            }
        } else {
            assertEquals("Unexpected fault response code", 401, connection.getResponseCode());
        }

        Wait.until(() -> getTracer().finishedSpans().size() == 2);

        List<MockSpan> spans = getTracer().finishedSpans();
        assertEquals(2, spans.size());

        MockSpan serverSpan = spans.stream()
                .filter(s -> s.tags().get(Tags.SPAN_KIND.getKey()).equals(Tags.SPAN_KIND_SERVER))
                .findFirst().get();
        assertEquals(method, serverSpan.operationName());
        assertEquals(toHttpURL(url), serverSpan.tags().get(Tags.HTTP_URL.getKey()));
        assertEquals(fault ? "401" : "200", serverSpan.tags().get(Tags.HTTP_STATUS.getKey()));
    }
}
