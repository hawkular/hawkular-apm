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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ClientCamelSplitterParallelITest extends ClientCamelITestBase {

    @Override
    public RouteBuilder getRouteBuilder() {
        XPathBuilder xPathBuilder = new XPathBuilder("/order/item");

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:src/test/data/camel/splitter?noop=true")
                .split(xPathBuilder)
                .parallelProcessing()
                .setHeader("LineItemId")
                .xpath("/item/@id", String.class)
                .to("file:target/data/camel/splitter?fileName="
                        + "${in.header.LineItemId}-${date:now:yyyyMMddHHmmssSSSSS}.xml");
            }
        };
    }

    @Test
    public void testFileSplitNotParallel() {
        Wait.until(() -> getApmMockServer().getTraces().size() == 6);
        List<Trace> btxns = getApmMockServer().getTraces();

        // Check stored traces (including 1 for the test client)
        assertEquals(6, btxns.size());

        Trace parent=null;
        Producer producer=null;
        List<Trace> spawned=new ArrayList<Trace>();

        for (Trace trace : btxns) {
            List<Consumer> consumers = NodeUtil.findNodes(trace.getNodes(), Consumer.class);

            List<Producer> producers = NodeUtil.findNodes(trace.getNodes(), Producer.class);

            if (consumers.isEmpty()) {
                if (producers.isEmpty()) {
                    fail("Expected producer");
                }
                if (producers.size() > 1) {
                    fail("Expected only 1 producer");
                }
                if (parent != null) {
                    fail("Already have a producer btxn");
                }
                parent = trace;
                producer = producers.get(0);
            } else if (!producers.isEmpty()) {
                fail("Should not have both consumers and producer");
            } else if (consumers.size() > 1) {
                fail("Only 1 consumer expected per btxn, got: "+consumers.size());
            } else {
                spawned.add(trace);
            }
        }

        assertEquals(5, spawned.size());

        assertNotNull(parent);

        // Check 'apm_publish' set on producer
        assertTrue(producer.hasProperty("apm_publish"));
        assertEquals(producer.getProperties("apm_publish").iterator().next().getValue(), "true");
    }

}
