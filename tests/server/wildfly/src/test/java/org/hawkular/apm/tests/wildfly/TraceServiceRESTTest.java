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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.tests.common.Wait;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

    private static TracePublisherRESTClient publisher;

    @BeforeClass
    public static void initClass() {
        service = new TraceServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        publisher = new TracePublisherRESTClient();
        publisher.setUsername(TEST_USERNAME);
        publisher.setPassword(TEST_PASSWORD);
    }

    @Before
    public void initTest() {
        service.clear(null);
    }

    @Test
    public void testStoreAndRetrieveFragmentById() {
        Trace trace1 = new Trace();
        trace1.setId("1");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Wait.until(() -> service.getFragment(null, "1") != null);

        // Retrieve stored trace
        Trace result = service.getFragment(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void testStoreAndRetrieveSimpleTraceById() {
        Trace trace1 = new Trace();
        trace1.setId("1");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Wait.until(() -> service.getTrace(null, "1") != null);

        // Retrieve stored trace
        Trace result = service.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void testStoreAndRetrieveComplexTraceById() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis());
        trace1.getProperties().add(new Property("prop1","value1"));
        Consumer c1 = new Consumer();
        c1.setUri("uri1");
        trace1.getNodes().add(c1);
        Producer p1_1 = new Producer();
        p1_1.addInteractionId("id1_1");
        c1.getNodes().add(p1_1);
        Producer p1_2 = new Producer();
        p1_2.addInteractionId("id1_2");
        p1_2.setUri("uri2");
        c1.getNodes().add(p1_2);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setStartTime(System.currentTimeMillis());
        trace2.getProperties().add(new Property("prop1","value1"));
        trace2.getProperties().add(new Property("prop2","value2"));
        Consumer c2 = new Consumer();
        c2.setUri("uri2");
        c2.addInteractionId("id1_2");
        trace2.getNodes().add(c2);
        Producer p2_1 = new Producer();
        p2_1.addInteractionId("id2_1");
        c2.getNodes().add(p2_1);
        Producer p2_2 = new Producer();
        p2_2.addInteractionId("id2_2");
        c2.getNodes().add(p2_2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        Wait.until(() -> {
            try {
                // see https://issues.jboss.org/browse/HWKAPM-584
                Trace t = service.getTrace(null, "1");
                return t != null;
            } catch (Throwable t) {
                return false;
            }
        });

        // Retrieve stored trace
        Trace result = service.getTrace(null, "1");

        assertNotNull(result);
        assertEquals("1", result.getId());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            System.out.println("TRACE=" + mapper.writeValueAsString(result));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        assertEquals(2, result.getProperties().size());
        assertEquals(1, result.getNodes().size());
        assertEquals(Consumer.class, result.getNodes().get(0).getClass());
        assertEquals("uri1", result.getNodes().get(0).getUri());
        assertEquals(2, ((Consumer)result.getNodes().get(0)).getNodes().size());
        assertEquals(Producer.class, ((Consumer)result.getNodes().get(0)).getNodes().get(0).getClass());
        assertTrue(((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(0)).getNodes().isEmpty());
        assertEquals(Producer.class, ((Consumer)result.getNodes().get(0)).getNodes().get(1).getClass());
        assertEquals("uri2", ((Consumer)result.getNodes().get(0)).getNodes().get(1).getUri());
        assertEquals(1, ((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(1)).getNodes().size());
        assertEquals(Consumer.class, ((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(1)).getNodes()
                .get(0).getClass());
        assertEquals("uri2", ((Producer)((Consumer)result.getNodes().get(0)).getNodes().get(1)).getNodes()
                .get(0).getUri());
    }

    @Test
    public void testStoreAndQueryAll() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        Wait.until(() -> service.searchFragments(null, new Criteria()).size() == 1);

        // Query stored trace
        List<Trace> result = service.searchFragments(null, new Criteria());

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);

        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);
        List<Trace> result = service.searchFragments(null, criteria);

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);

        criteria.setStartTime(1100);
        List<Trace> result = service.searchFragments(null, criteria);

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(2000);
        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);

        List<Trace> result = service.searchFragments(null, criteria);

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.setEndTime(1500);
        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);

        criteria.setEndTime(1100);
        List<Trace> result = service.searchFragments(null, criteria);

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", null);
        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);

        List<Trace> result = service.searchFragments(null, criteria);

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteriaToWait = new Criteria();
        criteriaToWait.addProperty("hello", "world", null);
        Wait.until(() -> service.searchFragments(null, criteriaToWait).size() > 0, 1, TimeUnit.SECONDS);

        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "fred", null);
        List<Trace> result = service.searchFragments(null, criteria);

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
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Criteria criteriaToWait = new Criteria();
        criteriaToWait.addProperty("hello", "world", null);
        Wait.until(() -> service.searchFragments(null, criteriaToWait).size() == 1);

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", Operator.HASNOT);

        List<Trace> result = service.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testStoreAndQueryCorrelationsInclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Interaction);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "myid"));
        Wait.until(() -> service.searchFragments(null, criteria).size() == 1);

        List<Trace> result = service.searchFragments(null, criteria);

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());
    }

    @Test
    public void testStoreAndQueryCorrelationsExclude() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Interaction);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Criteria criteriaToWait = new Criteria();
        criteriaToWait.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "myid"));
        Wait.until(() -> service.searchFragments(null, criteriaToWait).size() == 1);

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "notmyid"));

        List<Trace> result = service.searchFragments(null, criteria);

        assertEquals(0, result.size());
    }

    @Test
    public void testSearchPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("hello", "world"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        Wait.until(() -> service.getTrace(null, "1") != null);

        // Query stored trace
        Criteria criteria = new Criteria();
        criteria.addProperty("hello", "world", null);

        List<Trace> result = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/traces/fragments/search");
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
