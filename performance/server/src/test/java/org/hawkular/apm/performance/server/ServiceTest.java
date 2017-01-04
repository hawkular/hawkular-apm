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
package org.hawkular.apm.performance.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ServiceTest {

    private static final String TXN_NAME = "TestBTxnName";

    @Test
    public void testBusinessTxnNamePropagated() {
        TestTracePublisher publisher = (TestTracePublisher) ServiceResolver.getSingletonService(TracePublisher.class);

        TestServiceRegistry serviceRegistry = new TestServiceRegistry();

        Service serviceB = new Service("serviceB", "/service/b", null, serviceRegistry, null);
        serviceRegistry.setServiceInstance(serviceB);

        Service serviceA = new Service("serviceA", "/service/a", null, serviceRegistry, new HashMap<String, String>());

        serviceA.getCalledServices().put("path1", "serviceB");

        serviceA.getCollector().setTransaction(null, TXN_NAME);

        // Check no traces currently reported
        assertTrue(publisher.getTraces().isEmpty());

        Message mesg = new Message("path1");
        serviceA.call(mesg, "1", null);

        Wait.until(() -> publisher.getTraces().size() == 2);

        // Check that two traces reported, both with transaction name
        assertEquals(2, publisher.getTraces().size());
        assertEquals(TXN_NAME, publisher.getTraces().get(0).getTransaction());
        assertEquals(TXN_NAME, publisher.getTraces().get(1).getTransaction());
    }

    public class TestServiceRegistry implements ServiceRegistry {

        private Service service;

        public TestServiceRegistry() {
        }

        public void setServiceInstance(Service service) {
            this.service = service;
        }

        @Override
        public Service getServiceInstance(String name) {
            return service;
        }

        @Override
        public void returnServiceInstance(Service service) {
        }

    }

}
