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
import java.util.List;

import org.hawkular.btm.analytics.service.rest.client.AnalyticsServiceRESTClient;
import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.CompletionTimeCriteria;
import org.hawkular.btm.api.services.NodeCriteria;
import org.hawkular.btm.btxn.service.rest.client.BusinessTransactionServiceRESTClient;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private static final TypeReference<java.util.List<String>> STRING_LIST =
            new TypeReference<java.util.List<String>>() {
    };

    private static final TypeReference<java.util.List<Cardinality>> CARDINALITY_LIST =
            new TypeReference<java.util.List<Cardinality>>() {
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    // NOTE: Tests are using the fact that the business transaction service is
    // overwriting the same business transaction id (i.e. 1).

    @Test
    public void testGetUnboundURIs() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored business transaction
        List<URIInfo> uris = analytics.getUnboundURIs(null, 0, 0, true);

        assertNotNull(uris);
        assertEquals(1, uris.size());
        assertEquals("testuri", uris.get(0).getUri());
    }

    @Test
    public void testGetBoundURIs() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        // Retrieve stored business transaction URIs
        List<String> uris = analytics.getBoundURIs(null, "btxn1", 0, 0);

        assertNotNull(uris);
        assertEquals(1, uris.size());
        assertTrue(uris.contains("testuri"));
    }

    @Test
    public void testGetPropertyInfo() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        List<PropertyInfo> pis = analytics.getPropertyInfo(null, "btxn1", 0, 0);

        assertNotNull(pis);
        assertEquals(1, pis.size());
        assertTrue(pis.get(0).getName().equals("prop1"));
    }

    @Test
    public void testGetCompletionCount() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        CompletionTimeCriteria criteria = new CompletionTimeCriteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        Long count = analytics.getCompletionCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionFaultCount() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        CompletionTimeCriteria criteria = new CompletionTimeCriteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        Long count = analytics.getCompletionFaultCount(null, criteria);

        assertNotNull(count);
        assertEquals(1, count.longValue());
    }

    @Test
    public void testGetCompletionTimeseriesStatistics() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        CompletionTimeCriteria criteria = new CompletionTimeCriteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<CompletionTimeseriesStatistics> stats = analytics.getCompletionTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetCompletionTimeseriesStatisticsPOST() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            os.write(mapper.writeValueAsBytes(new CompletionTimeCriteria().setBusinessTransaction("testapp")));

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
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        CompletionTimeCriteria criteria = new CompletionTimeCriteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getCompletionPropertyDetails(null, criteria, "prop1");

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionPropertyDetailsPOST() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            os.write(mapper.writeValueAsBytes(new CompletionTimeCriteria().setBusinessTransaction("testapp")));

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
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        CompletionTimeCriteria criteria = new CompletionTimeCriteria();
        criteria.setBusinessTransaction("testapp").setStartTime(0).setEndTime(0);

        List<Cardinality> cards = analytics.getCompletionFaultDetails(null, criteria);

        assertNotNull(cards);
        assertEquals(1, cards.size());
    }

    @Test
    public void testGetCompletionFaultDetailsPOST() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            os.write(mapper.writeValueAsBytes(new CompletionTimeCriteria().setBusinessTransaction("testapp")));

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
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        NodeCriteria criteria = new NodeCriteria();
        criteria.setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsPOST() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            os.write(mapper.writeValueAsBytes(new NodeCriteria()));

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
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        NodeCriteria criteria = new NodeCriteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        // Get transaction count
        List<NodeTimeseriesStatistics> stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(1, stats.size());

        criteria = new NodeCriteria();
        criteria.setHostName("hostB").setStartTime(0).setEndTime(0);

        // Get transaction count
        stats = analytics.getNodeTimeseriesStatistics(null, criteria, 1000);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeTimeseriesStatisticsPOSTHostName() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            os.write(mapper.writeValueAsBytes(new NodeCriteria().setHostName("hostA")));

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

            os.write(mapper.writeValueAsBytes(new NodeCriteria().setHostName("hostB")));

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
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        NodeCriteria criteria = new NodeCriteria();
        criteria.setStartTime(0).setEndTime(0);

        List<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsPOST() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            os.write(mapper.writeValueAsBytes(new NodeCriteria()));

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
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        NodeCriteria criteria = new NodeCriteria();
        criteria.setHostName("hostA").setStartTime(0).setEndTime(0);

        List<NodeSummaryStatistics> stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(2, stats.size());

        criteria = new NodeCriteria();
        criteria.setHostName("hostB").setStartTime(0).setEndTime(0);

        stats = analytics.getNodeSummaryStatistics(null, criteria);

        assertNotNull(stats);
        assertEquals(0, stats.size());
    }

    @Test
    public void testGetNodeSummaryStatisticsPOSTHostName() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.setName("testapp");
        btxn1.setStartTime(System.currentTimeMillis() - 4000); // Within last hour
        btxn1.setHostName("hostA");

        Consumer c1 = new Consumer();
        c1.setUri("testuri");
        c1.setDuration(1000000);
        btxn1.getNodes().add(c1);

        Component comp1=new Component();
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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            NodeCriteria criteria=new NodeCriteria();
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

            NodeCriteria criteria=new NodeCriteria();
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
    public void testGetHostNames() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

        assertEquals(1, result.size());

        assertEquals("1", result.get(0).getId());

        NodeCriteria criteria = new NodeCriteria();
        criteria.setStartTime(0).setEndTime(0);

        List<String> hosts = analytics.getHostNames(null, criteria);

        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("hostA", hosts.get(0));
    }

    @Test
    public void testGetHostNamesPOST() {
        AnalyticsServiceRESTClient analytics = new AnalyticsServiceRESTClient();
        analytics.setUsername(TEST_USERNAME);
        analytics.setPassword(TEST_PASSWORD);

        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

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
        List<BusinessTransaction> result = service.query(null, new BusinessTransactionCriteria());

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

            NodeCriteria criteria=new NodeCriteria();

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

}
