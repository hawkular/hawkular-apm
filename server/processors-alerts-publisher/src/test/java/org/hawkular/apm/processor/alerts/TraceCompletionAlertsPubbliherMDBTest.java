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
import org.hawkular.apm.api.model.events.CompletionTime;
import org.junit.Test;

/**
 * @author Juraci Paixão Kröhling
 */
public class TraceCompletionAlertsPubbliherMDBTest {
    @Test
    public void eventConsumesFault() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.getProperties().add(new Property(Constants.PROP_FAULT, "the fault"));
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals(completionTime.getProperties().iterator().next().getValue(),
                event.getTags().get(Constants.PROP_FAULT));
    }

    @Test
    public void eventGetsProperContext() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setUri("/foo/bar");
        completionTime.setOperation("GET");
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals(completionTime.getUri(), event.getTags().get("uri"));
        assertEquals(completionTime.getOperation(), event.getTags().get("operation"));
    }

    @Test
    public void eventSetsDataSource() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setHostName("myhost");
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals(completionTime.getHostName(), event.getDataSource());
    }

    @Test
    public void eventSetsId() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setId("abc123");
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertNotEquals("eventSetsId-abc123", event.getId());
        assertNotEquals("abc123", event.getId());
        assertEquals("abc123", event.getContext().get("id"));
    }

    @Test
    public void eventSetsDataId() {
        CompletionTime completionTime = new CompletionTime();
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals("TraceCompletion", event.getDataId());
    }

    @Test
    public void eventSetsCategory() {
        CompletionTime completionTime = new CompletionTime();
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals("APM", event.getCategory());
    }

    @Test
    public void eventSetsProperties() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.getProperties().add(new Property("foo", "baz"));
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals("baz", event.getTags().get("foo"));
    }

    @Test
    public void eventSetsMultiValuedProperties() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.getProperties().add(new Property("foo", "baz"));
        completionTime.getProperties().add(new Property("foo", "bar"));
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        String result = event.getTags().get("foo");
        assertNotNull(result);
        assertTrue(result.equals("baz,bar") || result.equals("bar,baz"));
    }

    @Test
    public void eventSetsCreationTime() {
        CompletionTime completionTime = new CompletionTime();
        long now = Clock.systemDefaultZone().millis();
        completionTime.setTimestamp(now);
        Event event = TraceCompletionAlertsPublisherMDB.toEvent(completionTime);
        assertEquals(now, event.getCtime());
    }
}
