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
package org.hawkular.apm.tests.client.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ClientCamelSplitterNotParallelITest extends ClientCamelITestBase {

    @Override
    public RouteBuilder getRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:src/test/data/camel/splitter?noop=true")
                        .split()
                        .xpath("/order/item")
                        .setHeader("LineItemId")
                        .xpath("/item/@id", String.class)
                        .to("file:target/data/camel/splitter?fileName="
                                + "${in.header.LineItemId}-${date:now:yyyyMMddHHmmssSSSSS}.xml");
            }
        };
    }

    @Test
    public void testFileSplitNotParallel() {
        getApmMockServer().getTraces();
        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        Trace trace = getApmMockServer().getTraces().get(0);

        List<Consumer> consumers = NodeUtil.findNodes(trace.getNodes(), Consumer.class);

        assertTrue("Should be no consumers", consumers.isEmpty());

        List<Producer> producers = NodeUtil.findNodes(trace.getNodes(), Producer.class);

        assertTrue("Should be no producers", producers.isEmpty());
    }

}
