/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.tests.client.camel;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.tests.btxn.TestBTxnService;
import org.junit.After;
import org.junit.Before;

/**
 * @author gbrown
 */
public abstract class ClientCamelTestBase {

    private CamelContext context = new DefaultCamelContext();

    private TestBTxnService btxnService = new TestBTxnService();

    @Before
    public void init() {
        try {
            initContext(context);

            context.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            btxnService.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method initialises the camel context.
     *
     * @param context The camel context
     * @throws Exception Failed to initialise
     */
    protected void initContext(CamelContext context) throws Exception {
        context.addRoutes(getRouteBuilder());
    }

    /**
     * This method defines the route to be tested.
     *
     * @return The route builder
     */
    protected abstract RouteBuilder getRouteBuilder();

    @After
    public void close() {
        try {
            context.stop();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            btxnService.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the btxnService
     */
    public TestBTxnService getBtxnService() {
        return btxnService;
    }

    /**
     * @param btxnService the btxnService to set
     */
    public void setBtxnService(TestBTxnService btxnService) {
        this.btxnService = btxnService;
    }

    /**
     * This method checks that two correlation identifiers are equivalent.
     *
     * @param producer The producer
     * @param consumer The consumer
     */
    protected void checkInteractionCorrelationIdentifiers(Producer producer, Consumer consumer) {
        CorrelationIdentifier pcid = producer.getCorrelationIds().iterator().next();
        CorrelationIdentifier ccid = consumer.getCorrelationIds().iterator().next();

        assertEquals(pcid, ccid);
    }

    /**
     * This method finds nodes within a hierarchy of the required type.
     *
     * @param nodes The nodes to recursively check
     * @param cls The class of interest
     * @param results The results
     */
    @SuppressWarnings("unchecked")
    protected <T extends Node> void findNodes(List<Node> nodes, Class<T> cls, List<T> results) {
        for (Node n : nodes) {
            if (n instanceof ContainerNode) {
                findNodes(((ContainerNode) n).getNodes(), cls, results);
            }

            if (cls.isAssignableFrom(n.getClass())) {
                results.add((T) n);
            }
        }
    }
}
