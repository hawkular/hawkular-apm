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
package org.hawkular.apm.api.services.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationSummaryTreeBuilderTest {

    @Test
    public void testGetRootCommunicationSummaryNodes() {
        Map<String, CommunicationSummaryStatistics> nodeMap = new HashMap<String, CommunicationSummaryStatistics>();

        CommunicationSummaryStatistics css1 = new CommunicationSummaryStatistics();
        css1.setId("css1");
        nodeMap.put(css1.getId(), css1);

        CommunicationSummaryStatistics.ConnectionStatistics con1 =
                new CommunicationSummaryStatistics.ConnectionStatistics();
        css1.getOutbound().put("css2", con1);

        CommunicationSummaryStatistics css2 = new CommunicationSummaryStatistics();
        css2.setId("css2");
        nodeMap.put(css2.getId(), css2);

        CommunicationSummaryStatistics css3 = new CommunicationSummaryStatistics();
        css3.setId("css3");
        nodeMap.put(css3.getId(), css3);

        Collection<CommunicationSummaryStatistics> result =
                CommunicationSummaryTreeBuilder.getRootCommunicationSummaryNodes(nodeMap);

        assertNotNull(result);
        assertEquals(2, result.size());

        for (CommunicationSummaryStatistics css : result) {
            assertNotEquals("css2", css.getId());
        }
    }

    @Test
    public void testBuildCommunicationSummaryTree() {
        List<CommunicationSummaryStatistics> nodes = new ArrayList<CommunicationSummaryStatistics>();

        CommunicationSummaryStatistics css1 = new CommunicationSummaryStatistics();
        css1.setId("css1");
        nodes.add(css1);

        CommunicationSummaryStatistics.ConnectionStatistics con1 =
                new CommunicationSummaryStatistics.ConnectionStatistics();
        css1.getOutbound().put("css2", con1);

        CommunicationSummaryStatistics css2 = new CommunicationSummaryStatistics();
        css2.setId("css2");
        nodes.add(css2);

        CommunicationSummaryStatistics css3 = new CommunicationSummaryStatistics();
        css3.setId("css3");
        nodes.add(css3);

        Collection<CommunicationSummaryStatistics> result =
                CommunicationSummaryTreeBuilder.buildCommunicationSummaryTree(nodes);

        assertNotNull(result);
        assertEquals(2, result.size());

        CommunicationSummaryStatistics css1a = null;
        CommunicationSummaryStatistics css3a = null;

        for (CommunicationSummaryStatistics css : result) {
            if (css.getId().equals("css1")) {
                css1a = css;
            } else if (css.getId().equals("css3")) {
                css3a = css;
            } else {
                fail("Unexpected id: " + css.getId());
            }
        }

        assertEquals(1, css1a.getOutbound().size());
        assertTrue(css1a.getOutbound().containsKey("css2"));
        assertTrue(css3a.getOutbound().isEmpty());

        assertNotNull(css1a.getOutbound().get("css2").getNode());
        assertEquals("css2", css1a.getOutbound().get("css2").getNode().getId());
    }

}
