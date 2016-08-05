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

package org.hawkular.apm.server.api.services;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class ObservablePropertyTest {

    @Test
    public void testObservable() {
        ObservableProperty<Double> observableProperty = new ObservableProperty<>("id", 20D);

        DummyObserver dummyObserver = new DummyObserver();
        observableProperty.addObserver(dummyObserver);
        observableProperty.addObserver(dummyObserver);

        observableProperty.updateValue(15D);
        Assert.assertEquals(2, dummyObserver.called.intValue());
    }

    @Test
    public void testRemoveObserver() {
        ObservableProperty<Double> observableProperty = new ObservableProperty<>("id", 20D);

        DummyObserver dummyObserver = new DummyObserver();
        observableProperty.addObserver(dummyObserver);

        observableProperty.updateValue(40D);
        observableProperty.removeObserver(dummyObserver);

        Assert.assertEquals(1, dummyObserver.called.intValue());
        observableProperty.updateValue(1D);
        Assert.assertEquals(1, dummyObserver.called.intValue());
    }

    private class DummyObserver implements ObservableProperty.Observer {

        private AtomicInteger called = new AtomicInteger();

        @Override
        public void update() {
            called.incrementAndGet();
        }
    }
}
