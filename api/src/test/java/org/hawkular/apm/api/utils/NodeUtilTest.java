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
package org.hawkular.apm.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.trace.Consumer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class NodeUtilTest {

    private static final String NEW_URI = "/new/uri";
    private static final String ORIGINAL_URI = "/original/uri";

    @Test
    public void testRewriteURI() {
        Consumer consumer = new Consumer();
        consumer.setUri(ORIGINAL_URI);

        NodeUtil.rewriteURI(consumer, NEW_URI);

        assertEquals(NEW_URI, consumer.getUri());
    }

    @Test
    public void testIsOriginalURINotRewritten() {
        Consumer consumer = new Consumer();
        consumer.setUri(ORIGINAL_URI);

        assertTrue(NodeUtil.isOriginalURI(consumer, ORIGINAL_URI));
    }

    @Test
    public void testIsOriginalURIRewritten() {
        Consumer consumer = new Consumer();
        consumer.setUri(ORIGINAL_URI);

        NodeUtil.rewriteURI(consumer, NEW_URI);

        assertTrue(NodeUtil.isOriginalURI(consumer, ORIGINAL_URI));
    }

    @Test
    public void testRewriteURINoTemplate() {
        Consumer consumer = new Consumer();
        consumer.setUri("/anything");
        assertFalse(NodeUtil.rewriteURI(consumer));
    }

    @Test
    public void testRewriteURITemplateNoQuery1() {
        Consumer consumer = new Consumer();
        consumer.setUri("/service/helloworld");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_URL_TEMPLATE, "/service/{serviceName}"));
        assertTrue(NodeUtil.rewriteURI(consumer));
        assertEquals("/service/{serviceName}", consumer.getUri());
        assertTrue(consumer.hasProperty("serviceName"));
        assertEquals("helloworld", consumer.getProperties("serviceName").iterator().next().getValue());
    }

    @Test
    public void testRewriteURITemplateWithSpec() {
        Consumer consumer = new Consumer();
        consumer.setUri("/service/helloworld");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_URL_TEMPLATE, "/service/{serviceName:.+}"));
        assertTrue(NodeUtil.rewriteURI(consumer));
        assertEquals("/service/{serviceName:.+}", consumer.getUri());
        assertTrue(consumer.hasProperty("serviceName"));
        assertEquals("helloworld", consumer.getProperties("serviceName").iterator().next().getValue());
    }

    @Test
    public void testRewriteURITemplateWithMultipleSlashes() {
        Consumer consumer = new Consumer();
        consumer.setUri("/download/aa/bb/cc");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_URL_TEMPLATE, "/download/{file:.+}"));
        assertTrue(NodeUtil.rewriteURI(consumer));
        assertEquals("/download/{file:.+}", consumer.getUri());
        assertTrue(consumer.hasProperty("file"));
        assertEquals("aa/bb/cc", consumer.getProperties("file").iterator().next().getValue());
    }

    @Test
    public void testRewriteURITemplateNoQuery2() {
        Consumer consumer = new Consumer();
        consumer.setUri("/service/helloworld/123");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_URL_TEMPLATE, "/service/{serviceName}/{num}"));
        assertTrue(NodeUtil.rewriteURI(consumer));
        assertEquals("/service/{serviceName}/{num}", consumer.getUri());
        assertTrue(consumer.hasProperty("serviceName"));
        assertEquals("helloworld", consumer.getProperties("serviceName").iterator().next().getValue());
        assertTrue(consumer.hasProperty("num"));
        assertEquals("123", consumer.getProperties("num").iterator().next().getValue());
    }

    @Test
    public void testRewriteURITemplateWithQuery() {
        Consumer consumer = new Consumer();
        consumer.setUri("/service/helloworld");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_QUERY, "param1=fred&param2=joe"));
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_URL_TEMPLATE, "/service/{serviceName}?{param2}&{param1}"));
        assertTrue(NodeUtil.rewriteURI(consumer));
        assertEquals("/service/{serviceName}", consumer.getUri());
        assertTrue(consumer.hasProperty("param1"));
        assertEquals("fred", consumer.getProperties("param1").iterator().next().getValue());
        assertTrue(consumer.hasProperty("param2"));
        assertEquals("joe", consumer.getProperties("param2").iterator().next().getValue());
    }

    @Test
    public void dontFailOnMisconfiguration() {
        Consumer consumer = new Consumer();
        consumer.setUri("/service/helloworld");
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_QUERY, "param1=fred&param2=joe"));
        consumer.getProperties().add(new Property(Constants.PROP_HTTP_URL_TEMPLATE, null));
        assertFalse(NodeUtil.rewriteURI(consumer));
    }

}
