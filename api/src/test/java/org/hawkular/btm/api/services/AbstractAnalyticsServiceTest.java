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
package org.hawkular.btm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.analytics.EndpointInfo;
import org.hawkular.btm.api.utils.EndpointUtil;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AbstractAnalyticsServiceTest {

    @Test
    public void testCompressLeafNoOp() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new EndpointInfo().setEndpoint(EndpointUtil.encodeEndpoint("/hello/" + i, null))
                    .setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/hello/*", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^/hello/.*$", result.get(0).getRegex());
        assertEquals("/hello/{helloId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressLeafWithSingleOp() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new EndpointInfo().setEndpoint(EndpointUtil.encodeEndpoint("/hello/" + i, "op"))
                    .setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/hello/*[op]", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^/hello/.*\\[op\\]$", result.get(0).getRegex());
        assertEquals("/hello/{helloId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressLeafWithMultiOp() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 30; i++) {
            uris.add(new EndpointInfo().setEndpoint(EndpointUtil.encodeEndpoint("/hello/" + i, "op" + (i%2)))
                    .setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(2, result.size());

        EndpointInfo op0=null;
        EndpointInfo op1=null;

        for (EndpointInfo ei : result) {
            if (ei.getEndpoint().endsWith("[op0]")) {
                op0 = ei;
            } else if (ei.getEndpoint().endsWith("[op1]")) {
                op1 = ei;
            } else {
                fail("Unknown endpoint: "+ei);
            }
        }

        assertEquals("/hello/*[op0]", op0.getEndpoint());
        assertEquals("/hello/*[op1]", op1.getEndpoint());
        assertEquals("http", op0.getType());
        assertEquals("http", op1.getType());

        assertTrue(op0.metaURI());
        assertTrue(op1.metaURI());
        assertEquals("^/hello/.*\\[op0\\]$", op0.getRegex());
        assertEquals("^/hello/.*\\[op1\\]$", op1.getRegex());
        assertEquals("/hello/{helloId}", op0.getTemplate());
        assertEquals("/hello/{helloId}", op1.getTemplate());
    }

    @Test
    public void testCompressPluralParameter() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new EndpointInfo().setEndpoint("/events/" + i).setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/events/*", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^/events/.*$", result.get(0).getRegex());
        assertEquals("/events/{eventId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressMidAndLeaf() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 400; i++) {
            uris.add(new EndpointInfo().setEndpoint("/hello/" + (i % 10) + "/mid/" + i / 10).setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/hello/*/mid/*", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^/hello/.*/mid/.*$", result.get(0).getRegex());
        assertEquals("/hello/{helloId}/mid/{midId}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressTop() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 20; i++) {
            uris.add(new EndpointInfo().setEndpoint("/" + i + "/leaf").setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/*/leaf", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^/.*/leaf$", result.get(0).getRegex());
        assertEquals("/{param1}/leaf", result.get(0).getTemplate());
    }

    @Test
    public void testCompressConcurrentLevels() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        for (int i = 0; i < 400; i++) {
            uris.add(new EndpointInfo().setEndpoint("/" + (i % 10) + "/" + i / 10).setType("http"));
        }

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/*/*", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertTrue(result.get(0).metaURI());
        assertEquals("^/.*/.*$", result.get(0).getRegex());
        assertEquals("/{param1}/{param2}", result.get(0).getTemplate());
    }

    @Test
    public void testCompressDontObscureIntermediateLevel() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        uris.add(new EndpointInfo().setEndpoint("/events").setType("http"));
        uris.add(new EndpointInfo().setEndpoint("/events/1").setType("http"));

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("/events", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());
        assertEquals("/events/1", result.get(1).getEndpoint());
        assertEquals("http", result.get(1).getType());

        assertFalse(result.get(0).metaURI());
        assertFalse(result.get(1).metaURI());
    }

    @Test
    public void testCompressDontNonRest() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        uris.add(new EndpointInfo().setEndpoint("/events").setType("http"));
        uris.add(new EndpointInfo().setEndpoint("[HornetQ]MyQueue").setType("mom"));
        uris.add(new EndpointInfo().setEndpoint("OtherURI-with-no-slash").setType("other"));

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("/events", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());
        assertEquals("[HornetQ]MyQueue", result.get(1).getEndpoint());
        assertEquals("mom", result.get(1).getType());
        assertEquals("OtherURI-with-no-slash", result.get(2).getEndpoint());
        assertEquals("other", result.get(2).getType());

        assertFalse(result.get(0).metaURI());
        assertFalse(result.get(1).metaURI());
        assertFalse(result.get(2).metaURI());

        assertNotNull(result.get(0).getRegex());
        assertNotNull(result.get(1).getRegex());
        assertNotNull(result.get(2).getRegex());
    }

    @Test
    public void testCompressRootContext() {
        List<EndpointInfo> uris = new ArrayList<EndpointInfo>();

        uris.add(new EndpointInfo().setEndpoint("/").setType("http"));

        List<EndpointInfo> result = AbstractAnalyticsService.compressEndpointInfo(uris);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/", result.get(0).getEndpoint());
        assertEquals("http", result.get(0).getType());

        assertFalse(result.get(0).metaURI());

        assertNotNull(result.get(0).getRegex());
    }
}
