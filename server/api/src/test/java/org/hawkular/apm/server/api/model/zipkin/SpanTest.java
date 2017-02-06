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
package org.hawkular.apm.server.api.model.zipkin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.PropertyType;
import org.hawkular.apm.server.api.utils.zipkin.BinaryAnnotationMappingDeriver;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author gbrown
 */
public class SpanTest {

    @Test
    public void testFullUrl() throws MalformedURLException {
        URL url = new URL("http://localhost:8080/my/path");
        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        ba.setValue(url.toString());
        Span span = new Span(Arrays.asList(ba), null);

        URL result = span.url();
        assertNotNull(result);
        assertEquals(url, result);
    }

    @Test
    public void testPartialUrl() throws MalformedURLException {
        URL url = new URL("http://localhost:8080/my/path");
        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        ba.setValue(url.getPath());
        Span span = new Span(Arrays.asList(ba), null);

        URL result = span.url();
        assertNotNull(result);
        assertEquals(url.getPath(), result.getPath());
    }

    @Test
    public void testUrlFromUri() throws MalformedURLException {
        URL url = new URL("http://localhost:8080/my/path");
        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URI);
        ba.setValue(url.toString());

        Span span = new Span(Arrays.asList(ba), null);

        URL spanURL = span.url();
        assertEquals(url, spanURL);
    }

    @Test
    public void testUrlFromPath() throws MalformedURLException {
        URL url = new URL("http://localhost:8080/my/path");
        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_PATH);
        ba.setValue(url.getPath());

        Span span = new Span(Arrays.asList(ba), null);

        URL spanURL = span.url();
        assertEquals(url.getPath(), spanURL.getPath());
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        BinaryAnnotationMappingDeriver.clearStorage();
        String testResourcesPath = getClass().getClassLoader().getResource(".").getPath();
        BinaryAnnotationMappingDeriver.getInstance(testResourcesPath + "test-binary-annotations-mapping.json");

        BinaryAnnotation stringAnnotation = new BinaryAnnotation();
        stringAnnotation.setKey("ignore.key");
        stringAnnotation.setValue("ignore.value");
        stringAnnotation.setEndpoint(createEndpoint("bonjour", "127.0.0.2", (short)8090));
        stringAnnotation.setType(AnnotationType.STRING);

        BinaryAnnotation binaryAnnotationWithMapping = new BinaryAnnotation();
        binaryAnnotationWithMapping.setKey("foo");
        binaryAnnotationWithMapping.setValue("value");
        binaryAnnotationWithMapping.setEndpoint(createEndpoint("bonjour2", "127.0.1.2", (short)8090));
        binaryAnnotationWithMapping.setType(AnnotationType.DOUBLE);

        Span span = new Span(Arrays.asList(stringAnnotation, binaryAnnotationWithMapping), serverAnnotations());

        Span deserializedSpan = (Span)deserialize(serialize(span));

        Assert.assertEquals(span, deserializedSpan);
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                new Property("foo.prop", "value", PropertyType.Number))),
                new HashSet<>(deserializedSpan.binaryAnnotationMapping().getProperties()));

        Assert.assertEquals("foo.modified", deserializedSpan.binaryAnnotationMapping().getComponentType());
        Assert.assertEquals("foo.endpoint", deserializedSpan.binaryAnnotationMapping().getEndpointType());
    }

    @Test
    public void testIsServerSpan() {
        Annotation sr = new Annotation();
        sr.setValue("sr");
        Annotation ss = new Annotation();
        ss.setValue("ss");

        Span span = new Span(null, Arrays.asList(sr, ss));
        Assert.assertTrue(span.serverSpan());

        span = new Span(null, Arrays.asList(ss, sr));
        Assert.assertTrue(span.serverSpan());

        span = new Span(null, null);
        Assert.assertFalse(span.serverSpan());
        span = new Span(null, Arrays.asList(ss));
        Assert.assertFalse(span.serverSpan());

        Annotation cr = new Annotation();
        cr.setValue("cr");
        Annotation cs = new Annotation();
        cs.setValue("cs");
        span = new Span(null, Arrays.asList(cr, cs, ss , sr));
        Assert.assertTrue(span.serverSpan());
    }

    @Test
    public void testIsClientSpan() {
        Annotation cr = new Annotation();
        cr.setValue("cr");
        Annotation cs = new Annotation();
        cs.setValue("cs");

        Span span = new Span(null, Arrays.asList(cr, cs));
        Assert.assertTrue(span.clientSpan());

        span = new Span(null, Arrays.asList(cs, cr));
        Assert.assertTrue(span.clientSpan());

        span = new Span(null, null);
        Assert.assertFalse(span.serverSpan());
        span = new Span(null, Arrays.asList(cs));
        Assert.assertFalse(span.clientSpan());

        Annotation sr = new Annotation();
        sr.setValue("sr");
        Annotation ss = new Annotation();
        ss.setValue("ss");
        span = new Span(null, Arrays.asList(sr, ss, cs , cr));
        Assert.assertTrue(span.clientSpan());
    }

    private List<Annotation> serverAnnotations() {
        Annotation csAnnotation = new Annotation();
        csAnnotation.setValue("sr");
        csAnnotation.setTimestamp(1);
        csAnnotation.setEndpoint(createEndpoint("hola", "127.0.0.1", (short)8080));

        Annotation crAnnotation = new Annotation();
        crAnnotation.setValue("ss");
        crAnnotation.setTimestamp(2);
        crAnnotation.setEndpoint(createEndpoint("ola", "127.0.0.1", (short)9080));

        return Collections.unmodifiableList(Arrays.asList(csAnnotation, crAnnotation));
    }

    private Endpoint createEndpoint(String serviceName, String ipv4, Short port) {
        Endpoint endpoint = new Endpoint();
        endpoint.setServiceName(serviceName);
        endpoint.setIpv4(ipv4);
        endpoint.setPort(port);

        return endpoint;
    }
    private static Object deserialize(File file) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    private static File serialize(Object obj) throws IOException {
        File temp = File.createTempFile("hawkular-apm-span-serialization", ".tmp");
        temp.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(temp);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(obj);
        oos.close();

        return temp;
    }
}
