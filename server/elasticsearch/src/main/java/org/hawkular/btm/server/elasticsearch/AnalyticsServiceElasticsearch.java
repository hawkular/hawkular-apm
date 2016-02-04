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
package org.hawkular.btm.server.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram.Bucket;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.missing.MissingBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.avg.AvgBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentile;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsBuilder;
import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics.NodeComponentTypeStatistics;
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.events.CompletionTime;
import org.hawkular.btm.api.model.events.NodeDetails;
import org.hawkular.btm.api.services.AbstractAnalyticsService;
import org.hawkular.btm.api.services.Criteria;
import org.hawkular.btm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the Elasticsearch implementation of the Analytics
 * Service.
 *
 * @author gbrown
 */
public class AnalyticsServiceElasticsearch extends AbstractAnalyticsService {

    private static final Logger log = Logger.getLogger(AnalyticsServiceElasticsearch.class.getName());

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    /**  */
    private static final String NODE_DETAILS_TYPE = "nodedetails";

    /**  */
    private static final String COMPLETION_TIME_TYPE = "completiontime";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    private ElasticsearchClient client;

    /**
     * This method gets the elasticsearch client.
     *
     * @return The elasticsearch client
     */
    public ElasticsearchClient getElasticsearchClient() {
        return client;
    }

