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
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class BinaryAnnotationMappingDeriverTest {

    @Test
    public void testMappingFromTestJson() {
        BinaryAnnotationMappingDeriver.clearStorage();

        String testResourcesPath = getClass().getClassLoader().getResource(".").getPath();
        BinaryAnnotationMappingDeriver mappingDeriver =
                BinaryAnnotationMappingDeriver.getInstance(testResourcesPath + "test-binary-annotations-mapping.json");

        BinaryAnnotation fooBA = new BinaryAnnotation();
        fooBA.setKey("foo");
        fooBA.setValue("foo-value");

        BinaryAnnotation barBA = new BinaryAnnotation();
        barBA.setKey("bar");
        barBA.setValue("bar-value");

        BinaryAnnotation withoutMappingBA = new BinaryAnnotation();
        withoutMappingBA.setKey("key.no.mapping");
        withoutMappingBA.setValue("no-mapping-value");

        BinaryAnnotation ignoredBA = new BinaryAnnotation();
        ignoredBA.setKey("ignore.key");
        ignoredBA.setValue("ignore-value");

        List<BinaryAnnotation> binaryAnnotations = Arrays.asList(fooBA, barBA, ignoredBA, withoutMappingBA);

        MappingResult mappingResult = mappingDeriver.mappingResult(binaryAnnotations);

        Assert.assertEquals("foo.modified", mappingResult.getComponentType());
        Assert.assertEquals("foo.endpoint", mappingResult.getEndpointType());
        Assert.assertEquals(new HashSet<>(Arrays.asList(
                    new Property("foo.prop", "foo-value"),
                    new Property("key.no.mapping", "no-mapping-value"),
                    new Property("bar", "bar-value"))),
                new HashSet<>(mappingResult.getProperties()));

        BinaryAnnotationMappingDeriver.clearStorage();
    }
}
