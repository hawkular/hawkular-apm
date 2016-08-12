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

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.tests.common.Wait;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class TraceServiceElasticsearchTest {

    private TraceServiceElasticsearch ts;

    @BeforeClass
    public static void initClass() {
        System.setProperty("HAWKULAR_APM_CONFIG_DIR", "target");
    }

    @Before
    public void beforeTest() {
        ts = new TraceServiceElasticsearch();
    }

    @After
    public void afterTest() {
        ts.clear(null);
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
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.setBusinessTransaction("trace1");

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

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
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.setBusinessTransaction("");

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

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
        trace1.getProperties().add(new Property("prop1", "value1"));
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().add(new Property("prop2", "value2"));
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().add(new Property("prop1", "value3"));
        traces.add(trace3);

        try {
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", null);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

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
        trace1.getProperties().add(new Property("prop1", "value1"));
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().add(new Property("prop2", "value2"));
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().add(new Property("prop1", "value3"));
        traces.add(trace3);

        try {
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", Operator.HASNOT);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 2);
        List<Trace> result1 = ts.searchFragments(null, criteria);

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
        trace1.getProperties().add(new Property("prop1", "value1"));
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().add(new Property("prop2", "value2"));
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().add(new Property("prop3", "value3"));
        traces.add(trace3);

        Trace trace4 = new Trace();
        trace4.setId("id4");
        trace4.setStartTime(4000);
        trace4.getProperties().add(new Property("prop1", "value1"));
        trace4.getProperties().add(new Property("prop3", "value3"));
        traces.add(trace4);

        try {
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", null);
        criteria.addProperty("prop3", "value3", null);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

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
        trace1.getProperties().add(new Property("prop1", "value1"));
        traces.add(trace1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        trace2.getProperties().add(new Property("prop2", "value2"));
        traces.add(trace2);

        Trace trace3 = new Trace();
        trace3.setId("id3");
        trace3.setStartTime(3000);
        trace3.getProperties().add(new Property("prop1", "value3"));
        traces.add(trace3);

        try {
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.addProperty("prop1", "value1", Operator.HASNOT);
        criteria.addProperty("prop1", "value3", Operator.HASNOT);

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

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
        c1.addInteractionId("gid1");
        trace1.getNodes().add(c1);

        Trace trace2 = new Trace();
        trace2.setId("id2");
        trace2.setStartTime(2000);
        traces.add(trace2);

        Consumer c2=new Consumer();
        c2.addInteractionId("gid2");
        trace2.getNodes().add(c2);

        try {
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        Criteria criteria = new Criteria();
        criteria.setStartTime(100);
        criteria.getCorrelationIds().add(new CorrelationIdentifier(Scope.Interaction, "gid1"));

        Wait.until(() -> ts.searchFragments(null, criteria).size() == 1);
        List<Trace> result1 = ts.searchFragments(null, criteria);

        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals("id1", result1.get(0).getId());
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
            ts.storeFragments(null, traces);
        } catch (Exception e) {
            fail("Failed to store");
        }

        // Retrieve stored trace
        Wait.until(() -> ts.getTrace(null, "1") != null);
        Trace result = ts.getTrace(null, "1");

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

}
