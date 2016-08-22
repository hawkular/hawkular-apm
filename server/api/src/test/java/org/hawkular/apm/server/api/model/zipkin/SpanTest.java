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
package org.hawkular.apm.server.api.model.zipkin;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author gbrown
 */
public class SpanTest {

    @Test
    public void testFullUrl() throws MalformedURLException {
        URL url = new URL("http://localhost:8080/my/path");
        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey("http.url");
        ba.setValue(url.toString());
        Span span = new Span(Arrays.asList(ba));

        URL result = span.url();
        assertNotNull(result);
        assertEquals(url, result);
    }

    @Test
    public void testPartialUrl() throws MalformedURLException {
        URL url = new URL("http://localhost:8080/my/path");
        BinaryAnnotation ba = new BinaryAnnotation();
        ba.setKey("http.url");
        ba.setValue(url.getPath());
        Span span = new Span(Arrays.asList(ba));

        URL result = span.url();
        assertNotNull(result);
        assertEquals(url.getPath(), result.getPath());
    }

}
