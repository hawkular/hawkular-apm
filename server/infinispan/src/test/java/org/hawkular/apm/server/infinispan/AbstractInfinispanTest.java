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

package org.hawkular.apm.server.infinispan;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractInfinispanTest {

    protected static DefaultCacheManager cacheManager;

    @BeforeClass
    public static void initClass() {
        Configuration configuration = new ConfigurationBuilder().invocationBatching().enable().build();
        cacheManager = new DefaultCacheManager(configuration);
    }

    @AfterClass
    public static void voidDestroyClass() {
        cacheManager.stop();
    }

    @After
    public void after() {
        cacheManager.getCacheNames().forEach(cacheManager::removeCache);
    }
}