    /**
     * This method sets the elasticsearch client.
     *
     * @param client The elasticsearch client
     */
    public void setElasticsearchClient(ElasticsearchClient client) {
        this.client = client;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AbstractAnalyticsService#getFragments(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria)
     */
    @Override
    protected List<BusinessTransaction> getFragments(String tenantId, Criteria criteria) {
        return BusinessTransactionServiceElasticsearch.internalQuery(client,
                tenantId, criteria);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionCount(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public long getCompletionCount(String tenantId, Criteria criteria) {
        if (criteria.getBusinessTransaction() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
                return 0;
            } else {
                return response.getHits().getTotalHits();
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion count");
            }
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionFaultCount(java.lang.String,
     *              org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public long getCompletionFaultCount(String tenantId, Criteria criteria) {
        if (criteria.getBusinessTransaction() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            FilterBuilder filter = FilterBuilders.existsFilter("fault");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query)
                    .setPostFilter(filter);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
                return 0;
            } else {
                return response.getHits().getTotalHits();
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion faultcount");
            }
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getStats(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public Percentiles getCompletionPercentiles(String tenantId, Criteria criteria) {
        if (criteria.getBusinessTransaction() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        Percentiles percentiles = new Percentiles();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            PercentilesBuilder percentileAgg = AggregationBuilders
                    .percentiles("percentiles")
                    .field("duration");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(percentileAgg)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles agg =
                    response.getAggregations().get("percentiles");

            for (Percentile entry : agg) {
                percentiles.addPercentile((int) entry.getPercent(), entry.getValue());
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion percentiles");
            }
        }

        return percentiles;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionStatistics(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria, long)
     */
    @Override
    public List<CompletionTimeseriesStatistics> getCompletionTimeseriesStatistics(String tenantId,
            Criteria criteria, long interval) {
        if (criteria.getBusinessTransaction() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        List<CompletionTimeseriesStatistics> stats = new ArrayList<CompletionTimeseriesStatistics>();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            StatsBuilder statsBuilder = AggregationBuilders
                    .stats("stats")
                    .field("duration");

            MissingBuilder faultCountBuilder = AggregationBuilders
                    .missing("faults")
                    .field("fault");

            DateHistogramBuilder histogramBuilder = AggregationBuilders
                    .dateHistogram("histogram")
                    .interval(interval)
                    .field("timestamp")
                    .subAggregation(statsBuilder)
                    .subAggregation(faultCountBuilder);

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(histogramBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            DateHistogram histogram = response.getAggregations().get("histogram");

            for (Bucket bucket : histogram.getBuckets()) {
                Stats stat = bucket.getAggregations().get("stats");
                Missing missing = bucket.getAggregations().get("faults");

                CompletionTimeseriesStatistics s = new CompletionTimeseriesStatistics();
                s.setTimestamp(bucket.getKeyAsDate().getMillis());
                s.setAverage(stat.getAvg());
                s.setMin(stat.getMin());
                s.setMax(stat.getMax());
                s.setCount(stat.getCount());
                s.setFaultCount(stat.getCount() - missing.getDocCount());

                stats.add(s);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion timeseries stats");
            }
        }

        return stats;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionFaultDetails(java.lang.String,
     *              org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public List<Cardinality> getCompletionFaultDetails(String tenantId, Criteria criteria) {
        if (criteria.getBusinessTransaction() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        List<Cardinality> ret = new ArrayList<Cardinality>();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            TermsBuilder cardinalityBuilder = AggregationBuilders
                    .terms("cardinality")
                    .field("fault")
                    .order(Order.aggregation("_count", false))
                    .size(criteria.getMaxResponseSize());

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(cardinalityBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Terms terms = response.getAggregations().get("cardinality");

            for (Terms.Bucket bucket : terms.getBuckets()) {
                Cardinality card = new Cardinality();
                card.setValue(bucket.getKey());
                card.setCount(bucket.getDocCount());
                ret.add(card);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion fault details");
            }
        }

        Collections.sort(ret, new Comparator<Cardinality>() {
            @Override
            public int compare(Cardinality arg0, Cardinality arg1) {
                return (int) (arg1.getCount() - arg0.getCount());
            }
        });

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionPropertyDetails(java.lang.String,
     *              org.hawkular.btm.api.services.Criteria, java.lang.String)
     */
    @Override
    public List<Cardinality> getCompletionPropertyDetails(String tenantId, Criteria criteria,
            String property) {
        if (criteria.getBusinessTransaction() == null) {
            throw new IllegalArgumentException("Business transaction name not specified");
        }

        String index = client.getIndex(tenantId);

        List<Cardinality> ret = new ArrayList<Cardinality>();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            TermsBuilder cardinalityBuilder = AggregationBuilders
                    .terms("cardinality")
                    .field("properties." + property)
                    .order(Order.aggregation("_count", false))
                    .size(criteria.getMaxResponseSize());

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(cardinalityBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Terms terms = response.getAggregations().get("cardinality");

            for (Terms.Bucket bucket : terms.getBuckets()) {
                Cardinality card = new Cardinality();
                card.setValue(bucket.getKey());
                card.setCount(bucket.getDocCount());
                ret.add(card);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion property details");
            }
        }

        Collections.sort(ret, new Comparator<Cardinality>() {
            @Override
            public int compare(Cardinality arg0, Cardinality arg1) {
                return arg0.getValue().compareTo(arg1.getValue());
            }
        });

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getAlertCount(java.lang.String, java.lang.String)
     */
    @Override
    public int getAlertCount(String tenantId, String name) {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getNodeStatistics(java.lang.String,
     *                      org.hawkular.btm.api.services.Criteria, long)
     */
    @Override
    public List<NodeTimeseriesStatistics> getNodeTimeseriesStatistics(String tenantId, Criteria criteria,
            long interval) {
        String index = client.getIndex(tenantId);

        List<NodeTimeseriesStatistics> stats = new ArrayList<NodeTimeseriesStatistics>();

        long queryTime = 0;
        int numOfNodes = 0;
        if (log.isLoggable(Level.FINEST)) {
            queryTime = System.currentTimeMillis();
        }

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            AvgBuilder avgBuilder = AggregationBuilders
                    .avg("avg")
                    .field("actual");

            TermsBuilder componentsBuilder = AggregationBuilders
                    .terms("components")
                    .field("componentType")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(avgBuilder);

            DateHistogramBuilder histogramBuilder = AggregationBuilders
                    .dateHistogram("histogram")
                    .interval(interval)
                    .field("timestamp")
                    .subAggregation(componentsBuilder);

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(NODE_DETAILS_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(histogramBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            DateHistogram histogram = response.getAggregations().get("histogram");

            for (Bucket bucket : histogram.getBuckets()) {
                Terms term = bucket.getAggregations().get("components");

                NodeTimeseriesStatistics s = new NodeTimeseriesStatistics();
                s.setTimestamp(bucket.getKeyAsDate().getMillis());

                for (Terms.Bucket termBucket : term.getBuckets()) {
                    Avg avg = termBucket.getAggregations().get("avg");
                    s.getComponentTypes().put(termBucket.getKey(),
                            new NodeComponentTypeStatistics(avg.getValue(), termBucket.getDocCount()));
                }

                stats.add(s);

                numOfNodes += bucket.getDocCount();
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get node timeseries stats");
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Performance: Results processed in " + (System.currentTimeMillis() - queryTime) + "ms and "
                    + "number of nodes processed = " + numOfNodes);
        }

        return stats;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getNodeSummaryStatistics(java.lang.String,
     *                  org.hawkular.btm.api.services.Criteria)
     */
    @Override
    public Collection<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);

        List<NodeSummaryStatistics> stats = new ArrayList<NodeSummaryStatistics>();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            AvgBuilder actualBuilder = AggregationBuilders
                    .avg("actual")
                    .field("actual");

            AvgBuilder elapsedBuilder = AggregationBuilders
                    .avg("elapsed")
                    .field("elapsed");

            TermsBuilder operationsBuilder = AggregationBuilders
                    .terms("operations")
                    .field("operation")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(actualBuilder)
                    .subAggregation(elapsedBuilder);

            MissingBuilder missingOperationBuilder = AggregationBuilders
                    .missing("missingOperation")
                    .field("operation")
                    .subAggregation(actualBuilder)
                    .subAggregation(elapsedBuilder);

            TermsBuilder urisBuilder = AggregationBuilders
                    .terms("uris")
                    .field("uri")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(operationsBuilder)
                    .subAggregation(missingOperationBuilder);

            TermsBuilder componentsBuilder = AggregationBuilders
                    .terms("components")
                    .field("componentType")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(urisBuilder);

            TermsBuilder interactionUrisBuilder = AggregationBuilders
                    .terms("uris")
                    .field("uri")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(actualBuilder)
                    .subAggregation(elapsedBuilder);

            MissingBuilder missingComponentsBuilder = AggregationBuilders
                    .missing("missingcomponent")
                    .field("componentType")
                    .subAggregation(interactionUrisBuilder);

            TermsBuilder nodesBuilder = AggregationBuilders
                    .terms("types")
                    .field("type")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(componentsBuilder)
                    .subAggregation(missingComponentsBuilder);

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(NODE_DETAILS_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(nodesBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Terms types = response.getAggregations().get("types");

            for (Terms.Bucket typeBucket : types.getBuckets()) {
                Terms components = typeBucket.getAggregations().get("components");

                for (Terms.Bucket componentBucket : components.getBuckets()) {
                    Terms uris = componentBucket.getAggregations().get("uris");

                    for (Terms.Bucket uriBucket : uris.getBuckets()) {
                        Terms operations = uriBucket.getAggregations().get("operations");

                        for (Terms.Bucket operationBucket : operations.getBuckets()) {
                            Avg actual = operationBucket.getAggregations().get("actual");
                            Avg elapsed = operationBucket.getAggregations().get("elapsed");

                            NodeSummaryStatistics stat = new NodeSummaryStatistics();

                            if (typeBucket.getKey().equalsIgnoreCase("consumer")) {
                                stat.setComponentType("consumer");
                            } else if (typeBucket.getKey().equalsIgnoreCase("producer")) {
                                stat.setComponentType("producer");
                            } else {
                                stat.setComponentType(componentBucket.getKey());
                            }
                            stat.setUri(uriBucket.getKey());
                            stat.setOperation(operationBucket.getKey());
                            stat.setActual(actual.getValue());
                            stat.setElapsed(elapsed.getValue());
                            stat.setCount(operationBucket.getDocCount());

                            stats.add(stat);
                        }

                        Missing missingOp = uriBucket.getAggregations().get("missingOperation");
                        Avg actual = missingOp.getAggregations().get("actual");
                        Avg elapsed = missingOp.getAggregations().get("elapsed");

                        // TODO: For some reason doing comparison of value against Double.NaN does not work
                        if (!actual.getValueAsString().equals("NaN")) {
                            NodeSummaryStatistics stat = new NodeSummaryStatistics();

                            if (typeBucket.getKey().equalsIgnoreCase("consumer")) {
                                stat.setComponentType("consumer");
                            } else if (typeBucket.getKey().equalsIgnoreCase("producer")) {
                                stat.setComponentType("producer");
                            } else {
                                stat.setComponentType(componentBucket.getKey());
                            }
                            stat.setUri(uriBucket.getKey());
                            stat.setActual(actual.getValue());
                            stat.setElapsed(elapsed.getValue());
                            stat.setCount(missingOp.getDocCount());

                            stats.add(stat);
                        }
                    }
                }

                Missing missingComponents = typeBucket.getAggregations().get("missingcomponent");

                Terms uris = missingComponents.getAggregations().get("uris");

                for (Terms.Bucket uriBucket : uris.getBuckets()) {
                    Avg actual = uriBucket.getAggregations().get("actual");
                    Avg elapsed = uriBucket.getAggregations().get("elapsed");

                    NodeSummaryStatistics stat = new NodeSummaryStatistics();

                    stat.setComponentType(typeBucket.getKey());
                    stat.setUri(uriBucket.getKey());
                    stat.setActual(actual.getValue());
                    stat.setElapsed(elapsed.getValue());
                    stat.setCount(uriBucket.getDocCount());

                    stats.add(stat);
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get node summary stats");
            }
        }

        return stats;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeNodeDetails(java.lang.String, java.util.List)
     */
    @Override
    public void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < nodeDetails.size(); i++) {
            NodeDetails rt = nodeDetails.get(i);
            String json = mapper.writeValueAsString(rt);

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing node details: %s", json);
            }

            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    NODE_DETAILS_TYPE, rt.getId()).setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store node details: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store node details to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing node details to elasticsearch");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeCompletionTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < completionTimes.size(); i++) {
            CompletionTime ct = completionTimes.get(i);
            String json = mapper.writeValueAsString(ct);

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing completion time: %s", json);
            }

            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    COMPLETION_TIME_TYPE, ct.getId()).setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store completion times: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store completion times to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing completion times to elasticsearch");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getHostNames(java.lang.String,
     *                      org.hawkular.btm.api.services.BaseCriteria)
     */
    @Override
    public List<String> getHostNames(String tenantId, Criteria criteria) {
        List<String> ret = new ArrayList<String>();
        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "startTime", "name");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(BusinessTransactionServiceElasticsearch.BUSINESS_TRANSACTION_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            List<BusinessTransaction> btxns = new ArrayList<BusinessTransaction>();

            for (SearchHit searchHitFields : response.getHits()) {
                try {
                    btxns.add(mapper.readValue(searchHitFields.getSourceAsString(),
                            BusinessTransaction.class));
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }

            // Process the fragments to identify host names
            for (int i = 0; i < btxns.size(); i++) {
                BusinessTransaction btxn = btxns.get(i);

                if (btxn.getHostName() != null && btxn.getHostName().trim().length() != 0
                        && !ret.contains(btxn.getHostName())) {
                    ret.add(btxn.getHostName());
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no business transactions have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get host names");
            }
        }

        Collections.sort(ret);

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        String index = client.getIndex(tenantId);

        try {
            client.getElasticsearchClient().admin().indices().prepareDelete(index).execute().actionGet();
        } catch (IndexMissingException ime) {
            // Ignore
        }
    }

}
