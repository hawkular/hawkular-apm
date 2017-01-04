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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class BinaryAnnotationMappingStorageTest {

    @BeforeClass
    public static void beforeClass() {
        String testResourcesPath = BinaryAnnotationMappingStorageTest.class.getClassLoader().getResource(".").getPath();
        System.setProperty("jboss.server.config.dir", testResourcesPath);
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("jboss.server.config.dir");
    }

    @Test
    public void testJBossDir() {
        BinaryAnnotationMappingStorage baStorage = new BinaryAnnotationMappingStorage();

        Assert.assertEquals(1, baStorage.getKeyBasedMappings().size());
        Assert.assertEquals(new HashSet<>(Arrays.asList("foo")), baStorage.getKeyBasedMappings().keySet());
    }

}
