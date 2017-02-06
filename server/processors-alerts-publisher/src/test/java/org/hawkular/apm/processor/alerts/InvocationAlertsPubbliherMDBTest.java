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
package org.hawkular.apm.processor.alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.junit.Test;

/**
 * @author Juraci Paixão Kröhling
 */
public class InvocationAlertsPubbliherMDBTest {
    @Test
    public void eventConsumesFault() {
        NodeDetails nodeDetails = new NodeDetails();
        nodeDetails.getProperties().add(new Property(Constants.PROP_FAULT, "the fault"));
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals(nodeDetails.getProperties().iterator().next().getValue(),
                event.getTags().get(Constants.PROP_FAULT));
    }

    @Test
    public void eventGetsProperContext() {
        NodeDetails nodeDetails = new NodeDetails();
        nodeDetails.setUri("/foo/bar");
        nodeDetails.setOperation("GET");
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals(nodeDetails.getUri(), event.getTags().get("uri"));
        assertEquals(nodeDetails.getOperation(), event.getTags().get("operation"));
    }

    @Test
    public void eventSetsDataSource() {
        NodeDetails nodeDetails = new NodeDetails();
        nodeDetails.setHostName("myhost");
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals(nodeDetails.getHostName(), event.getDataSource());
    }

    @Test
    public void eventSetsId() {
        NodeDetails nodeDetails = new NodeDetails();
        nodeDetails.setId("abc123");
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertNotEquals("eventSetsId-abc123", event.getId());
        assertNotEquals("abc123", event.getId());
        assertEquals("abc123", event.getContext().get("id"));
    }

    @Test
    public void eventSetsDataId() {
        NodeDetails nodeDetails = new NodeDetails();
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals("Invocation", event.getDataId());
    }

    @Test
    public void eventSetsCategory() {
        NodeDetails nodeDetails = new NodeDetails();
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals("APM", event.getCategory());
    }

    @Test
    public void eventSetsProperties() {
        NodeDetails nodeDetails = new NodeDetails();
        nodeDetails.getProperties().add(new Property("foo", "baz"));
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals("baz", event.getTags().get("foo"));
    }

    @Test
    public void eventSetsMultiValuedProperties() {
        NodeDetails nodeDetails = new NodeDetails();
        nodeDetails.getProperties().add(new Property("foo", "baz"));
        nodeDetails.getProperties().add(new Property("foo", "bar"));
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        String result = event.getTags().get("foo");
        assertNotNull(result);
        assertTrue(result.equals("baz,bar") || result.equals("bar,baz"));
    }

    @Test
    public void eventSetsCreationTime() {
        NodeDetails nodeDetails = new NodeDetails();
        long now = Clock.systemDefaultZone().millis();
        nodeDetails.setTimestamp(now);
        Event event = InvocationAlertsPublisherMDB.toEvent(nodeDetails);
        assertEquals(now, event.getCtime());
    }
}
