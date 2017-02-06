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
package org.hawkular.apm.server.elasticsearch;

import static org.hawkular.apm.server.elasticsearch.ElasticsearchUtil.buildQuery;
import static org.hawkular.apm.server.elasticsearch.TraceServiceElasticsearch.TRACE_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
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
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics.ConnectionStatistics;
import org.hawkular.apm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics.NodeComponentTypeStatistics;
import org.hawkular.apm.api.model.analytics.Percentiles;
import org.hawkular.apm.api.model.analytics.PropertyInfo;
import org.hawkular.apm.api.model.analytics.TimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.TransactionInfo;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.events.ApmEvent;
import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.AbstractAnalyticsService;
import org.hawkular.apm.api.services.ConfigurationService;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.utils.EndpointUtil;
import org.hawkular.apm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the Elasticsearch implementation of the Analytics
 * Service.
 *
 * @author gbrown
 */
public class AnalyticsServiceElasticsearch extends AbstractAnalyticsService {
    private static final MsgLogger msgLog = MsgLogger.LOGGER;
    private static final String COMMUNICATION_DETAILS_TYPE = "communicationdetails";
    private static final String NODE_DETAILS_TYPE = "nodedetails";
    private static final String TRACE_COMPLETION_TIME_TYPE = "tracecompletion";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static ElasticsearchClient client = ElasticsearchClient.getSingleton();

    @Inject
    private ConfigurationService configService;

    @Override
    protected List<Trace> getFragments(String tenantId, Criteria criteria) {
        return TraceServiceElasticsearch.internalQuery(client, tenantId, criteria);
    }

    @Override
    public List<TransactionInfo> getTransactionInfo(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        if (criteria.getTransaction() != null) {
            // Copy criteria and clear the transaction field to ensure all
            // transactions are returned related to the other filter criteria
            criteria = new Criteria(criteria);
            criteria.setTransaction(null);
        }

        TermsBuilder cardinalityBuilder = AggregationBuilders
                .terms("cardinality")
                .field(ElasticsearchUtil.TRANSACTION_FIELD)
                .order(Order.aggregation("_count", false))
                .size(criteria.getMaxResponseSize());

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, Trace.class);
        SearchRequestBuilder request = getBaseSearchRequestBuilder(TRACE_TYPE, index, criteria, query, 0)
                .addAggregation(cardinalityBuilder);

        SearchResponse response = getSearchResponse(request);

