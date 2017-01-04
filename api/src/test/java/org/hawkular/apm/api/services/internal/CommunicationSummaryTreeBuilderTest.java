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
package org.hawkular.apm.api.services.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationSummaryTreeBuilderTest {

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
        css2.setCount(1);
        nodes.add(css2);

        CommunicationSummaryStatistics css3 = new CommunicationSummaryStatistics();
        css3.setId("css3");
        nodes.add(css3);

        Collection<CommunicationSummaryStatistics> result =
                CommunicationSummaryTreeBuilder.buildCommunicationSummaryTree(nodes,
                        new HashSet<>(Arrays.asList("css1", "css3")));

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
        assertEquals(1, css1a.getOutbound().get("css2").getNode().getCount());
    }

    @Test
    public void testBuildCommunicationSummaryTreeMissingTarget() {
        List<CommunicationSummaryStatistics> nodes = new ArrayList<CommunicationSummaryStatistics>();

        CommunicationSummaryStatistics css1 = new CommunicationSummaryStatistics();
        css1.setId("css1");
        nodes.add(css1);

        CommunicationSummaryStatistics.ConnectionStatistics con1 =
                new CommunicationSummaryStatistics.ConnectionStatistics();
        css1.getOutbound().put("css2", con1);

        Collection<CommunicationSummaryStatistics> result =
                CommunicationSummaryTreeBuilder.buildCommunicationSummaryTree(nodes,
                        new HashSet<>(Collections.singletonList("css1")));

        assertNotNull(result);
        assertEquals(1, result.size());

        CommunicationSummaryStatistics css1result = result.iterator().next();

        assertEquals(css1.getId(), css1result.getId());
        assertEquals(1, css1.getOutbound().size());
        assertTrue(css1result.getOutbound().containsKey("css2"));

        assertNotNull(css1result.getOutbound().get("css2").getNode());
        assertEquals("css2", css1result.getOutbound().get("css2").getNode().getId());

        // Zero because there is no direct information about this endpoint (e.g. it is
        // not directly monitored)
        assertEquals(0, css1result.getOutbound().get("css2").getNode().getCount());
    }
}
