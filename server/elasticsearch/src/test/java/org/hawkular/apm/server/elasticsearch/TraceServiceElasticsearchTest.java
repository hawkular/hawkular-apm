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
package org.hawkular.apm.server.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class TraceServiceElasticsearchTest {

    private TraceServiceElasticsearch ts;

    private ElasticsearchClient client;

    @BeforeClass
    public static void initClass() {
        System.setProperty("HAWKULAR_APM_DATA_DIR", "target");
    }

    @Before
    public void beforeTest() {
        client = new ElasticsearchClient();
        try {
            client.init();
        } catch (Exception e) {
            fail("Failed to initialise Elasticsearch client: "+e);
        }
        ts = new TraceServiceElasticsearch();
        ts.setElasticsearchClient(client);
    }

    @After
    public void afterTest() {
        ts.clear(null);
        client.close();
    }

    @Test
    public void testQueryBTxnName() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(1000);
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setBusinessTransaction("trace2");
        trace2.setStartTime(2000);
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        traces.add(trace3);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.setBusinessTransaction("trace1");

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
        assertEquals("trace1", result1.get(0).getBusinessTransaction());
    }

    @Test
    public void testQueryNoBTxnName() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setBusinessTransaction("trace1");
        trace1.setStartTime(1000);
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setBusinessTransaction("trace2");
        trace2.setStartTime(2000);
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        traces.add(trace3);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.setBusinessTransaction("");

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id3", result1.get(0).getId());
        assertNull(result1.get(0).getBusinessTransaction());
    }

    @Test
    public void testQuerySinglePropertyAndValueIncluded() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setStartTime(1000);
        trace1.getProperties().put("prop1", "value1");
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().put("prop2", "value2");
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().put("prop1", "value3");
        traces.add(trace3);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", false);

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
    }

    @Test
    public void testQuerySinglePropertyAndValueExcluded() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setStartTime(1000);
        trace1.getProperties().put("prop1", "value1");
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().put("prop2", "value2");
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().put("prop1", "value3");
        traces.add(trace3);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", true);

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertTrue((result1.get(0).getId().equals("id2") && result1.get(1).getId().equals("id3"))
                || (result1.get(0).getId().equals("id3") && result1.get(1).getId().equals("id2")));
    }

    @Test
    public void testQuerySinglePropertyAndMultiValueIncluded() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setStartTime(1000);
        trace1.getProperties().put("prop1", "value1");
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().put("prop2", "value2");
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().put("prop3", "value3");
        traces.add(trace3);

        Trace trace4 = new Trace();
        trace4.setId("id4");
        trace4.setStartTime(4000);
        trace4.getProperties().put("prop1", "value1");
        trace4.getProperties().put("prop3", "value3");
        traces.add(trace4);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", false);
        criteria.addProperty("prop3", "value3", false);

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id4", result1.get(0).getId());
    }

    @Test
    public void testQuerySinglePropertyAndMultiValueExcluded() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setStartTime(1000);
        trace1.getProperties().put("prop1", "value1");
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().put("prop2", "value2");
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().put("prop1", "value3");
        traces.add(trace3);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", true);
        criteria.addProperty("prop1", "value3", true);

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id2", result1.get(0).getId());
    }

    @Test
    public void testQueryCorrelationId() {
        List<Trace> traces = new ArrayList<Trace>();

        Trace trace1 = new Trace();
        trace1.setId("id1");
        trace1.setStartTime(1000);
        traces.add(trace1);

        Consumer c1=new Consumer();
        c1.addGlobalId("gid1");
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        traces.add(trace2);

        Consumer c2=new Consumer();
        c2.addGlobalId("gid2");
        trace2.getNodes().add(c2);

        try {
            ts.storeTraces(null, traces);

            synchronized (this) {
                wait(1000);
            }
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Global, "gid1"));

        List<Trace> result1 = ts.query(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
    }
}
