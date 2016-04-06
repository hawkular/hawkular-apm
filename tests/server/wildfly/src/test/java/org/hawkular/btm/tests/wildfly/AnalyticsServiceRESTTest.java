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
package org.hawkular.btm.tests.wildfly;

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

import org.hawkular.btm.analytics.service.rest.client.AnalyticsServiceRESTClient;
import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.EndpointInfo;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.btxn.service.rest.client.BusinessTransactionServiceRESTClient;
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

    private static BusinessTransactionServiceRESTClient service;

    @BeforeClass
    public static void initClass() {
        analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);
    }

    @Before
    public void initTest() {
        analytics.clear(null);
        service.clear(null);
    }

    @Test
    public void testGetUnboundEndpoints() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored business transaction
        List<EndpointInfo> endpoints = analytics.getUnboundEndpoints(null, 0, 0, true);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertEquals("testuri", endpoints.get(0).getEndpoint());
    }

    @Test
    public void testGetBoundEndpoints() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("btxn1");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored business transaction Endpoints
        List<EndpointInfo> endpoints = analytics.getBoundEndpoints(null, "btxn1", 0, 0);

        assertNotNull(endpoints);
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains(new EndpointInfo("testuri")));
    }

    @Test
    public void testGetPropertyInfo() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("btxn1");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.getProperties().put("prop1", "value1");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria=new Criteria()
            .setBusinessTransaction("btxn1")
            .setStartTime(0)
            .setEndTime(0);

        List<PropertyInfo> pis = analytics.getPropertyInfo(null, criteria);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
    }

    @Test
    public void testGetCompletionCount() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        Long count = analytics.getCompletionCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionFaultCount() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setFault("Failed");
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        Long count = analytics.getCompletionFaultCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetCompletionTimeseriesStatisticsPOST() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/completion/statistics?interval=1000");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.getProperties().put("prop1", "value1");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getCompletionPropertyDetails(null, criteria, "prop1");

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionPropertyDetailsPOST() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.getProperties().put("prop1", "value1");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        List<Cardinality> cards = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/completion/property/prop1");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setFault("fault1");
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getCompletionFaultDetails(null, criteria);

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionFaultDetailsPOST() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        c1.setFault("fault1");
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        List<Cardinality> cards = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/completion/faults");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/node/statistics?interval=1000");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/node/statistics?interval=1000");
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
            URL url = new URL(service.getBaseUrl() + "analytics/node/statistics?interval=1000");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/node/summary");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1 = new Component();
        comp1.setComponentType("Database");
        comp1.setUri("jdbc:h2:hello");
        comp1.setOperation("query");
        comp1.setDuration(600000);
        c1.getNodes().add(comp1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/node/summary");
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
            URL url = new URL(service.getBaseUrl() + "analytics/node/summary");
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
    public void testGetCommunicationSummaryStatistics() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setDuration(1000000);
        p1.addInteractionId("interaction1");
        c1.getNodes().add(p1);

        btxn1.getNodes().add(c1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("2");
        btxn2.setName("testapp");
        btxn2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setDuration(500000);
        c2.addInteractionId("interaction1");
        btxn2.getNodes().add(c2);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);
        btxns.add(btxn2);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setStartTime(0).setEndTime(0);

        Collection<CommunicationSummaryStatistics> stats = analytics.getCommunicationSummaryStatistics(null, criteria);

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
    public void testGetCommunicationSummaryStatisticsPOST() {
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("originuri");
        c1.setDuration(1200000);

        Producer p1 = new Producer();
        p1.setUri("testuri");
        p1.setDuration(1000000);
        p1.addInteractionId("interaction1");
        c1.getNodes().add(p1);

        btxn1.getNodes().add(c1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("2");
        btxn2.setName("testapp");
        btxn2.setStartTime(System.currentTimeMillis() - 3000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri");
        c2.setDuration(500000);
        c2.addInteractionId("interaction1");
        btxn2.getNodes().add(c2);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);
        btxns.add(btxn2);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        // Get transaction count
        List<CommunicationSummaryStatistics> stats = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/communication/summary");
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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

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
        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        try {
            service.publish(null, btxns);
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

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(1, result.size());

        // Get transaction count
        List<String> hosts = null;

        try {
            URL url = new URL(service.getBaseUrl() + "analytics/hostnames");
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

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(baseTime); // Within last hour

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

        btxn1.getNodes().add(c1);

        BusinessTransaction btxn2 = new BusinessTransaction();
        btxn2.setId("2");
        btxn2.setName("testapp");
        btxn2.setStartTime(baseTime + 1000); // Within last hour

        Consumer c2 = new Consumer();
        c2.setUri("testuri2");
        c2.setDuration(500000000);
        c2.setBaseTime(1);
        c2.addInteractionId("interaction2");

        Component comp2 = new Component();
        comp2.setDuration(1500000000);
        comp2.setBaseTime(1);
        c2.getNodes().add(comp2);

        btxn2.getNodes().add(c2);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);
        btxns.add(btxn2);

        try {
            service.publish(null, btxns);
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

        assertEquals(1000, btxn1.calculateDuration());
        assertEquals(1500, btxn2.calculateDuration());

        // Query stored business transaction
        List<BusinessTransaction> result = service.query(null, new Criteria());

        assertEquals(2, result.size());

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria, 10000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        assertEquals(1750, stats.get(0).getAverage());
        assertEquals(1, stats.get(0).getCount());
    }

}
