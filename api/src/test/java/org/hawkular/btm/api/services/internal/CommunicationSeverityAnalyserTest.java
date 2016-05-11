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
package org.hawkular.btm.api.services.internal;

import static org.junit.Assert.assertEquals;

import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.junit.Test;

/**
 * @author gbrown
 */
public class CommunicationSeverityAnalyserTest {

    @Test
    public void testDeriveSeverityMax() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setAverageDuration(1000);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max);

        assertEquals(5, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMin() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setAverageDuration(100);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max);

        assertEquals(0, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityOverMax() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setAverageDuration(1300);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max);

        assertEquals(5, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMid1() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setAverageDuration(700);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max);

        assertEquals(4, css.getSeverity());
    }

    @Test
    public void testDeriveSeverityMid2() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setAverageDuration(500);

        long max = 1000;

        CommunicationSeverityAnalyser.deriveSeverity(css, max);

        assertEquals(3, css.getSeverity());
    }


    @Test
    public void testDeriveSeverityMid3() {
        CommunicationSummaryStatistics css = new CommunicationSummaryStatistics();
        css.setAverageDuration(597);

        long max = 1789;

        CommunicationSeverityAnalyser.deriveSeverity(css, max);

        assertEquals(2, css.getSeverity());
    }
}
