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
package org.hawkular.apm.api.model.analytics;

import static org.junit.Assert.assertEquals;

import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics.ConnectionStatistics;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationSummaryStatisticsTest {

    @Test
    public void testCopy() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();

        css.setAverageDuration(5);
        css.setCount(4);
        css.setId("myid");
        css.setMaximumDuration(65);
        css.setMinimumDuration(23);
        css.setOperation("myop");
        css.setSeverity(4);
        css.setUri("myuri");

        ConnectionStatistics cs = new ConnectionStatistics();
        cs.setAverageLatency(65);
        cs.setCount(3);
        cs.setMaximumLatency(766);
        cs.setMinimumLatency(23);
        cs.setSeverity(6);

        CommunicationSummaryStatistics css2 = new CommunicationSummaryStatistics();

        css2.setId("id2");
        cs.setNode(css2);

        css.getOutbound().put("out1", cs);

        CommunicationSummaryStatistics cssCopy = new CommunicationSummaryStatistics(css);

        assertEquals(css, cssCopy);
    }

}
