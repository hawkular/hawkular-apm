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
package org.hawkular.btm.server.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.junit.Test;

/**
 * @author gbrown
 */
public class BusinessTransactionHandlerTest {

    @Test
    public void testDecodePropertiesSingle() {
        String encoded = "hello|world";
        Map<String, String> properties = new HashMap<String, String>();

        BusinessTransactionHandler.decodeProperties(properties, encoded);

        assertTrue("Properties should have 1 entry", properties.size() == 1);

        assertEquals("world", properties.get("hello"));
    }

    @Test
    public void testDecodePropertiesSingleWithSpaces() {
        String encoded = "hello | world ";
        Map<String, String> properties = new HashMap<String, String>();

        BusinessTransactionHandler.decodeProperties(properties, encoded);

        assertTrue("Properties should have 1 entry", properties.size() == 1);

        assertEquals("world", properties.get("hello"));
    }

    @Test
    public void testDecodePropertiesMultiple() {
        String encoded = "hello|world,fred|bloggs";
        Map<String, String> properties = new HashMap<String, String>();

        BusinessTransactionHandler.decodeProperties(properties, encoded);

        assertTrue("Properties should have 2 entries", properties.size() == 2);

        assertEquals("world", properties.get("hello"));
        assertEquals("bloggs", properties.get("fred"));
    }

    @Test
    public void testDecodeCorrelationIdsSingle() {
        String encoded = "Global|world";
        Set<CorrelationIdentifier> correlations = new HashSet<CorrelationIdentifier>();

        BusinessTransactionHandler.decodeCorrelationIdentifiers(correlations, encoded);

        assertTrue("Correlation identifiers should have 1 entry", correlations.size() == 1);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Global);
        cid.setValue("world");

        assertTrue(correlations.contains(cid));
    }

    @Test
    public void testDecodeCorrelationIdsSingleWithSpaces() {
        String encoded = " Local | world ";
        Set<CorrelationIdentifier> correlations = new HashSet<CorrelationIdentifier>();

        BusinessTransactionHandler.decodeCorrelationIdentifiers(correlations, encoded);

        assertTrue("Correlation identifiers should have 1 entry", correlations.size() == 1);

        CorrelationIdentifier cid = new CorrelationIdentifier();
        cid.setScope(Scope.Local);
        cid.setValue("world");

        assertTrue(correlations.contains(cid));
    }

    @Test
    public void testDecodeCorrelationIdsMultiple() {
        String encoded = "Global|world,Interaction|hello";
        Set<CorrelationIdentifier> correlations = new HashSet<CorrelationIdentifier>();

        BusinessTransactionHandler.decodeCorrelationIdentifiers(correlations, encoded);

        assertTrue("Correlation identifiers should have 2 entry", correlations.size() == 2);

        CorrelationIdentifier cid1 = new CorrelationIdentifier();
        cid1.setScope(Scope.Global);
        cid1.setValue("world");

        CorrelationIdentifier cid2 = new CorrelationIdentifier();
        cid2.setScope(Scope.Interaction);
        cid2.setValue("hello");

        assertTrue(correlations.contains(cid1));
        assertTrue(correlations.contains(cid2));
    }

}
