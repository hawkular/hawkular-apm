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

package org.hawkular.apm.server.api.utils.zipkin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class SpanUniqueIdGeneratorTest {

    @Test
    public void testToUnique() {
        String originalId = "064c292e02db027c";

        Span span = new Span(null, clientAnnotations());
        span.setId(originalId);

        String uniqueID = SpanUniqueIdGenerator.toUnique(span);
        Assert.assertEquals(originalId + SpanUniqueIdGenerator.CLIENT_ID_SUFFIX, uniqueID);
    }

    @Test
    public void testToOriginal() {
        String originalId = "064c292e02db027c22g";

        Span span = new Span(null, clientAnnotations());
        span.setId(originalId);

        span.setId(span.getId() + SpanUniqueIdGenerator.CLIENT_ID_SUFFIX);
        Assert.assertEquals(originalId, SpanUniqueIdGenerator.toOriginal(span));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalState() {
        String originalId = "064c292e02db027c22g";

        Span span = new Span(null, clientAnnotations());
        span.setId(originalId);

        String uniqueID = SpanUniqueIdGenerator.toUnique(span);
        span.setId(uniqueID);

        SpanUniqueIdGenerator.toUnique(span);
    }

    private List<Annotation> clientAnnotations() {
        Annotation csAnnotation = new Annotation();
        csAnnotation.setValue("cs");
        Annotation crAnnotation = new Annotation();
        crAnnotation.setValue("cr");

        return Collections.unmodifiableList(Arrays.asList(csAnnotation, crAnnotation));
    }
}
