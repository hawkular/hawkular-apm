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
package org.hawkular.apm.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.api.services.Criteria.PropertyCriteria;
import org.junit.Test;

/**
 * @author gbrown
 */
public class RESTServiceUtilTest {

    @Test
    public void testDecodePropertiesSingle() {
        String encoded = "hello|world";
        Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();

        RESTServiceUtil.decodeProperties(properties, encoded);

        assertTrue("Properties should have 1 entry", properties.size() == 1);

        PropertyCriteria pc = properties.iterator().next();

        assertEquals("hello", pc.getName());
        assertEquals("world", pc.getValue());
        assertEquals(Operator.HAS, pc.getOperator());
    }

    @Test
    public void testDecodePropertiesSingleHasNot() {
        String encoded = "hello|world|HASNOT";
        Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();

        RESTServiceUtil.decodeProperties(properties, encoded);

        assertTrue("Properties should have 1 entry", properties.size() == 1);

        PropertyCriteria pc = properties.iterator().next();

        assertEquals("hello", pc.getName());
        assertEquals("world", pc.getValue());
        assertEquals(Operator.HASNOT, pc.getOperator());
    }

    @Test
    public void testDecodePropertiesSingleWithSpaces() {
        String encoded = "hello | world ";
        Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();

        RESTServiceUtil.decodeProperties(properties, encoded);

        PropertyCriteria pc = properties.iterator().next();

        assertEquals("hello", pc.getName());
        assertEquals("world", pc.getValue());
        assertEquals(Operator.HAS, pc.getOperator());
    }

    @Test
    public void testDecodePropertiesSingleWithSpacesHasNot() {
        String encoded = "hello | world | HASNOT ";
        Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();

        RESTServiceUtil.decodeProperties(properties, encoded);

        PropertyCriteria pc = properties.iterator().next();

        assertEquals("hello", pc.getName());
        assertEquals("world", pc.getValue());
        assertEquals(Operator.HASNOT, pc.getOperator());
    }

    @Test
    public void testDecodePropertiesMultiple() {
        String encoded = "hello|world,fred|bloggs";
        Set<PropertyCriteria> properties = new HashSet<PropertyCriteria>();

        RESTServiceUtil.decodeProperties(properties, encoded);

        assertTrue("Properties should have 2 entries", properties.size() == 2);

        Iterator<PropertyCriteria> iter = properties.iterator();
        PropertyCriteria pc1 = iter.next();
        PropertyCriteria pc2 = iter.next();

        if (pc1.getName().equals("hello")) {
            assertEquals("hello", pc1.getName());
            assertEquals("world", pc1.getValue());
            assertEquals(Operator.HAS, pc1.getOperator());

            assertEquals("fred", pc2.getName());
            assertEquals("bloggs", pc2.getValue());
            assertEquals(Operator.HAS, pc1.getOperator());
        } else {
            assertEquals("hello", pc2.getName());
            assertEquals("world", pc2.getValue());
            assertEquals(Operator.HAS, pc2.getOperator());

            assertEquals("fred", pc1.getName());
            assertEquals("bloggs", pc1.getValue());
            assertEquals(Operator.HAS, pc2.getOperator());
        }
    }

    @Test
    public void testDecodeCorrelationIdsSingle() {
        String encoded = "Interaction|world";
        Set<CorrelationIdentifier> correlations = new HashSet<CorrelationIdentifier>();

        RESTServiceUtil.decodeCorrelationIdentifiers(correlations, encoded);

        assertTrue("Correlation identifiers should have 1 entry", correlations.size() == 1);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Interaction);
        cid.setValue("world");

        assertTrue(correlations.contains(cid));
    }

    @Test
    public void testDecodeCorrelationIdsSingleWithSpaces() {
        String encoded = " Interaction | world ";
        Set<CorrelationIdentifier> correlations = new HashSet<CorrelationIdentifier>();

        RESTServiceUtil.decodeCorrelationIdentifiers(correlations, encoded);

        assertTrue("Correlation identifiers should have 1 entry", correlations.size() == 1);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Interaction);
        cid.setValue("world");

        assertTrue(correlations.contains(cid));
    }

    @Test
    public void testDecodeCorrelationIdsMultiple() {
        String encoded = "Interaction|world,ControlFlow|hello";
        Set<CorrelationIdentifier> correlations = new HashSet<CorrelationIdentifier>();

        RESTServiceUtil.decodeCorrelationIdentifiers(correlations, encoded);

        assertTrue("Correlation identifiers should have 2 entry", correlations.size() == 2);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Interaction);
        cid1.setValue("world");

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.ControlFlow);
        cid2.setValue("hello");

        assertTrue(correlations.contains(cid1));
        assertTrue(correlations.contains(cid2));
    }

}
