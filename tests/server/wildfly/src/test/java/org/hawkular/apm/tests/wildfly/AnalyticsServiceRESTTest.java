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
import java.util.Collection;
import java.util.List;

import org.hawkular.apm.analytics.service.rest.client.AnalyticsServiceRESTClient;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.PropertyType;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.EndpointInfo;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.PrincipalInfo;
import org.hawkular.apm.api.model.analytics.PropertyInfo;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;
import org.hawkular.apm.trace.service.rest.client.TraceServiceRESTClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class AnalyticsServiceRESTTest {

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    private static final TypeReference<java.util.List<CompletionTimeseriesStatistics>> COMPLETION_STATISTICS_LIST =
            new TypeReference<java.util.List<CompletionTimeseriesStatistics>>() {
    };

    private static final TypeReference<java.util.List<NodeTimeseriesStatistics>> NODE_TIMESERIES_STATISTICS_LIST =
            new TypeReference<java.util.List<NodeTimeseriesStatistics>>() {
    };

    private static final TypeReference<java.util.List<NodeSummaryStatistics>> NODE_SUMMARY_STATISTICS_LIST =
            new TypeReference<java.util.List<NodeSummaryStatistics>>() {
    };

    private static final TypeReference<java.util.List<CommunicationSummaryStatistics>> COMMS_SUMMARY_STATISTICS_LIST =
            new TypeReference<java.util.List<CommunicationSummaryStatistics>>() {
    };

    private static final TypeReference<java.util.List<String>> STRING_LIST =
            new TypeReference<java.util.List<String>>() {
    };

    private static final TypeReference<java.util.List<Cardinality>> CARDINALITY_LIST =
            new TypeReference<java.util.List<Cardinality>>() {
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    private static AnalyticsServiceRESTClient analytics;

    private static TraceServiceRESTClient service;

    private static TracePublisherRESTClient publisher;

    @BeforeClass
    public static void initClass() {
        analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        service = new TraceServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        publisher = new TracePublisherRESTClient();
        publisher.setUsername(TEST_USERNAME);
        publisher.setPassword(TEST_PASSWORD);
    }

    @Before
    public void initTest() {
        analytics.clear(null);
        service.clear(null);
    }

    @Test
    public void testGetUnboundEndpoints() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored trace
        List<EndpointInfo> endpoints = analytics.getUnboundEndpoints(null, 0, 0, true);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertEquals("testuri", endpoints.get(0).getEndpoint());
    }

    @Test
    public void testGetBoundEndpoints() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored trace Endpoints
        List<EndpointInfo> endpoints = analytics.getBoundEndpoints(null, "trace1", 0, 0);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains(new EndpointInfo("testuri")));
    }

    @Test
    public void testGetPropertyInfo() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("prop1", "value1"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria=new Criteria()
            .setBusinessTransaction("trace1")
            .setStartTime(0)
            .setEndTime(0);

        List<PropertyInfo> pis = analytics.getPropertyInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
    }

    @Test
    public void testGetPrincipalInfo() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setPrincipal("p1");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria=new Criteria()
            .setBusinessTransaction("trace1")
            .setStartTime(0)
            .setEndTime(0);

        List<PrincipalInfo> pis = analytics.getPrincipalInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getId().equals("p1"));
        assertEquals(1, pis.get(0).getCount());
    }

    @Test
    public void testGetCompletionCount() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        Long count = analytics.getTraceCompletionCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionCountWithPropertyFilter() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        trace1.getNodes().add(c1);
        trace1.getProperties().add(new Property("prop1", "2.5", PropertyType.Number));
        trace1.getProperties().add(new Property("prop2", "hello"));

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        assertEquals(1, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction("testapp")
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "1", Operator.GT)));

        assertEquals(1, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction("testapp")
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "3", Operator.LT)));

        assertEquals(0, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction("testapp")
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop1", "2.4", Operator.LT)));

        assertEquals(1, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction("testapp")
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HAS)));

        assertEquals(0, analytics.getTraceCompletionCount(null, new Criteria()
                .setBusinessTransaction("testapp")
                .setStartTime(0)
                .setEndTime(0)
                .addProperty("prop2", "hello", Operator.HASNOT)));
    }

    @Test
    public void testGetCompletionFaultCount() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setFault("Failed");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        Long count = analytics.getTraceCompletionFaultCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetCompletionTimeseriesStatisticsPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/trace/completion/statistics?interval=1000");
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

            os.write(mapper.writeValueAsBytes(new Criteria().setBusinessTransaction("testapp")));

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
                stats = mapper.readValue(builder.toString(), COMPLETION_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetCompletionPropertyDetails() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("prop1", "value1"));

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getTraceCompletionPropertyDetails(null, criteria, "prop1");

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionPropertyDetailsPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.getProperties().add(new Property("prop1", "value1"));

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        List<Cardinality> cards = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/trace/completion/property/prop1");
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

            os.write(mapper.writeValueAsBytes(new Criteria().setBusinessTransaction("testapp")));

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
                cards = mapper.readValue(builder.toString(), CARDINALITY_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send property details request: " + e);
        }

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionFaultDetails() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setFault("fault1");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getTraceCompletionFaultDetails(null, criteria);

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionFaultDetailsPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setFault("fault1");
        trace1.getNodes().add(c1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        List<Cardinality> cards = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/trace/completion/faults");
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

            os.write(mapper.writeValueAsBytes(new Criteria().setBusinessTransaction("testapp")));

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
                cards = mapper.readValue(builder.toString(), CARDINALITY_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send fault details request: " + e);
        }

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetNodeTimeseriesStatistics() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/node/statistics?interval=1000");
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

            os.write(mapper.writeValueAsBytes(new Criteria()));

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
                stats = mapper.readValue(builder.toString(), NODE_TIMESERIES_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send node timeseries statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsHostName() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        criteria = new Criteria();
        criteria.setHostName("hostB").setStartTime(0).setEndTime(0);

        // Get transaction count
        stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsPOSTHostName() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/node/statistics?interval=1000");
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

            os.write(mapper.writeValueAsBytes(new Criteria().setHostName("hostA")));

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
                stats = mapper.readValue(builder.toString(), NODE_TIMESERIES_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send node timeseries statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(1, stats.size());

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/node/statistics?interval=1000");
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

            os.write(mapper.writeValueAsBytes(new Criteria().setHostName("hostB")));

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

            assertEquals(200, connection.getResponseCode());

            stats = mapper.readValue(builder.toString(), NODE_TIMESERIES_STATISTICS_LIST);

        } catch (Exception e) {
            fail("Failed to send node timeseries statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatistics() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/node/summary");
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

            os.write(mapper.writeValueAsBytes(new Criteria()));

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
                stats = mapper.readValue(builder.toString(), NODE_SUMMARY_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send node summary statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(2, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsHostName() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        Collection<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        criteria = new Criteria();
        criteria.setHostName("hostB").setStartTime(0).setEndTime(0);

        stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsPOSTHostName() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setEndpointType("HTTP");
        trace1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/node/summary");
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

            Criteria criteria = new Criteria();
            criteria.setHostName("hostA");

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
                stats = mapper.readValue(builder.toString(), NODE_SUMMARY_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send node summary statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(2, stats.size());

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/node/summary");
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

            Criteria criteria = new Criteria();
            criteria.setHostName("hostB");

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
                stats = mapper.readValue(builder.toString(), NODE_SUMMARY_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send node summary statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsFlat() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setDuration(1000000);
        p1.addInteractionId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction("testapp");
        trace2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setDuration(500000);
        c2.addInteractionId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                                    criteria, false);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            System.out.println("COMMS STATS=" + mapper.writeValueAsString(stats));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CommunicationSummaryStatistics first = null;
        CommunicationSummaryStatistics second = null;

        for (CommunicationSummaryStatistics css : stats) {
            if (css.getId().equals("originuri")) {
                first = css;
            } else if (css.getId().equals("testuri")) {
                second = css;
            } else {
                fail("Unknown uri: " + css.getId());
            }
        }

        assertNotNull(first);
        assertNotNull(second);

        assertEquals(1, first.getOutbound().size());
        assertEquals(0, second.getOutbound().size());

        assertEquals(first.getOutbound().keySet().iterator().next(), second.getId());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsTree() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setDuration(1000000);
        p1.addInteractionId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction("testapp");
        trace2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setDuration(500000);
        c2.addInteractionId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null,
                                    criteria, true);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            System.out.println("COMMS STATS=" + mapper.writeValueAsString(stats));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CommunicationSummaryStatistics first = stats.iterator().next();

        assertNotNull(first);

        assertEquals(1, first.getOutbound().size());

        assertTrue(first.getOutbound().containsKey("testuri"));
        assertNotNull(first.getOutbound().get("testuri").getNode());

        CommunicationSummaryStatistics second = first.getOutbound().get("testuri").getNode();

        assertEquals(0, second.getOutbound().size());

        assertEquals(first.getOutbound().keySet().iterator().next(), second.getId());
        assertEquals("originuri", first.getId());
        assertEquals("testuri", second.getId());
    }

    @Test
    public void testGetCommunicationSummaryStatisticsPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setEndpointType("endpoint");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setEndpointType("endpoint");
        p1.setDuration(1000000);
        p1.addInteractionId("interaction1");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction("testapp");
        trace2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setEndpointType("endpoint");
        c2.setDuration(500000);
        c2.addInteractionId("interaction1");
        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        // Get transaction count
        List<CommunicationSummaryStatistics> stats = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/communication/summary");
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

            os.write(mapper.writeValueAsBytes(new Criteria()));

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
                stats = mapper.readValue(builder.toString(), COMMS_SUMMARY_STATISTICS_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send node summary statistics request: " + e);
        }

        assertNotNull(stats);
        assertEquals(2, stats.size());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            System.out.println("COMMS STATS=" + mapper.writeValueAsString(stats));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CommunicationSummaryStatistics first = null;
        CommunicationSummaryStatistics second = null;

        for (CommunicationSummaryStatistics css : stats) {
            if (css.getId().equals("originuri")) {
                first = css;
            } else if (css.getId().equals("testuri")) {
                second = css;
            } else {
                fail("Unknown uri: " + css.getId());
            }
        }

        assertNotNull(first);
        assertNotNull(second);

        assertEquals(1, first.getOutbound().size());
        assertEquals(0, second.getOutbound().size());

        assertEquals(first.getOutbound().keySet().iterator().next(), second.getId());
    }

    @Test
    public void testGetHostNames() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        List<String> hosts = analytics.getHostNames(null, criteria);

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("hostA", hosts.get(0));
    }

    @Test
    public void testGetHostNamesPOST() {
        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        trace1.setHostName("hostA");

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        // Get transaction count
        List<String> hosts = null;

        try {
            URL url = new URL(service.getUri() + "hawkular/apm/analytics/hostnames");
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

            Criteria criteria = new Criteria();

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
                hosts = mapper.readValue(builder.toString(), STRING_LIST);
            }
        } catch (Exception e) {
            fail("Failed to send host names request: " + e);
        }

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("hostA", hosts.get(0));
    }

    @Test
    public void testGetCompletionTimeMultiFragment() {
        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        long baseTime=System.currentTimeMillis() - 4000;

        Trace trace1 = new Trace();
        trace1.setId("1");
        trace1.setBusinessTransaction("testapp");
        trace1.setStartTime(baseTime); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri2");
        c1.setDuration(1000000000);
        c1.setBaseTime(1);

        Producer p1 = new Producer();
        p1.setUri("testuri2");
        p1.setDuration(1000000000);
        c1.setBaseTime(1);
        p1.addInteractionId("interaction2");
        c1.getNodes().add(p1);

        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("2");
        trace2.setBusinessTransaction("testapp");
        trace2.setStartTime(baseTime + 1000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(500000000);
        c2.setBaseTime(1);
        c2.addInteractionId("interaction2");

        Component comp2 = new Component();
        comp2.setDuration(1500000000);
        comp2.setBaseTime(1);
        c2.getNodes().add(comp2);

        trace2.getNodes().add(c2);

        List<Trace> traces = new ArrayList<Trace>();
        traces.add(trace1);
        traces.add(trace2);

        try {
            publisher.publish(null, traces);
        } catch (Exception e1) {
            fail("Failed to store: " + e1);
        }

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        assertEquals(1000, trace1.calculateDuration());
        assertEquals(1500, trace2.calculateDuration());

        // Query stored trace
        List<Trace> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = analytics.getTraceCompletionTimeseriesStatistics(null, criteria, 10000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1750, stats.get(0).getAverage());
        assertEquals(1, stats.get(0).getCount());
    }

}
