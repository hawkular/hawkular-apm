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
package org.hawkular.btm.tests.performance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.btxn.NodeType;
import org.hawkular.btm.api.model.events.NodeDetails;
import org.hawkular.btm.api.services.NodeCriteria;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ServicesPerformanceTest {

    private static List<ServicesManager> servicesManager = new ArrayList<ServicesManager>();

    @BeforeClass
    public static void init() throws Exception {
        servicesManager.add(new ServicesManagerElasticsearch().init());
    }

    @AfterClass
    public static void close() throws Exception {
        for (ServicesManager sm : servicesManager) {
            sm.close();
        }
    }

    @Before
    public void initTest() throws Exception {
        for (ServicesManager sm : servicesManager) {
            sm.clear();
        }
    }

    @Test
    public void testStoreAndQueryNodeDetails() throws Exception {
        testStoreAndQueryNodeDetails(1000, 100);
    }

    protected void testStoreAndQueryNodeDetails(int numberOfBatches, int batchSize) throws Exception {
        Map<ServicesManager, Long> initStoreTime = new HashMap<ServicesManager, Long>();
        Map<ServicesManager, Long> storeTime = new HashMap<ServicesManager, Long>();
        Map<ServicesManager, Long> queryNodeSummariesTime = new HashMap<ServicesManager, Long>();
        Map<ServicesManager, Integer> queryNodeSummariesResultSize = new HashMap<ServicesManager, Integer>();
        Map<ServicesManager, Long> queryNodeTimeseriesTime = new HashMap<ServicesManager, Long>();
        Map<ServicesManager, Integer> queryNodeTimeseriesResultSize = new HashMap<ServicesManager, Integer>();

        // Create the number of batches
        for (int i = 0; i < numberOfBatches; i++) {
            List<NodeDetails> nds = new ArrayList<NodeDetails>();

            for (int j = 0; j < batchSize; j++) {
                NodeDetails nd = new NodeDetails();
                nd.setActual((i * batchSize) + j);
                nd.setElapsed((i * batchSize) + j);
                nd.setComponentType("Database");
                nd.setId("id-" + i + "-" + j);
                nd.setTimestamp((i * batchSize) + j + 1);
                nd.setType(NodeType.Component);
                nd.setUri("uri" + j);
                nds.add(nd);
            }

            // Store the batch in each service impl
            for (ServicesManager sm : servicesManager) {
                long startTime = System.currentTimeMillis();
                sm.getAnalyticsService().storeNodeDetails(null, nds);
                long duration = System.currentTimeMillis() - startTime;

                // Store initial measurement separately in case it includes startup delays
                if (storeTime.containsKey(sm)) {
                    long current = storeTime.get(sm);
                    current += duration;
                    storeTime.put(sm, current);
                } else {
                    initStoreTime.put(sm, duration);
                    storeTime.put(sm, 0L);
                }
            }
        }

        NodeCriteria criteria = new NodeCriteria();
        criteria.setStartTime(1);

        for (ServicesManager sm : servicesManager) {
            long startTime = System.currentTimeMillis();
            List<NodeSummaryStatistics> result = sm.getAnalyticsService().getNodeSummaryStatistics(null, criteria);
            long duration = System.currentTimeMillis() - startTime;

            queryNodeSummariesTime.put(sm, duration);
            queryNodeSummariesResultSize.put(sm, result.size());
        }

        for (ServicesManager sm : servicesManager) {
            long startTime = System.currentTimeMillis();
            List<NodeTimeseriesStatistics> result = sm.getAnalyticsService().getNodeTimeseriesStatistics(null,
                    criteria, batchSize);
            long duration = System.currentTimeMillis() - startTime;

            queryNodeTimeseriesTime.put(sm, duration);
            queryNodeTimeseriesResultSize.put(sm, result.size());
        }

        // Report on findings
        System.out.println("\r\n\r\nPerformance Test: Store and Query Node Details");
        System.out.println("==============================================");
        System.out.println("Number of batches = " + numberOfBatches);
        System.out.println("Batch size = " + batchSize);
        System.out.println("----------------------------------------------");

        for (ServicesManager sm : servicesManager) {
            System.out.println("Services Implementation: " + sm.getName() + "\r\n");
            System.out.println("Time to store first node details = " + initStoreTime.get(sm) + "ms");
            System.out.println("Time to store remaining node details = " + storeTime.get(sm) + "ms");
            System.out.println("Time to query node summaries = " + queryNodeSummariesTime.get(sm) + "ms");
            System.out.println("Number of node summaries = " + queryNodeSummariesResultSize.get(sm));
            System.out.println("Time to query node timeseries = " + queryNodeTimeseriesTime.get(sm) + "ms");
            System.out.println("Number of node timeseries = " + queryNodeTimeseriesResultSize.get(sm));
        }
    }

}
