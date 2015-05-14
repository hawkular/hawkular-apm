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
package org.hawkular.btm.btxn.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
public class BusinessTransactionServiceRESTTests {

    private static Client client = new ResteasyClientBuilder().build();
    private WebTarget baseTarget = client.target(System.getProperty("hawkular.base-uri"));

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
            };

    ObjectMapper mapper = new ObjectMapper();

    @AfterClass
    public static void close() {
        client.close();
    }

    @Test
    public void testStoreAndRetrieveById() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        Response resp1 = target1.request().post(Entity.json(btxns));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Retrieve stored business transaction
        WebTarget target2 = baseTarget.path("transactions/1");

        Response resp2 = target2.request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            BusinessTransaction result = resp2.readEntity(BusinessTransaction.class);

            assertEquals("1", result.getId());
        } finally {
            resp1.close();
        }

    }

    @Test
    public void testStoreAndQueryAll() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(1, result.size());

            assertEquals("1", result.get(0).getId());
        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    public void testStoreAndQueryStartTimeInclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("startTime", "100").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(1, result.size());

            assertEquals("1", result.get(0).getId());
        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    public void testStoreAndQueryStartTimeExclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("startTime", "1100").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(0, result.size());

        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    public void testStoreAndQueryEndTimeInclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        c1.setDuration(500);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("endTime", "2000").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(1, result.size());

            assertEquals("1", result.get(0).getId());
        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    public void testStoreAndQueryEndTimeExclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        Consumer c1 = new Consumer();
        c1.setStartTime(1000);
        c1.setDuration(500);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("endTime", "1100").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(0, result.size());

        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    @Ignore("Until HAWKULAR-225 is fixed")
    public void testStoreAndQueryPropertiesInclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.getProperties().put("hello", "world");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("properties", "hello|world").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(1, result.size());

            assertEquals("1", result.get(0).getId());
        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    @Ignore("Until HAWKULAR-225 is fixed")
    public void testStoreAndQueryPropertiesExclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");
        btxn1.getProperties().put("hello", "world");

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("properties", "hello|fred").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(0, result.size());

        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    @Ignore("Until HAWKULAR-225 is fixed")
    public void testStoreAndQueryCorrelationsInclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("correlations", "Global|myid").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(1, result.size());

            assertEquals("1", result.get(0).getId());
        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }

    @Test
    @Ignore("Until HAWKULAR-225 is fixed")
    public void testStoreAndQueryCorrelationsExclude() {
        WebTarget target1 = baseTarget.path("transactions");

        BusinessTransaction btxn1 = new BusinessTransaction();
        btxn1.setId("1");

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("myid");

        Consumer c1 = new Consumer();
        c1.getCorrelationIds().add(cid);
        btxn1.getNodes().add(c1);

        List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();
        btxns.add(btxn1);

        String json = null;
        try {
            json = mapper.writeValueAsString(btxns);
        } catch (JsonProcessingException e1) {
            fail("Failed to serialize: " + e1);
        }

        Response resp1 = target1.request().post(Entity.json(json));

        assertNotNull(resp1);

        assertEquals(200, resp1.getStatus());

        resp1.close();

        // Wait to ensure record persisted
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Query stored business transaction
        WebTarget target2 = baseTarget.path("transactions");

        Response resp2 = target2.queryParam("correlations", "Exchange|notmyid").request().get();

        try {
            assertNotNull(resp2);
            assertEquals(200, resp2.getStatus());

            List<BusinessTransaction> result = mapper.readValue(resp2.readEntity(String.class).getBytes(),
                    BUSINESS_TXN_LIST);

            assertEquals(0, result.size());

        } catch (Exception e) {
            fail("Failed to deserialize response: " + e);
        } finally {
            resp2.close();
        }

    }
}
