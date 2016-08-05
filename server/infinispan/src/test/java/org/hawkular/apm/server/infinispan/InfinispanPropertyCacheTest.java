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

package org.hawkular.apm.server.infinispan;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.ObservableProperty;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class InfinispanPropertyCacheTest extends AbstractInfinispanTest {

    @Test
    public void testGetNonExisting() {
        InfinispanPropertyCache cache = createCache();

        ObservableProperty observableProperty = cache.get(null, "id");
        Assert.assertNull(observableProperty);
    }

    @Test
    public void testGetOne() throws CacheException {
        InfinispanPropertyCache cache = createCache();

        ObservableProperty<Double> doubleObservableProperty = new ObservableProperty<>("id", 15D);
        ObservableProperty<String> stringObservableProperty = new ObservableProperty<>("id2", "str");

        cache.store(null, Arrays.asList(doubleObservableProperty, stringObservableProperty));

        ObservableProperty propertyFromDb = cache.get(null, "id");
        Assert.assertEquals(doubleObservableProperty.getValue(), propertyFromDb.getValue());

        propertyFromDb = cache.get(null, "id2");
        Assert.assertEquals(stringObservableProperty.getValue(), propertyFromDb.getValue());
    }

    @Test
    public void testSystemProp() {
        InfinispanPropertyCache cache = createCache();

        System.setProperty("strProp", "foo");
        ObservableProperty<?> observableProperty = cache.get(null, "strProp");
        Assert.assertEquals("foo", observableProperty.getValue());
        System.clearProperty("strProp");

        System.setProperty("intProp", "2");
        observableProperty = cache.get(null, "intProp");
        Assert.assertEquals("2", observableProperty.getValue());
        System.clearProperty("intProp");
    }

    @Test
    public void testObservablePropertyChanges() throws CacheException {
        InfinispanPropertyCache cache = createCache();

        DummyObserver observer = new DummyObserver();

        ObservableProperty<Integer> property = new ObservableProperty<>("id1", 2, Collections.singleton(observer));
        cache.store(null, Arrays.asList(property));

        cache.get(null, "id1").updateValue(2);
        cache.get(null, "id1").updateValue(5);

        Assert.assertEquals(2, observer.called.intValue());
    }

    @Test
    public void testStore() throws CacheException {
        InfinispanPropertyCache cache = createCache();

        DummyObserver observer = new DummyObserver();
        ObservableProperty<Integer> property = new ObservableProperty<>("id1", 2, Collections.singleton(observer));
        cache.store(null, Arrays.asList(property));

        DummyObserver observer2 = new DummyObserver();
        ObservableProperty<Integer> property2 = new ObservableProperty<>("id1", 3, Collections.singleton(observer2));

        cache.store(null, Arrays.asList(property2));

        ObservableProperty propFromCache = cache.get(null, "id1");
        Assert.assertEquals(3, propFromCache.getValue());
        Assert.assertEquals(1, observer.called.intValue());
        Assert.assertEquals(0, observer2.called.intValue());
    }

    private InfinispanPropertyCache createCache() {
        InfinispanPropertyCache propertyCache = new InfinispanPropertyCache(
                cacheManager.getCache(InfinispanCommunicationDetailsCache.CACHE_NAME + UUID.randomUUID()));

        return propertyCache;
    }

    private class DummyObserver implements ObservableProperty.Observer {

        private AtomicInteger called = new AtomicInteger();

        @Override
        public void update() {
            called.incrementAndGet();
        }
    }
}
