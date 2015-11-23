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
package org.hawkular.btm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.analytics.URIInfo;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AbstractAnalyticsServiceTest {

    @Test
    public void testCompressLeaf() {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new URIInfo().setUri("/hello/" + i).setEndpointType("http"));
        }

        List<URIInfo> result = AbstractAnalyticsService.compressURIInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/hello/*", result.get(0).getUri());
        assertEquals("http", result.get(0).getEndpointType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^\\/hello\\/.*$", result.get(0).getRegex());
        assertEquals("/hello/{helloId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressPluralParameter() {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new URIInfo().setUri("/events/" + i).setEndpointType("http"));
        }

        List<URIInfo> result = AbstractAnalyticsService.compressURIInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/events/*", result.get(0).getUri());
        assertEquals("http", result.get(0).getEndpointType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^\\/events\\/.*$", result.get(0).getRegex());
        assertEquals("/events/{eventId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressMidAndLeaf() {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        for (int i = 0; i < 400; i++) {
            uris.add(new URIInfo().setUri("/hello/" + (i % 10) + "/mid/" + i / 10).setEndpointType("http"));
        }

        List<URIInfo> result = AbstractAnalyticsService.compressURIInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/hello/*/mid/*", result.get(0).getUri());
        assertEquals("http", result.get(0).getEndpointType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^\\/hello\\/.*\\/mid\\/.*$", result.get(0).getRegex());
        assertEquals("/hello/{helloId}/mid/{midId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressTop() {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new URIInfo().setUri("/" + i + "/leaf").setEndpointType("http"));
        }

        List<URIInfo> result = AbstractAnalyticsService.compressURIInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/*/leaf", result.get(0).getUri());
        assertEquals("http", result.get(0).getEndpointType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^\\/.*\\/leaf$", result.get(0).getRegex());
        assertEquals("/{param1}/leaf", result.get(0).getTemplate());
    }

    @Test
    public void testCompressConcurrentLevels() {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        for (int i = 0; i < 400; i++) {
            uris.add(new URIInfo().setUri("/" + (i % 10) + "/" + i / 10).setEndpointType("http"));
        }

        List<URIInfo> result = AbstractAnalyticsService.compressURIInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/*/*", result.get(0).getUri());
        assertEquals("http", result.get(0).getEndpointType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^\\/.*\\/.*$", result.get(0).getRegex());
        assertEquals("/{param1}/{param2}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressDontObscureIntermediateLevel() {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        uris.add(new URIInfo().setUri("/events").setEndpointType("http"));
        uris.add(new URIInfo().setUri("/events/1").setEndpointType("http"));

        List<URIInfo> result = AbstractAnalyticsService.compressURIInfo(uris);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("/events", result.get(0).getUri());
        assertEquals("http", result.get(0).getEndpointType());
        assertEquals("/events/1", result.get(1).getUri());
        assertEquals("http", result.get(1).getEndpointType());

        assertFalse(result.get(0).metaURI());
        assertFalse(result.get(1).metaURI());
    }
}
