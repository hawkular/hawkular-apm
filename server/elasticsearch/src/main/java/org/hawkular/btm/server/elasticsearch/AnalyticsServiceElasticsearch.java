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
package org.hawkular.btm.server.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.MetricsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentile;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles;
import org.hawkular.btm.api.model.analytics.BusinessTransactionStats;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.server.elasticsearch.log.MsgLogger;

/**
 * This class provides the Elasticsearch implementation of the Analytics
 * Service.
 *
 * @author gbrown
 */
@Singleton
public class AnalyticsServiceElasticsearch implements AnalyticsService {

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    /**  */
    private static final String RESPONSE_TIME_TYPE = "responsetime";

    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        client = new ElasticsearchClient();
        try {
            client.init();
        } catch (Exception e) {
            msgLog.errorFailedToInitialiseElasticsearchClient(e);
        }
    }

    protected ElasticsearchClient getElasticsearchClient() {
        return client;
    }

    protected void setElasticsearchClient(ElasticsearchClient client) {
        this.client = client;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getUnboundURIs(java.lang.String, long, long)
     */
    @Override
    public List<String> getUnboundURIs(String tenantId, long startTime, long endTime) {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria()
                .setStartTime(startTime)
                .setEndTime(endTime);

        List<BusinessTransaction> fragments = BusinessTransactionServiceElasticsearch.internalQuery(client,
                tenantId, criteria);

        // Process the fragments to identify which URIs are no used in any business transaction
        Set<String> unboundURIs = new HashSet<String>();
        Set<String> boundURIs = new HashSet<String>();

        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);
            analyseURIs(btxn.getName() != null, btxn.getNodes(), unboundURIs, boundURIs);
        }

        // Remove any URIs that may subsequently have become bound
        unboundURIs.removeAll(boundURIs);

        // Convert the set to a sorted list
        List<String> ret = new ArrayList<String>(unboundURIs);

        Collections.sort(ret);

        return ret;
    }

    /**
     * This method collects the information regarding bound and unbound URIs.
     *
     * @param bound Whether the business transaction fragment being processed is bound
     * @param nodes The nodes
     * @param unboundURIs The list of unbound URIs
     * @param boundURIs The list of bound URIs
     */
    protected void analyseURIs(boolean bound, List<Node> nodes, Set<String> unboundURIs,
            Set<String> boundURIs) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node.getUri() != null) {
                if (bound) {
                    boundURIs.add(node.getUri());
                } else {
                    unboundURIs.add(node.getUri());
                }
            }

            if (node instanceof ContainerNode) {
                analyseURIs(bound, ((ContainerNode) node).getNodes(),
                        unboundURIs, boundURIs);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getTransactionCount(java.lang.String,
     *                          java.lang.String, long, long)
     */
    @Override
    public long getTransactionCount(String tenantId, String name, long startTime, long endTime) {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria()
                .setName(name)
                .setStartTime(startTime)
                .setEndTime(endTime);

        // TODO: Need to distinguish initial/top level fragments

        return BusinessTransactionServiceElasticsearch.internalQuery(client,
                tenantId, criteria).size();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getTransactionFaultCount(java.lang.String,
     *                          java.lang.String, long, long)
     */
    @Override
    public long getTransactionFaultCount(String tenantId, String name, long startTime, long endTime) {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria()
                .setName(name)
                .setStartTime(startTime)
                .setEndTime(endTime);

        // TODO: Need to distinguish initial/top level fragments

        List<BusinessTransaction> btxns = BusinessTransactionServiceElasticsearch.internalQuery(client,
                tenantId, criteria);

        long ret = 0;

        for (int i = 0; i < btxns.size(); i++) {
            BusinessTransaction btxn = btxns.get(i);
            if (btxn.getNodes().size() > 0) {
                // Check for fault in any top level node
                for (int j = 0; j < btxn.getNodes().size(); j++) {
                    Node node = btxn.getNodes().get(j);
                    if (node.getFault() != null && node.getFault().trim().length() > 0) {
                        ret++;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getStats(java.lang.String,
     *                  org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public BusinessTransactionStats getStats(String tenantId, BusinessTransactionCriteria criteria) {
        if (criteria.getName() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        RefreshRequestBuilder refreshRequestBuilder =
                client.getElasticsearchClient().admin().indices().prepareRefresh(index);
        client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

        long startTime = criteria.getStartTime();
        long endTime = criteria.getEndTime();

        if (endTime == 0) {
            endTime = System.currentTimeMillis();
        }

        if (startTime == 0) {
            // Set to 1 hour before end time
            startTime = endTime - 3600000;
        }

        BoolQueryBuilder b2 = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("timestamp").from(startTime).to(endTime));

        b2 = b2.must(QueryBuilders.termQuery("businessTransaction", criteria.getName()));

        if (!criteria.getProperties().isEmpty()) {
            for (String key : criteria.getProperties().keySet()) {
                b2 = b2.must(QueryBuilders.matchQuery("properties." + key, criteria.getProperties().get(key)));
            }
        }

        MetricsAggregationBuilder percentileAgg = AggregationBuilders
                .percentiles("percentiles")
                .field("duration");

        MetricsAggregationBuilder averageAgg = AggregationBuilders
                .avg("average")
                .field("duration");

        SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(RESPONSE_TIME_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addAggregation(averageAgg)
                .addAggregation(percentileAgg)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setQuery(b2);

        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
        }

        BusinessTransactionStats stats = new BusinessTransactionStats();

        Percentiles agg = response.getAggregations().get("percentiles");
        for (Percentile entry : agg) {
            stats.addPercentile((int)entry.getPercent(), entry.getValue());
        }

        Avg avg = response.getAggregations().get("average");
        stats.setAverage(avg.getValue());

        return stats;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getAlertCount(java.lang.String, java.lang.String)
     */
    @Override
    public int getAlertCount(String tenantId, String name) {
        return 0;
    }

    /**
     * This method clears the Elasticsearch database, and is currently only intended for
     * testing purposes.
     *
     * @param tenantId The optional tenant id
     */
    protected void clear(String tenantId) {
        String index = client.getIndex(tenantId);

        client.getElasticsearchClient().admin().indices().prepareDelete(index).execute().actionGet();
    }

    /**
     * This method closes the Elasticsearch client.
     */
    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
        }
    }

}
