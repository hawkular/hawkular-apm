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

import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.Property;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class InfinispanPropertyCacheTest extends AbstractInfinispanTest {

    private InfinispanPropertyCache propertyCache;

    @Before
    public void before() {
        propertyCache = new InfinispanPropertyCache(cacheManager);
    }

    @Test
    public void testGetNonExisting() {
        Property property = propertyCache.get(null, "id");
        Assert.assertNull(property);
    }

    @Test
    public void testGetOne() throws CacheException {
        Property<Double> doubleProperty = new Property<>("id", 15D);
        Property<String> stringProperty = new Property<>("id2", "str");

        propertyCache.store(null, doubleProperty);
        propertyCache.store(null, stringProperty);

        Property propertyFromDb = propertyCache.get(null, "id");
        Assert.assertEquals(doubleProperty.getValue(), propertyFromDb.getValue());

        propertyFromDb = propertyCache.get(null, "id2");
        Assert.assertEquals(stringProperty.getValue(), propertyFromDb.getValue());
    }

    @Test
    public void testRemove() throws CacheException {
        propertyCache.remove(null, "id1");
        Assert.assertNull(propertyCache.get(null, "id1"));

        propertyCache.store(null, new Property<>("id", "foo"));
        CountingObserver observer = new CountingObserver();
        propertyCache.addObserver(null, "id", observer);
        propertyCache.store(null, new Property<>("id", "bar"));
        Assert.assertEquals(1, observer.updatedCounter.intValue());

        propertyCache.remove(null, "id");
        Assert.assertNull(propertyCache.get(null, "id"));
    }

    @Test
    public void testSystemProp() {
        System.setProperty("strProp", "foo");
        Assert.assertEquals("foo", propertyCache.get(null, "strProp").getValue());
        System.clearProperty("strProp");

        System.setProperty("intProp", "2");
        Assert.assertEquals("2", propertyCache.get(null, "intProp").getValue());
        System.clearProperty("intProp");
    }

    @Test
    public void testObservableCreated() throws CacheException {
        CountingObserver observer = new CountingObserver();
        propertyCache.addObserver(null, "id1", observer);

        propertyCache.store(null, new Property<>("id1", 2));
        Assert.assertEquals(1, observer.createdCounter.intValue());

        propertyCache.store(null, new Property<>("id1", 22));
        Assert.assertEquals(1, observer.createdCounter.intValue());
    }

    @Test
    public void testObservableUpdated() throws CacheException {
        CountingObserver observer = new CountingObserver();
        propertyCache.addObserver(null, "id1", observer);

        propertyCache.store(null, new Property<>("id1", 2));
        Assert.assertEquals(0, observer.updatedCounter.intValue());

        propertyCache.store(null, new Property<>("id1", 2));
        Assert.assertEquals(0, observer.updatedCounter.intValue());

        propertyCache.store(null, new Property<>("id1", 111));
        Assert.assertEquals(1, observer.updatedCounter.intValue());
    }

    @Test
    public void testObservableRemoved() throws CacheException {
        CountingObserver observer = new CountingObserver();
        propertyCache.addObserver(null, "id1", observer);

        propertyCache.store(null, new Property<>("id1", 2));
        Assert.assertEquals(0, observer.removedCounter.intValue());

        propertyCache.store(null, new Property<>("id1", 111));
        Assert.assertEquals(0, observer.removedCounter.intValue());

        propertyCache.remove(null, "id1");
        Assert.assertEquals(1, observer.removedCounter.intValue());
        propertyCache.remove(null, "id1");
        Assert.assertEquals(1, observer.removedCounter.intValue());

        propertyCache.store(null, new Property<>("id1", 222));
        propertyCache.remove(null, "id1");
        Assert.assertEquals(2, observer.removedCounter.intValue());
    }

    @Test
    public void testObservableOnNonExistingEntry() throws CacheException {
        CountingObserver observer = new CountingObserver();
        propertyCache.addObserver(null, "id1", observer);

        propertyCache.store(null, new Property<>("id1", 33));
        Assert.assertEquals(1, observer.createdCounter.intValue());
        Assert.assertEquals(0, observer.updatedCounter.intValue());
        Assert.assertEquals(0, observer.removedCounter.intValue());
    }

    @Test
    public void testRemoveObserver() throws CacheException {
        CountingObserver observer = new CountingObserver();

        propertyCache.store(null, new Property<>("id1", 2));
        propertyCache.addObserver(null, "id1", observer);

        propertyCache.store(null, new Property<>("id1", 34));
        Assert.assertEquals(1, observer.updatedCounter.intValue());

        propertyCache.removeObserver(null, "id1", observer);
        propertyCache.store(null, new Property<>("id1", 32));
        Assert.assertEquals(1, observer.updatedCounter.intValue());
    }

    private class CountingObserver<T> implements InfinispanPropertyCache.Observer<T> {

        private AtomicInteger createdCounter = new AtomicInteger();
        private AtomicInteger updatedCounter = new AtomicInteger();
        private AtomicInteger removedCounter = new AtomicInteger();

        @Override
        public void created(Property<T> value) {
            createdCounter.getAndIncrement();
        }

        @Override
        public void updated(Property<T> newValue) {
            updatedCounter.getAndIncrement();
        }

        @Override
        public void removed(Property<T> oldValue) {
            removedCounter.getAndIncrement();
        }
    }
}
