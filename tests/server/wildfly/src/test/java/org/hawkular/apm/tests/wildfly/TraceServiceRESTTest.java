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
package org.hawkular.apm.tests.wildfly;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
public class TraceServiceRESTTest {

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    private static final TypeReference<java.util.List<Trace>> TRACE_LIST =
            new TypeReference<java.util.List<Trace>>() {
            };

    private static TraceServiceRESTClient service;

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void initClass() {
        service = new TraceServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);
    }

    @Before
    public void initTest() {
        service.clear(null);
    }

    @Test
    public void testStoreAndRetrieveById() {
        Trace trace1 = new Trace();
        trace1.setId("1");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Retrieve stored trace
        Trace result = service.get(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void testStoreAndQueryAll() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryStartTimeInclude() {
        Trace trace1 = new Trace();
        trace1.setStartTime(1000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);

        List<Trace> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryStartTimeExclude() {
        Trace trace1 = new Trace();
        trace1.setStartTime(1000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(1100);

        List<Trace> result = service.query(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryEndTimeInclude() {
        Trace trace1 = new Trace();
        trace1.setStartTime(1000);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(2000);

        List<Trace> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryEndTimeExclude() {
        Trace trace1 = new Trace();
        trace1.setStartTime(1200);
        trace1.setId("1");

        Consumer c1 = new Consumer();
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(1100);

        List<Trace> result = service.query(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryPropertiesInclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("hello", "world"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", false);

        List<Trace> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryPropertiesNotFound() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("hello", "world"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "fred", false);

        List<Trace> result = service.query(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryPropertiesExclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("hello", "world"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", true);

        List<Trace> result = service.query(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryCorrelationsInclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "myid"));

        List<Trace> result = service.query(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryCorrelationsExclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "notmyid"));

        List<Trace> result = service.query(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testQueryPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("hello", "world"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            service.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", false);

        List<Trace> result = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/fragments/query");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Content-Type",
                    "application/json");

            String authString = TEST_USERNAME + ":" + TEST_PASSWORD;
            String encoded = Base64.getEncoder().encodeToString(authString.getBytes());

            String authorization = "Basic " + encoded;

            connection.setRequestProperty("Authorization", authorization);

            java.io.OutputStream os = connection.getOutputStream();

            os.write(mapper.writeValueAsBytes(criteria));

            os.flush();
            os.close();

            java.io.InputStream is = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            StringBuilder builder = new StringBuilder();
            String str = null;

            while ((str = reader.readLine()) != null) {
                builder.append(str);
            }

            is.close();

            if (connection.getResponseCode() == 200) {
                result = mapper.readValue(builder.toString(), TRACE_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send 'query' trace request: " + e);
        }

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

    }
}
