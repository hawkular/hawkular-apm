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
package org.hawkular.apm.tests.client.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class ClientCamelSplitterNotParallelTest extends ClientCamelTestBase {

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
        try {
            synchronized (this) {
                wait(5000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        for (Trace trace : getApmMockServer().getTraces()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        Trace trace = getApmMockServer().getTraces().get(0);

        List<Consumer> consumers = new ArrayList<Consumer>();
        NodeUtil.findNodes(trace.getNodes(), Consumer.class, consumers);

        assertTrue("Should be no consumers", consumers.isEmpty());

        List<Producer> producers = new ArrayList<Producer>();
        NodeUtil.findNodes(trace.getNodes(), Producer.class, producers);

        assertTrue("Should be no producers", producers.isEmpty());
    }

}
