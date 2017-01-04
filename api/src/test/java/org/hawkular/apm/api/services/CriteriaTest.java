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
package org.hawkular.apm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CriteriaTest {

    @Test
    public void testGetQueryParametersNoCriteria() {
        Criteria criteria = new Criteria();

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);
        assertTrue("Empty map expected", queryParameters.isEmpty());
    }

    @Test
    public void testGetQueryParametersStartTime() {
        Criteria criteria = new Criteria();
        criteria.setStartTime(100);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);
        assertEquals("100", queryParameters.get("startTime"));
    }

    @Test
    public void testGetQueryParametersEndTime() {
        Criteria criteria = new Criteria();
        criteria.setEndTime(200);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);
        assertEquals("200", queryParameters.get("endTime"));
    }

    @Test
    public void testGetQueryParametersSingleProperties() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", Operator.HAS);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertEquals("prop1|value1", queryParameters.get("properties"));
    }

    @Test
    public void testGetQueryParametersSinglePropertiesDefault() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", null);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertEquals("prop1|value1", queryParameters.get("properties"));
    }

    @Test
    public void testGetQueryParametersSinglePropertiesExcluded() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", Operator.HASNOT);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertEquals("prop1|value1|HASNOT", queryParameters.get("properties"));
    }

    @Test
    public void testGetQueryParametersMultipleProperties() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", Operator.HAS);
        criteria.addProperty("prop2", "value2", Operator.HAS);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertTrue(queryParameters.get("properties").equals("prop1|value1,prop2|value2")
                || queryParameters.get("properties").equals("prop2|value2,prop1|value1"));
    }

    @Test
    public void testGetQueryParametersMultiplePropertiesExcluded() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", Operator.HASNOT);
        criteria.addProperty("prop2", "value2", Operator.HASNOT);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertTrue(queryParameters.get("properties").equals("prop1|value1|HASNOT,prop2|value2|HASNOT")
                || queryParameters.get("properties").equals("prop2|value2|HASNOT,prop1|value1|HASNOT"));
    }

    @Test
    public void testGetQueryParametersMultipleSameNameProperties() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", Operator.HAS);
        criteria.addProperty("prop1", "value2", Operator.HAS);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertTrue(queryParameters.get("properties").equals("prop1|value1,prop1|value2")
                || queryParameters.get("properties").equals("prop1|value2,prop1|value1"));
    }

    @Test
    public void testGetQueryParametersMultipleSameNamePropertiesExcluded() {
        Criteria criteria = new Criteria();
        criteria.addProperty("prop1", "value1", Operator.HASNOT);
        criteria.addProperty("prop1", "value2", Operator.HASNOT);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertTrue(queryParameters.get("properties").equals("prop1|value1|HASNOT,prop1|value2|HASNOT")
                || queryParameters.get("properties").equals("prop1|value2|HASNOT,prop1|value1|HASNOT"));
    }

    @Test
    public void testGetQueryParametersSingleCorrelations() {
        Criteria criteria = new Criteria();

        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("value1");

        criteria.getCorrelationIds().add(id1);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("correlations"));

        assertEquals("Interaction|value1", queryParameters.get("correlations"));
    }

    @Test
    public void testGetQueryParametersMultipleCorrelations() {
        Criteria criteria = new Criteria();

        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Interaction);
        id1.setValue("value1");

        criteria.getCorrelationIds().add(id1);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.ControlFlow);
        id2.setValue("value2");

        criteria.getCorrelationIds().add(id2);

        Map<String, String> queryParameters = criteria.parameters();

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("correlations"));

        assertTrue(queryParameters.get("correlations").equals("Interaction|value1,ControlFlow|value2")
                || queryParameters.get("correlations").equals("ControlFlow|value2,Interaction|value1"));
    }

}
