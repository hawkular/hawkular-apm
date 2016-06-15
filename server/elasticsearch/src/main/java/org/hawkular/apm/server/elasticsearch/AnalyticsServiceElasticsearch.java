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
package org.hawkular.apm.server.elasticsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram.Bucket;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.missing.MissingBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.avg.AvgBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentile;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsBuilder;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics.ConnectionStatistics;
import org.hawkular.apm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics.NodeComponentTypeStatistics;
import org.hawkular.apm.api.model.analytics.Percentiles;
import org.hawkular.apm.api.model.analytics.PrincipalInfo;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.AbstractAnalyticsService;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apmserver.elasticsearch.log.MsgLogger;

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
    private static final String COMMUNICATION_DETAILS_TYPE = "communicationdetails";

    /**  */
    private static final String NODE_DETAILS_TYPE = "nodedetails";

    /**  */
    private static final String TRACE_COMPLETION_TIME_TYPE = "tracecompletiontime";

    /**  */
    private static final String FRAGMENT_COMPLETION_TIME_TYPE = "fragmentcompletiontime";

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
     * @see org.hawkular.apm.api.services.AbstractAnalyticsService#getFragments(java.lang.String,
     *                  org.hawkular.apm.api.services.Criteria)
     */
    @Override
    protected List<Trace> getFragments(String tenantId, Criteria criteria) {
        return TraceServiceElasticsearch.internalQuery(client,
                tenantId, criteria);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getPrincipalInfo(java.lang.String,
     *                          org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<PrincipalInfo> getPrincipalInfo(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);

        List<PrincipalInfo> ret = new ArrayList<PrincipalInfo>();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "startTime", "businessTransaction");

            TermsBuilder cardinalityBuilder = AggregationBuilders
                    .terms("cardinality")
                    .field("principal")
                    .order(Order.aggregation("_count", false))
                    .size(criteria.getMaxResponseSize());

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(TraceServiceElasticsearch.TRACE_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(cardinalityBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Terms terms = response.getAggregations().get("cardinality");

            for (Terms.Bucket bucket : terms.getBuckets()) {
                PrincipalInfo pi = new PrincipalInfo();
                pi.setId(bucket.getKey());
                pi.setCount(bucket.getDocCount());
                ret.add(pi);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get principal details");
            }
        }

        Collections.sort(ret, new Comparator<PrincipalInfo>() {
            @Override
            public int compare(PrincipalInfo arg0, PrincipalInfo arg1) {
                return arg0.getId().compareTo(arg1.getId());
            }
        });

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionCount(java.lang.String,
     *                  org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public long getTraceCompletionCount(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(TRACE_COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
                return 0;
            } else {
                return response.getHits().getTotalHits();
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion count");
            }
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionFaultCount(java.lang.String,
     *              org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public long getTraceCompletionFaultCount(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            FilterBuilder filter = FilterBuilders.existsFilter("fault");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(TRACE_COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
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
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion faultcount");
            }
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getStats(java.lang.String,
     *                  org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public Percentiles getTraceCompletionPercentiles(String tenantId, Criteria criteria) {
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
                    .setTypes(TRACE_COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(percentileAgg)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles agg =
                    response.getAggregations().get("percentiles");

            for (Percentile entry : agg) {
                percentiles.addPercentile((int) entry.getPercent(), (long)entry.getValue());
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion percentiles");
            }
        }

        return percentiles;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionStatistics(java.lang.String,
     *                  org.hawkular.apm.api.services.Criteria, long)
     */
    @Override
    public List<CompletionTimeseriesStatistics> getTraceCompletionTimeseriesStatistics(String tenantId,
            Criteria criteria, long interval) {
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
                    .setTypes(TRACE_COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(histogramBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
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
                s.setAverage((long)stat.getAvg());
                s.setMin((long)stat.getMin());
                s.setMax((long)stat.getMax());
                s.setCount(stat.getCount());
                s.setFaultCount(stat.getCount() - missing.getDocCount());

                stats.add(s);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get completion timeseries stats");
            }
        }

        return stats;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionFaultDetails(java.lang.String,
     *              org.hawkular.apm.api.services.Criteria)
     */
    @Override
    public List<Cardinality> getTraceCompletionFaultDetails(String tenantId, Criteria criteria) {
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
                    .setTypes(TRACE_COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(cardinalityBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
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
            // Ignore, as means that no traces have
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
     * @see org.hawkular.apm.api.services.AnalyticsService#getCompletionPropertyDetails(java.lang.String,
     *              org.hawkular.apm.api.services.Criteria, java.lang.String)
     */
    @Override
    public List<Cardinality> getTraceCompletionPropertyDetails(String tenantId, Criteria criteria,
            String property) {
        String index = client.getIndex(tenantId);

        List<Cardinality> ret = new ArrayList<Cardinality>();

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            BoolQueryBuilder nestedQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery("properties.name", property));

            query.must(QueryBuilders.nestedQuery("properties", nestedQuery));

            TermsBuilder cardinalityBuilder = AggregationBuilders
                    .terms("cardinality")
                    .field("properties.value")
                    .order(Order.aggregation("_count", false))
                    .size(criteria.getMaxResponseSize());

            FilterAggregationBuilder filterAggBuilder = AggregationBuilders
                    .filter("nestedfilter")
                    .filter(FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("properties.name", property))))
                    .subAggregation(cardinalityBuilder);

            NestedBuilder nestedBuilder = AggregationBuilders
                    .nested("nested")
                    .path("properties")
                    .subAggregation(filterAggBuilder);

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(TRACE_COMPLETION_TIME_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(nestedBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Nested nested = response.getAggregations().get("nested");
            InternalFilter filteredAgg = nested.getAggregations().get("nestedfilter");
            Terms terms = filteredAgg.getAggregations().get("cardinality");

            for (Terms.Bucket bucket : terms.getBuckets()) {
                Cardinality card = new Cardinality();
                card.setValue(bucket.getKey());
                card.setCount(bucket.getDocCount());
                ret.add(card);
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
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
     * @see org.hawkular.apm.api.services.AnalyticsService#getNodeStatistics(java.lang.String,
     *                      org.hawkular.apm.api.services.Criteria, long)
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
                    .setSize(0)
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
                            new NodeComponentTypeStatistics((long)avg.getValue(), termBucket.getDocCount()));
                }

                stats.add(s);

                numOfNodes += bucket.getDocCount();
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
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
     * @see org.hawkular.apm.api.services.AnalyticsService#getNodeSummaryStatistics(java.lang.String,
     *                  org.hawkular.apm.api.services.Criteria)
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
                    .setSize(0)
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
                            stat.setActual((long)actual.getValue());
                            stat.setElapsed((long)elapsed.getValue());
                            stat.setCount(operationBucket.getDocCount());

                            stats.add(stat);
                        }

                        Missing missingOp = uriBucket.getAggregations().get("missingOperation");
                        if (missingOp != null && missingOp.getDocCount() > 0) {
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
                                stat.setActual((long)actual.getValue());
                                stat.setElapsed((long)elapsed.getValue());
                                stat.setCount(missingOp.getDocCount());

                                stats.add(stat);
                            }
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
                    stat.setActual((long)actual.getValue());
                    stat.setElapsed((long)elapsed.getValue());
                    stat.setCount(uriBucket.getDocCount());

                    stats.add(stat);
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get node summary stats");
            }
        }

        return stats;
    }

    /**
     * This method returns the flat list of communication summary stats.
     *
     * @param tenantId The tenant id
     * @param criteria The criteria
     * @return The list of communication summary nodes
     */
    protected Collection<CommunicationSummaryStatistics> doGetCommunicationSummaryStatistics(String tenantId,
            Criteria criteria) {
        String index = client.getIndex(tenantId);

        Map<String, CommunicationSummaryStatistics> stats = new HashMap<String, CommunicationSummaryStatistics>();

        if (!criteria.transactionWide()) {
            Criteria txnWideCriteria = criteria.deriveTransactionWide();
            buildCommunicationSummaryStatistics(stats, index, txnWideCriteria, false);
        }

        buildCommunicationSummaryStatistics(stats, index, criteria, true);

        return stats.values();
    }

    /**
     * This method builds a map of communication summary stats related to the supplied
     * criteria.
     *
     * @param stats The map of communication summary stats
     * @param index The index
     * @param criteria The criteria
     * @param addMetrics Whether to add metrics on the nodes/links
     */
    protected void buildCommunicationSummaryStatistics(Map<String, CommunicationSummaryStatistics> stats,
                            String index, Criteria criteria, boolean addMetrics) {
        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "timestamp", "businessTransaction");

            // Only want external communications
            query = query.mustNot(QueryBuilders.matchQuery("internal", "true"));

            StatsBuilder latencyBuilder = AggregationBuilders
                    .stats("latency")
                    .field("latency");

            TermsBuilder targetBuilder = AggregationBuilders
                    .terms("target")
                    .field("target")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(latencyBuilder);

            TermsBuilder sourceBuilder = AggregationBuilders
                    .terms("source")
                    .field("source")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(targetBuilder);

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(COMMUNICATION_DETAILS_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .addAggregation(sourceBuilder)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(0)
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Terms sources = response.getAggregations().get("source");

            for (Terms.Bucket sourceBucket : sources.getBuckets()) {
                Terms targets = sourceBucket.getAggregations().get("target");

                String id = sourceBucket.getKey();

                CommunicationSummaryStatistics css = stats.get(id);

                if (css == null) {
                    css = new CommunicationSummaryStatistics();
                    css.setId(sourceBucket.getKey());
                    stats.put(css.getId(), css);
                }

                if (addMetrics) {
                    css.setCount(sourceBucket.getDocCount());
                }

                for (Terms.Bucket targetBucket : targets.getBuckets()) {
                    Stats latency = targetBucket.getAggregations().get("latency");

                    String linkId = targetBucket.getKey();
                    ConnectionStatistics con = css.getOutbound().get(linkId);

                    if (con == null) {
                        con = new ConnectionStatistics();
                        css.getOutbound().put(linkId, con);
                    }

                    if (addMetrics) {
                        con.setMinimumLatency((long)latency.getMin());
                        con.setAverageLatency((long)latency.getAvg());
                        con.setMaximumLatency((long)latency.getMax());
                        con.setCount(targetBucket.getDocCount());
                    }
                }
            }

            // Obtain information about the fragments
            StatsBuilder durationBuilder = AggregationBuilders
                    .stats("duration")
                    .field("duration");

            TermsBuilder operationsBuilder2 = AggregationBuilders
                    .terms("operations")
                    .field("operation")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(durationBuilder);

            MissingBuilder missingOperationBuilder2 = AggregationBuilders
                    .missing("missingOperation")
                    .field("operation")
                    .subAggregation(durationBuilder);

            TermsBuilder urisBuilder2 = AggregationBuilders
                    .terms("uris")
                    .field("uri")
                    .size(criteria.getMaxResponseSize())
                    .subAggregation(operationsBuilder2)
                    .subAggregation(missingOperationBuilder2);

            SearchRequestBuilder request2 = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(FRAGMENT_COMPLETION_TIME_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addAggregation(urisBuilder2)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setSize(0)
                .setQuery(query);

            SearchResponse response2 = request2.execute().actionGet();
            if (response2.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            Terms completions = response2.getAggregations().get("uris");

            for (Terms.Bucket urisBucket : completions.getBuckets()) {
                Terms operations = urisBucket.getAggregations().get("operations");

                for (Terms.Bucket operationBucket : operations.getBuckets()) {
                    Stats duration = operationBucket.getAggregations().get("duration");
                    String id = EndpointUtil.encodeEndpoint(urisBucket.getKey(),
                            operationBucket.getKey());

                    CommunicationSummaryStatistics css = stats.get(id);
                    if (css == null) {
                        css = new CommunicationSummaryStatistics();
                        css.setId(id);
                        stats.put(id, css);
                    }

                    if (addMetrics) {
                        css.setMinimumDuration((long)duration.getMin());
                        css.setAverageDuration((long)duration.getAvg());
                        css.setMaximumDuration((long)duration.getMax());
                        css.setCount(operationBucket.getDocCount());
                    }
                }

                Missing missingOp = urisBucket.getAggregations().get("missingOperation");

                if (missingOp != null && missingOp.getDocCount() > 0) {
                    Stats duration = missingOp.getAggregations().get("duration");
                    String id = urisBucket.getKey();

                    CommunicationSummaryStatistics css = stats.get(id);
                    if (css == null) {
                        css = new CommunicationSummaryStatistics();
                        css.setId(id);
                        stats.put(id, css);
                    }

                    if (addMetrics) {
                        css.setMinimumDuration((long)duration.getMin());
                        css.setAverageDuration((long)duration.getAvg());
                        css.setMaximumDuration((long)duration.getMax());
                        css.setCount(missingOp.getDocCount());
                    }
                }
            }

        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get communication summary stats");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#storeCommunicationDetails(java.lang.String, java.util.List)
     */
    @Override
    public void storeCommunicationDetails(String tenantId, List<CommunicationDetails> communicationDetails)
            throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < communicationDetails.size(); i++) {
            CommunicationDetails cd = communicationDetails.get(i);
            String json = mapper.writeValueAsString(cd);

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing communication details: %s", json);
            }

            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    COMMUNICATION_DETAILS_TYPE, cd.getId()).setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store communication details: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store communication details to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing communication details to elasticsearch");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#storeNodeDetails(java.lang.String, java.util.List)
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
     * @see org.hawkular.apm.api.services.AnalyticsService#storeCompletionTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeTraceCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < completionTimes.size(); i++) {
            CompletionTime ct = completionTimes.get(i);
            String json = mapper.writeValueAsString(ct);

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing btxn completion time: %s", json);
            }

            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    TRACE_COMPLETION_TIME_TYPE, ct.getId()).setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store btxn completion times: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store btxn completion times to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing btxn completion times to elasticsearch");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#storeFragmentCompletionTimes(java.lang.String,
     *                                      java.util.List)
     */
    @Override
    public void storeFragmentCompletionTimes(String tenantId, List<CompletionTime> completionTimes) throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < completionTimes.size(); i++) {
            CompletionTime ct = completionTimes.get(i);
            String json = mapper.writeValueAsString(ct);

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing fragment completion time: %s", json);
            }

            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    FRAGMENT_COMPLETION_TIME_TYPE, ct.getId()).setSource(json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store fragment completion times: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store fragment completion times to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing fragment completion times to elasticsearch");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#getHostNames(java.lang.String,
     *                      org.hawkular.apm.api.services.BaseCriteria)
     */
    @Override
    public List<String> getHostNames(String tenantId, Criteria criteria) {
        List<String> ret = new ArrayList<String>();
        String index = client.getIndex(tenantId);

        try {
            RefreshRequestBuilder refreshRequestBuilder =
                    client.getElasticsearchClient().admin().indices().prepareRefresh(index);
            client.getElasticsearchClient().admin().indices().refresh(refreshRequestBuilder.request()).actionGet();

            BoolQueryBuilder query = ElasticsearchUtil.buildQuery(criteria, "startTime", "businessTransaction");

            SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                    .setTypes(TraceServiceElasticsearch.TRACE_TYPE)
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                    .setSize(criteria.getMaxResponseSize())
                    .setQuery(query);

            SearchResponse response = request.execute().actionGet();
            if (response.isTimedOut()) {
                msgLog.warnQueryTimedOut();
            }

            List<Trace> btxns = new ArrayList<Trace>();

            for (SearchHit searchHitFields : response.getHits()) {
                try {
                    btxns.add(mapper.readValue(searchHitFields.getSourceAsString(),
                            Trace.class));
                } catch (Exception e) {
                    msgLog.errorFailedToParse(e);
                }
            }

            // Process the fragments to identify host names
            for (int i = 0; i < btxns.size(); i++) {
                Trace trace = btxns.get(i);

                if (trace.getHostName() != null && trace.getHostName().trim().length() != 0
                        && !ret.contains(trace.getHostName())) {
                    ret.add(trace.getHostName());
                }
            }
        } catch (org.elasticsearch.indices.IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("No index found, so unable to get host names");
            }
        }

        Collections.sort(ret);

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.AnalyticsService#clear(java.lang.String)
     */
    @Override
    public void clear(String tenantId) {
        String index = client.getIndex(tenantId);

        try {
            client.getElasticsearchClient().admin().indices().prepareDelete(index).execute().actionGet();
            client.clear(tenantId);
        } catch (IndexMissingException ime) {
            // Ignore
        }
    }

}
