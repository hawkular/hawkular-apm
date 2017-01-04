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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationSeverityAnalyserTest {

    @Test
    public void testDeriveSeverityMax() {
        Map<String,CommunicationSummaryStatistics> nodes=new HashMap<String,CommunicationSummaryStatistics>();
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setId("css1");
        css.setAverageDuration(1000);
        nodes.put(css.getId(), css);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max, nodes);

        assertEquals(5, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMin() {
        Map<String,CommunicationSummaryStatistics> nodes=new HashMap<String,CommunicationSummaryStatistics>();
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setId("css1");
        css.setAverageDuration(100);
        nodes.put(css.getId(), css);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max, nodes);

        assertEquals(0, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityOverMax() {
        Map<String,CommunicationSummaryStatistics> nodes=new HashMap<String,CommunicationSummaryStatistics>();
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setId("css1");
        css.setAverageDuration(1300);
        nodes.put(css.getId(), css);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max, nodes);

        assertEquals(5, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMid1() {
        Map<String,CommunicationSummaryStatistics> nodes=new HashMap<String,CommunicationSummaryStatistics>();
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setId("css1");
        css.setAverageDuration(700);
        nodes.put(css.getId(), css);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max, nodes);

        assertEquals(4, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMid2() {
        Map<String,CommunicationSummaryStatistics> nodes=new HashMap<String,CommunicationSummaryStatistics>();
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setId("css1");
        css.setAverageDuration(500);
        nodes.put(css.getId(), css);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max, nodes);

        assertEquals(3, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMid3() {
        Map<String,CommunicationSummaryStatistics> nodes=new HashMap<String,CommunicationSummaryStatistics>();
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setId("css1");
        css.setAverageDuration(597);
        nodes.put(css.getId(), css);

        long max = 1789;

        CommunicationSeverityAnalyser.deriveSeverity(css, max, nodes);

        assertEquals(2, css.getSeverity());
    }

    @Test
    public void testEvaluateCommunicationSummarySeverityTree() {
        List<CommunicationSummaryStatistics> nodes=new ArrayList<CommunicationSummaryStatistics>();

        CommunicationSummaryStatistics css1 = new CommunicationSummaryStatistics();
        css1.setId("css1");
        css1.setAverageDuration(1789);
        CommunicationSummaryStatistics.ConnectionStatistics cs1 =
                new CommunicationSummaryStatistics.ConnectionStatistics();
        css1.getOutbound().put("css2", cs1);
        nodes.add(css1);

        CommunicationSummaryStatistics css2 = new CommunicationSummaryStatistics();
        css2.setId("css2");
        css2.setAverageDuration(597);
        nodes.add(css2);

        CommunicationSeverityAnalyser analyser = new CommunicationSeverityAnalyser();
        analyser.evaluateCommunicationSummarySeverity(nodes);

        assertEquals(5, css1.getSeverity());
        assertEquals(2, css2.getSeverity());
    }
}