        Terms terms = response.getAggregations().get("cardinality");
        List<TransactionInfo> txnInfo = terms.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toTransactionInfo)
                .collect(Collectors.toList());

        // If config service available, check if there is a transaction config for the list of
        // transactions being returned
        if (configService != null) {
            Map<String,TransactionConfig> btcs = configService.getTransactions(tenantId, 0);

            txnInfo.forEach(ti -> {
                TransactionConfig btc = btcs.get(ti.getName());
                if (btc != null) {
                    ti.setLevel(btc.getLevel());
                    ti.setStaticConfig(true);
                    btcs.remove(ti.getName());
                }
            });

            // Add entry for remaining transaction configs
            btcs.forEach((k,v) -> txnInfo.add(new TransactionInfo().setName(k).setLevel(v.getLevel()).setStaticConfig(true)));
        }

        // Sort the list by transaction name
        Collections.sort(txnInfo, new Comparator<TransactionInfo>() {
            @Override
            public int compare(TransactionInfo ti1, TransactionInfo ti2) {
                return ti1.getName().compareTo(ti2.getName());
            }

        });

        return txnInfo;
    }

    @Override
    public List<PropertyInfo> getPropertyInfo(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);

        TermsBuilder cardinalityBuilder = AggregationBuilders
                .terms("cardinality")
                .field(ElasticsearchUtil.PROPERTIES_NAME_FIELD)
                .order(Order.aggregation("_count", false))
                .size(criteria.getMaxResponseSize());

        NestedBuilder nestedBuilder = AggregationBuilders
                .nested("nested")
                .path(ElasticsearchUtil.PROPERTIES_FIELD)
                .subAggregation(cardinalityBuilder);

        SearchRequestBuilder request = getTraceCompletionRequest(index, criteria, query, 0)
                .addAggregation(nestedBuilder);

        SearchResponse response = getSearchResponse(request);
        Nested nested = response.getAggregations().get("nested");
        Terms terms = nested.getAggregations().get("cardinality");

        return terms.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toPropertyInfo)
                .sorted((one, another) -> one.getName().compareTo(another.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public long getTraceCompletionCount(String tenantId, Criteria criteria) {
        return getTraceCompletionCount(tenantId, criteria, false);
    }

    @Override
    public long getTraceCompletionFaultCount(String tenantId, Criteria criteria) {
        return getTraceCompletionCount(tenantId, criteria, true);
    }

    @Override
    public List<CompletionTime> getTraceCompletions(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);
        SearchRequestBuilder request = getTraceCompletionRequest(index, criteria, query, criteria.getMaxResponseSize());
        request.addSort(ElasticsearchUtil.TIMESTAMP_FIELD, SortOrder.DESC);
        SearchResponse response = getSearchResponse(request);
        if (response.isTimedOut()) {
            return null;
        }

        return Arrays.stream(response.getHits().getHits())
                .map(AnalyticsServiceElasticsearch::toCompletionTime)
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }

    @Override
    public Percentiles getTraceCompletionPercentiles(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        PercentilesBuilder percentileAgg = AggregationBuilders
                .percentiles("percentiles")
                .field(ElasticsearchUtil.DURATION_FIELD);

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);
        SearchRequestBuilder request = getTraceCompletionRequest(index, criteria, query, 0)
                .addAggregation(percentileAgg);

        SearchResponse response = getSearchResponse(request);

        org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles agg =
                response.getAggregations().get("percentiles");

        Percentiles percentiles = new Percentiles();
        agg.forEach(p -> percentiles.addPercentile((int) p.getPercent(), (long) p.getValue()));
        return percentiles;
    }

    @Override
    public List<TimeseriesStatistics> getTraceCompletionTimeseriesStatistics(String tenantId, Criteria criteria, long interval) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        StatsBuilder statsBuilder = AggregationBuilders
                .stats("stats")
                .field(ElasticsearchUtil.DURATION_FIELD);

        // TODO: HWKAPM-679 (related to HWKAPM-675), faults now recorded as properties. However this
        // current results in the fault count being an actual count of fault properties, where
        // the original intention of the fault count is the number of txns that have been affected
        // by a fault.
        FilterAggregationBuilder faultCountBuilder = AggregationBuilders
                .filter("faults")
                .filter(FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, Constants.PROP_FAULT))));

        NestedBuilder nestedFaultCountBuilder = AggregationBuilders
                .nested("nested")
                .path(ElasticsearchUtil.PROPERTIES_FIELD)
                .subAggregation(faultCountBuilder);

        DateHistogramBuilder histogramBuilder = AggregationBuilders
                .dateHistogram("histogram")
                .interval(interval)
                .field(ElasticsearchUtil.TIMESTAMP_FIELD)
                .subAggregation(statsBuilder)
                .subAggregation(nestedFaultCountBuilder);

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);
        SearchRequestBuilder request = getTraceCompletionRequest(index, criteria, query, 0)
                .addAggregation(histogramBuilder);

        SearchResponse response = getSearchResponse(request);
        DateHistogram histogram = response.getAggregations().get("histogram");

        return histogram.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toTimeseriesStatistics)
                .collect(Collectors.toList());
    }

    @Override
    public List<Cardinality> getTraceCompletionFaultDetails(String tenantId, Criteria criteria) {
        return getTraceCompletionPropertyDetails(tenantId, criteria, Constants.PROP_FAULT);
    }

    @Override
    public List<Cardinality> getTraceCompletionPropertyDetails(String tenantId, Criteria criteria, String property) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        BoolQueryBuilder nestedQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, property));

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);
        query.must(QueryBuilders.nestedQuery("properties", nestedQuery));

        TermsBuilder cardinalityBuilder = AggregationBuilders
                .terms("cardinality")
                .field(ElasticsearchUtil.PROPERTIES_VALUE_FIELD)
                .order(Order.aggregation("_count", false))
                .size(criteria.getMaxResponseSize());

        FilterAggregationBuilder filterAggBuilder = AggregationBuilders
                .filter("nestedfilter")
                .filter(FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, property))))
                .subAggregation(cardinalityBuilder);

        NestedBuilder nestedBuilder = AggregationBuilders
                .nested("nested")
                .path(ElasticsearchUtil.PROPERTIES_FIELD)
                .subAggregation(filterAggBuilder);

        SearchRequestBuilder request = getTraceCompletionRequest(index, criteria, query, 0)
                .addAggregation(nestedBuilder);

        SearchResponse response = getSearchResponse(request);
        Nested nested = response.getAggregations().get("nested");
        InternalFilter filteredAgg = nested.getAggregations().get("nestedfilter");
        Terms terms = filteredAgg.getAggregations().get("cardinality");

        return terms.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toCardinality)
                .sorted((one, another) -> one.getValue().compareTo(another.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<NodeTimeseriesStatistics> getNodeTimeseriesStatistics(String tenantId, Criteria criteria, long interval) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        AvgBuilder avgBuilder = AggregationBuilders
                .avg("avg")
                .field(ElasticsearchUtil.ACTUAL_FIELD);

        TermsBuilder componentsBuilder = AggregationBuilders
                .terms("components")
                .field("componentType")
                .size(criteria.getMaxResponseSize())
                .subAggregation(avgBuilder);

        DateHistogramBuilder histogramBuilder = AggregationBuilders
                .dateHistogram("histogram")
                .interval(interval)
                .field(ElasticsearchUtil.TIMESTAMP_FIELD)
                .subAggregation(componentsBuilder);

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, NodeDetails.class);
        SearchRequestBuilder request = getNodeDetailsRequest(index, criteria, query, 0)
                .addAggregation(histogramBuilder);

        SearchResponse response = getSearchResponse(request);
        DateHistogram histogram = response.getAggregations().get("histogram");

        return histogram.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toNodeTimeseriesStatistics)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<NodeSummaryStatistics> getNodeSummaryStatistics(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        List<NodeSummaryStatistics> stats = new ArrayList<>();

        AvgBuilder actualBuilder = AggregationBuilders
                .avg("actual")
                .field(ElasticsearchUtil.ACTUAL_FIELD);

        AvgBuilder elapsedBuilder = AggregationBuilders
                .avg("elapsed")
                .field(ElasticsearchUtil.ELAPSED_FIELD);

        TermsBuilder operationsBuilder = AggregationBuilders
                .terms("operations")
                .field(ElasticsearchUtil.OPERATION_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(actualBuilder)
                .subAggregation(elapsedBuilder);

        MissingBuilder missingOperationBuilder = AggregationBuilders
                .missing("missingOperation")
                .field(ElasticsearchUtil.OPERATION_FIELD)
                .subAggregation(actualBuilder)
                .subAggregation(elapsedBuilder);

        TermsBuilder urisBuilder = AggregationBuilders
                .terms("uris")
                .field(ElasticsearchUtil.URI_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(operationsBuilder)
                .subAggregation(missingOperationBuilder);

        MissingBuilder missingUrisBuilder = AggregationBuilders
                .missing("missingUri")
                .field("uri")
                .subAggregation(operationsBuilder)
                .subAggregation(missingOperationBuilder);

        TermsBuilder componentsBuilder = AggregationBuilders
                .terms("components")
                .field("componentType")
                .size(criteria.getMaxResponseSize())
                .subAggregation(urisBuilder)
                .subAggregation(missingUrisBuilder);

        TermsBuilder interactionUrisBuilder = AggregationBuilders
                .terms("uris")
                .field(ElasticsearchUtil.URI_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(actualBuilder)
                .subAggregation(elapsedBuilder);

        MissingBuilder missingComponentsBuilder = AggregationBuilders
                .missing("missingcomponent")
                .field("componentType")
                .subAggregation(interactionUrisBuilder);

        TermsBuilder nodesBuilder = AggregationBuilders
                .terms("types")
                .field(ElasticsearchUtil.TYPE_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(componentsBuilder)
                .subAggregation(missingComponentsBuilder);

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, NodeDetails.class);
        SearchRequestBuilder request = getNodeDetailsRequest(index, criteria, query, 0)
                .addAggregation(nodesBuilder);

        SearchResponse response = getSearchResponse(request);
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
                        stat.setComponentType(getComponentTypeForBucket(typeBucket, componentBucket));
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
                            stat.setComponentType(getComponentTypeForBucket(typeBucket, componentBucket));
                            stat.setUri(uriBucket.getKey());
                            stat.setActual((long)actual.getValue());
                            stat.setElapsed((long)elapsed.getValue());
                            stat.setCount(missingOp.getDocCount());

                            stats.add(stat);
                        }
                    }
                }

                Missing missingUri = componentBucket.getAggregations().get("missingUri");
                if (missingUri.getDocCount() > 0) {
                    Terms operations = missingUri.getAggregations().get("operations");

                    for (Terms.Bucket operationBucket : operations.getBuckets()) {
                        Avg actual = operationBucket.getAggregations().get("actual");
                        Avg elapsed = operationBucket.getAggregations().get("elapsed");

                        NodeSummaryStatistics stat = new NodeSummaryStatistics();
                        stat.setComponentType(getComponentTypeForBucket(typeBucket, componentBucket));
                        stat.setOperation(operationBucket.getKey());
                        stat.setActual((long)actual.getValue());
                        stat.setElapsed((long)elapsed.getValue());
                        stat.setCount(operationBucket.getDocCount());

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
                stat.setActual((long)actual.getValue());
                stat.setElapsed((long)elapsed.getValue());
                stat.setCount(uriBucket.getDocCount());

                stats.add(stat);
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
    protected Collection<CommunicationSummaryStatistics> doGetCommunicationSummaryStatistics(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        Map<String, CommunicationSummaryStatistics> stats = new HashMap<>();

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
    private void buildCommunicationSummaryStatistics(Map<String, CommunicationSummaryStatistics> stats, String index,
                                                     Criteria criteria, boolean addMetrics) {
        if (!refresh(index)) {
            return;
        }

        // Don't specify target class, so that query provided that can be used with
        // CommunicationDetails and CompletionTime
        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, null);

        // Only want external communications
        query = query.mustNot(QueryBuilders.matchQuery("internal", "true"));

        StatsBuilder latencyBuilder = AggregationBuilders
                .stats("latency")
                .field(ElasticsearchUtil.LATENCY_FIELD);

        TermsBuilder targetBuilder = AggregationBuilders
                .terms("target")
                .field(ElasticsearchUtil.TARGET_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(latencyBuilder);

        TermsBuilder sourceBuilder = AggregationBuilders
                .terms("source")
                .field(ElasticsearchUtil.SOURCE_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(targetBuilder);

        SearchRequestBuilder request = getBaseSearchRequestBuilder(COMMUNICATION_DETAILS_TYPE, index, criteria, query, 0)
                .addAggregation(sourceBuilder);
        SearchResponse response = getSearchResponse(request);

        for (Terms.Bucket sourceBucket : response.getAggregations().<Terms>get("source").getBuckets()) {
            Terms targets = sourceBucket.getAggregations().get("target");

            CommunicationSummaryStatistics css = stats.get(sourceBucket.getKey());

            if (css == null) {
                css = new CommunicationSummaryStatistics();
                css.setId(sourceBucket.getKey());
                css.setUri(EndpointUtil.decodeEndpointURI(css.getId()));
                css.setOperation(EndpointUtil.decodeEndpointOperation(css.getId(), true));
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

        addNodeInformation(stats, index, criteria, addMetrics, false);
        addNodeInformation(stats, index, criteria, addMetrics, true);
    }

    /**
     * This method adds node information to the communication summary nodes constructed based on the
     * communication details.
     *
     * @param stats The map of endpoint (uri[op]) to communication summary stat nodes
     * @param index The index
     * @param criteria The query criteria
     * @param addMetrics Whether to add metrics or just discover any missing nodes
     * @param clients Whether node information should be located for clients (i.e. fragments with
     *                                  top level Producer node)
     */
    protected void addNodeInformation(Map<String, CommunicationSummaryStatistics> stats, String index,
            Criteria criteria, boolean addMetrics, boolean clients) {
        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, null);

        // Obtain information about the fragments
        StatsBuilder durationBuilder = AggregationBuilders
                .stats("elapsed")
                .field(ElasticsearchUtil.ELAPSED_FIELD);

        TermsBuilder serviceTerm = AggregationBuilders
                .terms("serviceTerm")
                .field(ElasticsearchUtil.PROPERTIES_VALUE_FIELD);

        FilterAggregationBuilder propertiesServiceFilter = AggregationBuilders
                .filter("propertiesServiceFilter")
                .filter(FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, Constants.PROP_SERVICE_NAME))))
                .subAggregation(serviceTerm);

        NestedBuilder nestedProperties = AggregationBuilders
                .nested("nestedProperties")
                .path(ElasticsearchUtil.PROPERTIES_FIELD)
                .subAggregation(propertiesServiceFilter);

        TermsBuilder operationsBuilder2 = AggregationBuilders
                .terms("operations")
                .field(ElasticsearchUtil.OPERATION_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(durationBuilder)
                .subAggregation(nestedProperties);

        MissingBuilder missingOperationBuilder2 = AggregationBuilders
                .missing("missingOperation")
                .field(ElasticsearchUtil.OPERATION_FIELD)
                .subAggregation(durationBuilder)
                .subAggregation(nestedProperties);

        TermsBuilder urisBuilder2 = AggregationBuilders
                .terms("uris")
                .field(ElasticsearchUtil.URI_FIELD)
                .size(criteria.getMaxResponseSize())
                .subAggregation(operationsBuilder2)
                .subAggregation(missingOperationBuilder2);

        MissingBuilder missingUriBuilder2 = AggregationBuilders
                .missing("missingUri")
                .field(ElasticsearchUtil.URI_FIELD)
                .subAggregation(operationsBuilder2)
                .subAggregation(missingOperationBuilder2);

        query = query.must(QueryBuilders.matchQuery("initial", "true"));

        // If interested in clients, then need to identify node details for Producers
        if (clients) {
            query = query.must(QueryBuilders.matchQuery("type", "Producer"));
        } else {
            query = query.mustNot(QueryBuilders.matchQuery("type", "Producer"));
        }

        SearchRequestBuilder request2 = getBaseSearchRequestBuilder(NODE_DETAILS_TYPE, index, criteria, query, 0);
        request2.addAggregation(urisBuilder2).addAggregation(missingUriBuilder2);

        SearchResponse response2 = getSearchResponse(request2);
        Terms completions = response2.getAggregations().get("uris");

        for (Terms.Bucket urisBucket : completions.getBuckets()) {
            String uri = urisBucket.getKey();
            if (clients) {
                uri = EndpointUtil.encodeClientURI(uri);
            }

            for (Terms.Bucket operationBucket : urisBucket.getAggregations().<Terms>get("operations").getBuckets()) {
                Stats elapsed = operationBucket.getAggregations().get("elapsed");
                String id = EndpointUtil.encodeEndpoint(uri,
                        operationBucket.getKey());

                CommunicationSummaryStatistics css = stats.get(id);
                if (css == null) {
                    css = new CommunicationSummaryStatistics();
                    css.setId(id);
                    css.setUri(uri);
                    css.setOperation(operationBucket.getKey());
                    stats.put(id, css);
                }

                if (addMetrics) {
                    doAddMetrics(css, elapsed, operationBucket.getDocCount());
                }

                String serviceName = serviceName(operationBucket.getAggregations()
                        .<Nested>get("nestedProperties").getAggregations()
                        .<Filter>get("propertiesServiceFilter")
                        .getAggregations().get("serviceTerm"));
                if (serviceName != null) {
                    css.setServiceName(serviceName);
                }
            }

            Missing missingOp = urisBucket.getAggregations().get("missingOperation");

            if (missingOp.getDocCount() > 0) {
                Stats elapsed = missingOp.getAggregations().get("elapsed");
                String id = EndpointUtil.encodeEndpoint(uri, null);

                CommunicationSummaryStatistics css = stats.get(id);
                if (css == null) {
                    css = new CommunicationSummaryStatistics();
                    css.setId(id);
                    css.setUri(uri);
                    stats.put(id, css);
                }

                if (addMetrics) {
                    doAddMetrics(css, elapsed, missingOp.getDocCount());
                }

                String serviceName = serviceName(missingOp.getAggregations()
                        .<Nested>get("nestedProperties").getAggregations()
                        .<Filter>get("propertiesServiceFilter")
                        .getAggregations().get("serviceTerm"));
                if (serviceName != null) {
                    css.setServiceName(serviceName);
                }
            }
        }

        Missing missingUri = response2.getAggregations().get("missingUri");

        if (missingUri.getDocCount() > 0) {
            Terms operations = missingUri.getAggregations().get("operations");

            for (Terms.Bucket operationBucket : operations.getBuckets()) {
                Stats elapsed = operationBucket.getAggregations().get("elapsed");
                String id = EndpointUtil.encodeEndpoint(null,
                        operationBucket.getKey());

                CommunicationSummaryStatistics css = stats.get(id);
                if (css == null) {
                    css = new CommunicationSummaryStatistics();
                    css.setId(id);
                    css.setOperation(operationBucket.getKey());
                    stats.put(id, css);
                }

                String serviceName = serviceName(operationBucket.getAggregations()
                        .<Nested>get("nestedProperties").getAggregations()
                        .<Filter>get("propertiesServiceFilter")
                        .getAggregations().get("serviceTerm"));
                if (serviceName != null) {
                    css.setServiceName(serviceName);
                }

                if (addMetrics) {
                    doAddMetrics(css, elapsed, operationBucket.getDocCount());
                }
            }
        }
    }

    @Override
    public List<TimeseriesStatistics> getEndpointResponseTimeseriesStatistics(String tenantId, Criteria criteria, long interval) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        StatsBuilder statsBuilder = AggregationBuilders
                .stats("stats")
                .field(ElasticsearchUtil.ELAPSED_FIELD);

        // TODO: HWKAPM-679 (related to HWKAPM-675), faults now recorded as properties. However this
        // current results in the fault count being an actual count of fault properties, where
        // the original intention of the fault count is the number of txns that have been affected
        // by a fault.
        FilterAggregationBuilder faultCountBuilder = AggregationBuilders
                .filter("faults")
                .filter(FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, Constants.PROP_FAULT))));

        NestedBuilder nestedFaultCountBuilder = AggregationBuilders
                .nested("nested")
                .path(ElasticsearchUtil.PROPERTIES_FIELD)
                .subAggregation(faultCountBuilder);

        DateHistogramBuilder histogramBuilder = AggregationBuilders
                .dateHistogram("histogram")
                .interval(interval)
                .field(ElasticsearchUtil.TIMESTAMP_FIELD)
                .subAggregation(statsBuilder)
                .subAggregation(nestedFaultCountBuilder);

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, NodeDetails.class);
        // Only interested in service endpoints, so just Consumer nodes
        query.must(QueryBuilders.termQuery(ElasticsearchUtil.TYPE_FIELD, "Consumer"));

        SearchRequestBuilder request = getNodeDetailsRequest(index, criteria, query, 0)
                .addAggregation(histogramBuilder);

        SearchResponse response = getSearchResponse(request);
        DateHistogram histogram = response.getAggregations().get("histogram");

        return histogram.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toTimeseriesStatistics)
                .collect(Collectors.toList());
    }

    protected List<Cardinality> getEndpointPropertyDetails(String tenantId, Criteria criteria, String property) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        BoolQueryBuilder nestedQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, property));

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);
        // Only interested in the initial node within a service invocation
        query = query.must(QueryBuilders.matchQuery("initial", "true"));
        query.must(QueryBuilders.nestedQuery("properties", nestedQuery));

        TermsBuilder cardinalityBuilder = AggregationBuilders
                .terms("cardinality")
                .field(ElasticsearchUtil.PROPERTIES_VALUE_FIELD)
                .order(Order.aggregation("_count", false))
                .size(criteria.getMaxResponseSize());

        FilterAggregationBuilder filterAggBuilder = AggregationBuilders
                .filter("nestedfilter")
                .filter(FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, property))))
                .subAggregation(cardinalityBuilder);

        NestedBuilder nestedBuilder = AggregationBuilders
                .nested("nested")
                .path(ElasticsearchUtil.PROPERTIES_FIELD)
                .subAggregation(filterAggBuilder);

        SearchRequestBuilder request = getNodeDetailsRequest(index, criteria, query, 0)
                .addAggregation(nestedBuilder);

        SearchResponse response = getSearchResponse(request);
        Nested nested = response.getAggregations().get("nested");
        InternalFilter filteredAgg = nested.getAggregations().get("nestedfilter");
        Terms terms = filteredAgg.getAggregations().get("cardinality");

        return terms.getBuckets().stream()
                .map(AnalyticsServiceElasticsearch::toCardinality)
                .sorted((one, another) -> one.getValue().compareTo(another.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getHostNames(String tenantId, Criteria criteria) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return null;
        }

        List<Trace> btxns = TraceServiceElasticsearch.internalQuery(client, tenantId, criteria);
        return btxns.stream()
                .filter(t -> t.getHostName() != null && !t.getHostName().trim().isEmpty())
                .map(Trace::getHostName)
                .sorted()
                .collect(Collectors.toSet());
    }

    @Override
    public void storeCommunicationDetails(String tenantId, List<CommunicationDetails> communicationDetails) throws StoreException {
        bulkStoreApmEvents(tenantId, communicationDetails, COMMUNICATION_DETAILS_TYPE);
    }

    @Override
    public void storeNodeDetails(String tenantId, List<NodeDetails> nodeDetails) throws StoreException {
        bulkStoreApmEvents(tenantId, nodeDetails, NODE_DETAILS_TYPE);
    }

    @Override
    public void storeTraceCompletions(String tenantId, List<CompletionTime> completionTimes) throws StoreException {
        bulkStoreApmEvents(tenantId, completionTimes, TRACE_COMPLETION_TIME_TYPE);
    }

    private void bulkStoreApmEvents(String tenantId, List<? extends ApmEvent> events, String type) throws StoreException {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getClient().prepareBulk();

        for (ApmEvent event : events) {
            String json = toJson(event);
            if (null == json) {
                continue;
            }

            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Storing event: %s", json);
            }

            bulkRequestBuilder.add(toIndexRequestBuilder(client, tenantId, type, event.getId(), json));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store event to elasticsearch: " + bulkItemResponses.buildFailureMessage());
            }
            throw new StoreException(bulkItemResponses.buildFailureMessage());
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing event to elasticsearch");
            }
        }
    }

    @Override
    public void clear(String tenantId) {
        client.clearTenant(tenantId);
    }

    private static String toJson(Object ct) {
        try {
            return mapper.writeValueAsString(ct);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static IndexRequestBuilder toIndexRequestBuilder(ElasticsearchClient client, String tenantId, String type, String id, String json) {
        return client
                .getClient()
                .prepareIndex(client.getIndex(tenantId),type, id)
                .setSource(json);
    }

    private static NodeTimeseriesStatistics toNodeTimeseriesStatistics(Bucket bucket) {
        Terms term = bucket.getAggregations().get("components");
        NodeTimeseriesStatistics s = new NodeTimeseriesStatistics();
        s.setTimestamp(bucket.getKeyAsDate().getMillis());
        term.getBuckets().forEach(b -> {
            Avg avg = b.getAggregations().get("avg");
            NodeComponentTypeStatistics component = new NodeComponentTypeStatistics((long)avg.getValue(), b.getDocCount());
            s.getComponentTypes().put(b.getKey(), component);
        });
        return s;
    }

    private static boolean refresh(String index) {
        try {
            AdminClient adminClient = client.getClient().admin();
            RefreshRequestBuilder refreshRequestBuilder = adminClient.indices().prepareRefresh(index);
            adminClient.indices().refresh(refreshRequestBuilder.request()).actionGet();
            return true;
        } catch (IndexMissingException t) {
            // Ignore, as means that no traces have
            // been stored yet
            if (msgLog.isTraceEnabled()) {
                msgLog.tracef("Index [%s] not found, unable to proceed.", index);
            }
            return false;
        }
    }

    private static Cardinality toCardinality(Terms.Bucket bucket) {
        Cardinality card = new Cardinality();
        card.setValue(bucket.getKey());
        card.setCount(bucket.getDocCount());
        return card;
    }

    private static TimeseriesStatistics toTimeseriesStatistics(Bucket bucket) {
        Stats stat = bucket.getAggregations().get("stats");

        long faultCount = bucket.getAggregations()
                .<Nested>get("nested").getAggregations()
                .<Filter>get("faults").getDocCount();

        TimeseriesStatistics s = new TimeseriesStatistics();
        s.setTimestamp(bucket.getKeyAsDate().getMillis());
        s.setAverage((long)stat.getAvg());
        s.setMin((long)stat.getMin());
        s.setMax((long)stat.getMax());
        s.setCount(stat.getCount());
        s.setFaultCount(faultCount);
        return s;
    }

    private static CompletionTime toCompletionTime(SearchHit searchHit) {
        try {
            return mapper.readValue(searchHit.getSourceAsString(), CompletionTime.class);
        } catch (IOException e) {
            msgLog.errorFailedToParse(e);
            return null;
        }
    }

    private static TransactionInfo toTransactionInfo(Terms.Bucket bucket) {
        TransactionInfo ti = new TransactionInfo();
        ti.setName(bucket.getKey());
        ti.setCount(bucket.getDocCount());
        return ti;
    }

    private static PropertyInfo toPropertyInfo(Terms.Bucket bucket) {
        PropertyInfo pi = new PropertyInfo();
        pi.setName(bucket.getKey());
        return pi;
    }

    private static SearchResponse getSearchResponse(SearchRequestBuilder request) {
        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
        }
        return response;
    }

    private String getComponentTypeForBucket(Terms.Bucket typeBucket, Terms.Bucket parent) {
        if (typeBucket.getKey().equalsIgnoreCase("consumer")) {
            return "consumer";
        } else if (typeBucket.getKey().equalsIgnoreCase("producer")) {
            return "producer";
        } else {
            return parent.getKey();
        }
    }

    private void doAddMetrics(CommunicationSummaryStatistics css, Stats duration, long docCount) {
        css.setMinimumDuration((long)duration.getMin());
        css.setAverageDuration((long)duration.getAvg());
        css.setMaximumDuration((long)duration.getMax());
        css.setCount(docCount);
    }

    private long getTraceCompletionCount(String tenantId, Criteria criteria, boolean onlyFaulty) {
        String index = client.getIndex(tenantId);
        if (!refresh(index)) {
            return 0;
        }

        BoolQueryBuilder query = buildQuery(criteria, ElasticsearchUtil.TRANSACTION_FIELD, CompletionTime.class);
        SearchRequestBuilder request = getTraceCompletionRequest(index, criteria, query, 0);

        if (onlyFaulty) {
            FilterBuilder filter = FilterBuilders.queryFilter(QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchQuery(ElasticsearchUtil.PROPERTIES_NAME_FIELD, Constants.PROP_FAULT)));
            request.setPostFilter(FilterBuilders.nestedFilter("properties", filter));
        }

        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
            return 0;
        } else {
            return response.getHits().getTotalHits();
        }
    }

    private SearchRequestBuilder getTraceCompletionRequest(String index, Criteria criteria, BoolQueryBuilder query, int maxSize) {
        return getBaseSearchRequestBuilder(TRACE_COMPLETION_TIME_TYPE, index, criteria, query, maxSize);
    }

    private SearchRequestBuilder getNodeDetailsRequest(String index, Criteria criteria, BoolQueryBuilder query, int maxSize) {
        return getBaseSearchRequestBuilder(NODE_DETAILS_TYPE, index, criteria, query, maxSize);
    }

    private SearchRequestBuilder getBaseSearchRequestBuilder(String type, String index, Criteria criteria, BoolQueryBuilder query, int maxSize) {
        return client.getClient().prepareSearch(index)
                .setTypes(type)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setSize(maxSize)
                .setQuery(query);
    }

    private String serviceName(Terms serviceTerm) {
        if (serviceTerm == null) {
            return null;
        }

        String serviceName = null;
        if (serviceTerm.getBuckets().size() > 0) {
            serviceName = serviceTerm.getBuckets().iterator().next().getKey();
        }

        return serviceName;
    }
}
