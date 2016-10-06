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
package org.hawkular.apm.processor.alerts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.junit.Test;

/**
 * @author Juraci Paixão Kröhling
 */
public class EventTest {
    @Test
    public void eventConsumesFault() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setFault("the fault");
        Event event = new Event(completionTime, "eventConsumesFault");
        assertEquals(event.getTags().get("fault"), completionTime.getFault());
    }

    @Test(expected = IllegalStateException.class)
    public void failsOnNullEventSource() {
        new Event(new CompletionTime(), null);
    }

    @Test(expected = IllegalStateException.class)
    public void failsOnEmptyEventSource() {
        new Event(new CompletionTime(), "");
    }

    @Test
    public void eventGetsProperContext() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setUri("/foo/bar");
        completionTime.setOperation("GET");
        Event event = new Event(completionTime, "eventGetsProperContext");
        assertEquals(event.getTags().get("uri"), completionTime.getUri());
        assertEquals(event.getTags().get("operation"), completionTime.getOperation());
    }

    @Test
    public void eventSetsDataSource() {
        CompletionTime completionTime = new CompletionTime();
        Event event = new Event(completionTime, "eventSetsDataSource");
        assertEquals(event.getDataSource(), "APM");
    }

    @Test
    public void eventSetsId() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.setId("abc123");
        Event event = new Event(completionTime, "eventSetsId");
        assertNotEquals(event.getId(), "eventSetsId-abc123");
        assertNotEquals(event.getId(), "abc123");
        assertEquals(event.getContext().get("id"), "abc123");
    }

    @Test
    public void eventSetsCategory() {
        CompletionTime completionTime = new CompletionTime();
        Event event = new Event(completionTime, "eventSetsCategory");
        assertEquals(event.getCategory(), "eventSetsCategory");
    }

    @Test
    public void eventSetsProperties() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.getProperties().add(new Property("foo", "baz"));
        Event event = new Event(completionTime, "eventSetsProperties");
        assertEquals(event.getTags().get("foo"), "baz");
    }

    @Test
    public void eventSetsMultiValuedProperties() {
        CompletionTime completionTime = new CompletionTime();
        completionTime.getProperties().add(new Property("foo", "baz"));
        completionTime.getProperties().add(new Property("foo", "bar"));
        Event event = new Event(completionTime, "eventSetsProperties");
        String result = event.getTags().get("foo");
        assertNotNull(result);
        assertTrue(result.equals("baz,bar") || result.equals("bar,baz"));
    }

    @Test
    public void eventSetsCreationTime() {
        CompletionTime completionTime = new CompletionTime();
        long now = Clock.systemDefaultZone().millis();
        completionTime.setTimestamp(now);
        Event event = new Event(completionTime, "eventSetsCreationTime");
        assertEquals(event.getCtime(), now);
    }
}
