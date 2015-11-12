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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

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
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram.Bucket;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.metrics.percentiles.Percentile;
import org.elasticsearch.search.aggregations.metrics.percentiles.PercentilesBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.stats.StatsBuilder;
import org.hawkular.btm.api.model.analytics.CompletionTime;
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.analytics.ResponseTime;
import org.hawkular.btm.api.model.analytics.Statistics;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionCriteria.PropertyCriteria;
import org.hawkular.btm.api.services.ConfigurationService;
import org.hawkular.btm.server.elasticsearch.log.MsgLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    /**  */
    private static final String COMPLETION_TIME_TYPE = "completiontime";

    private static final ObjectMapper mapper = new ObjectMapper();

    private ElasticsearchClient client;

    @Inject
    private ConfigurationService configService;

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

    /**
     * This method gets the configuration service.
     *
     * @return The configuration service
     */
    public ConfigurationService getConfigurationService() {
        return this.configService;
    }

    /**
     * This method sets the configuration service.
     *
     * @param cs The configuration service
     */
    public void setConfigurationService(ConfigurationService cs) {
        this.configService = cs;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getUnboundURIs(java.lang.String, long, long)
     */
    @Override
    public List<URIInfo> getUnboundURIs(String tenantId, long startTime, long endTime) {
        List<URIInfo> ret = new ArrayList<URIInfo>();
        Map<String, URIInfo> map = new HashMap<String, URIInfo>();

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria()
                .setStartTime(startTime)
                .setEndTime(endTime);

        List<BusinessTransaction> fragments = BusinessTransactionServiceElasticsearch.internalQuery(client,
                tenantId, criteria);

        // Process the fragments to identify which URIs are no used in any business transaction
        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);

            if (btxn.initialFragment() && !btxn.getNodes().isEmpty() && btxn.getName() == null) {

                // Check if top level node is Consumer
                if (btxn.getNodes().get(0) instanceof Consumer) {
                    Consumer consumer = (Consumer) btxn.getNodes().get(0);
                    String uri = consumer.getUri();

                    // Check whether URI already known, and that it did not result
                    // in a fault (e.g. want to ignore spurious URIs that are not
                    // associated with a valid transaction)
                    if (!map.containsKey(uri) && consumer.getFault() == null) {
                        URIInfo info = new URIInfo();
                        info.setUri(uri);
                        info.setEndpointType(consumer.getEndpointType());
                        ret.add(info);
                        map.put(uri, info);
                    }
                } else {
                    obtainProducerURIs(btxn.getNodes(), ret, map);
                }
            }
        }

        // Check whether any of the top level URIs are already associated with
        // a business txn config
        if (configService != null) {
            Map<String, BusinessTxnConfig> configs = configService.getBusinessTransactions(tenantId, 0);
            for (BusinessTxnConfig config : configs.values()) {
                if (config.getFilter() != null && config.getFilter().getInclusions() != null) {
                    if (msgLog.isTraceEnabled()) {
                        msgLog.trace("Remove unbound URIs associated with btxn config=" + config);
                    }
                    for (String filter : config.getFilter().getInclusions()) {

                        Iterator<URIInfo> iter = ret.iterator();
                        while (iter.hasNext()) {
                            URIInfo info = iter.next();
                            if (Pattern.matches(filter, info.getUri())) {
                                iter.remove();
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(ret, new Comparator<URIInfo>() {
            @Override
            public int compare(URIInfo arg0, URIInfo arg1) {
                return arg0.getUri().compareTo(arg1.getUri());
            }
        });

        return ret;
    }

    /**
     * This method collects the information regarding URIs for
     * contained producers.
     *
     * @param nodes The nodes
     * @param uris The list of URI info
     * @param map The map of URis to info
     */
    protected void obtainProducerURIs(List<Node> nodes, List<URIInfo> uris, Map<String, URIInfo> map) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node instanceof Producer) {
                String uri = node.getUri();

                if (!map.containsKey(uri)) {
                    URIInfo info = new URIInfo();
                    info.setUri(uri);
                    info.setEndpointType(((Producer) node).getEndpointType());
                    uris.add(info);
                    map.put(uri, info);
                }
            }

            if (node instanceof ContainerNode) {
                obtainProducerURIs(((ContainerNode) node).getNodes(), uris, map);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getBoundURIs(java.lang.String, java.lang.String, long, long)
     */
    @Override
    public List<String> getBoundURIs(String tenantId, String businessTransaction, long startTime, long endTime) {
        List<String> ret = new ArrayList<String>();

        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria()
                .setName(businessTransaction)
                .setStartTime(startTime)
                .setEndTime(endTime);

        List<BusinessTransaction> fragments = BusinessTransactionServiceElasticsearch.internalQuery(client,
                tenantId, criteria);

        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);
            obtainURIs(btxn.getNodes(), ret);
        }

        return ret;
    }

    /**
     * This method collects the information regarding URIs.
     *
     * @param nodes The nodes
     * @param uris The list of URIs
     */
    protected void obtainURIs(List<Node> nodes, List<String> uris) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node.getUri() != null && !uris.contains(node.getUri())) {
                uris.add(node.getUri());
            }

            if (node instanceof ContainerNode) {
                obtainURIs(((ContainerNode) node).getNodes(), uris);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getTransactionCount(java.lang.String,
     *                  org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public long getCompletionCount(String tenantId, BusinessTransactionCriteria criteria) {
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
        } else if (endTime < 0) {
            endTime = System.currentTimeMillis() - endTime;
        }

        if (startTime == 0) {
            // Set to 1 hour before end time
            startTime = endTime - 3600000;
        } else if (startTime < 0) {
            startTime = endTime + startTime;
        }

        BoolQueryBuilder b2 = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("timestamp").from(startTime).to(endTime));

        b2 = b2.must(QueryBuilders.termQuery("businessTransaction", criteria.getName()));

        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded()) {
                    b2 = b2.mustNot(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                } else {
                    b2 = b2.must(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                }
            }
        }

        SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(COMPLETION_TIME_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setSize(criteria.getMaxResponseSize())
                .setQuery(b2);

        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
            return 0;
        } else {
            return response.getHits().getTotalHits();
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getTransactionFaultCount(java.lang.String,
     *              org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public long getCompletionFaultCount(String tenantId, BusinessTransactionCriteria criteria) {
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
        } else if (endTime < 0) {
            endTime = System.currentTimeMillis() - endTime;
        }

        if (startTime == 0) {
            // Set to 1 hour before end time
            startTime = endTime - 3600000;
        } else if (startTime < 0) {
            startTime = endTime + startTime;
        }

        BoolQueryBuilder b2 = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("timestamp").from(startTime).to(endTime));

        b2 = b2.must(QueryBuilders.termQuery("businessTransaction", criteria.getName()));

        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded()) {
                    b2 = b2.mustNot(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                } else {
                    b2 = b2.must(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                }
            }
        }

        FilterBuilder filter = FilterBuilders.existsFilter("fault");

        SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(COMPLETION_TIME_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setSize(criteria.getMaxResponseSize())
                .setQuery(b2)
                .setPostFilter(filter);

        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
            return 0;
        } else {
            return response.getHits().getTotalHits();
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getStats(java.lang.String,
     *                  org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public Percentiles getCompletionPercentiles(String tenantId, BusinessTransactionCriteria criteria) {
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
        } else if (endTime < 0) {
            endTime = System.currentTimeMillis() - endTime;
        }

        if (startTime == 0) {
            // Set to 1 hour before end time
            startTime = endTime - 3600000;
        } else if (startTime < 0) {
            startTime = endTime + startTime;
        }

        BoolQueryBuilder b2 = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("timestamp").from(startTime).to(endTime));

        b2 = b2.must(QueryBuilders.termQuery("businessTransaction", criteria.getName()));

        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded()) {
                    b2 = b2.mustNot(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                } else {
                    b2 = b2.must(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                }
            }
        }

        PercentilesBuilder percentileAgg = AggregationBuilders
                .percentiles("percentiles")
                .field("duration");

        SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(COMPLETION_TIME_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addAggregation(percentileAgg)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setQuery(b2);

        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
        }

        Percentiles percentiles = new Percentiles();

        org.elasticsearch.search.aggregations.metrics.percentiles.Percentiles agg =
                response.getAggregations().get("percentiles");

        for (Percentile entry : agg) {
            percentiles.addPercentile((int) entry.getPercent(), entry.getValue());
        }

        return percentiles;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getCompletionStatistics(java.lang.String,
     *                  org.hawkular.btm.api.services.BusinessTransactionCriteria, long)
     */
    @Override
    public List<Statistics> getCompletionStatistics(String tenantId, BusinessTransactionCriteria criteria,
                                    long interval) {
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
        } else if (endTime < 0) {
            endTime = System.currentTimeMillis() - endTime;
        }

        if (startTime == 0) {
            // Set to 1 hour before end time
            startTime = endTime - 3600000;
        } else if (startTime < 0) {
            startTime = endTime + startTime;
        }

        BoolQueryBuilder b2 = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("timestamp").from(startTime).to(endTime));

        b2 = b2.must(QueryBuilders.termQuery("businessTransaction", criteria.getName()));

        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded()) {
                    b2 = b2.mustNot(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                } else {
                    b2 = b2.must(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                }
            }
        }

        StatsBuilder statsBuilder = AggregationBuilders
                .stats("stats")
                .field("duration");

        DateHistogramBuilder histogramBuilder = AggregationBuilders
                .dateHistogram("histogram")
                .interval(interval)
                .field("timestamp")
                .subAggregation(statsBuilder);

        SearchRequestBuilder request = client.getElasticsearchClient().prepareSearch(index)
                .setTypes(COMPLETION_TIME_TYPE)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .addAggregation(histogramBuilder)
                .setTimeout(TimeValue.timeValueMillis(criteria.getTimeout()))
                .setQuery(b2);

        SearchResponse response = request.execute().actionGet();
        if (response.isTimedOut()) {
            msgLog.warnQueryTimedOut();
        }

        List<Statistics> stats = new ArrayList<Statistics>();

        DateHistogram histogram = response.getAggregations().get("histogram");

        for (Bucket bucket : histogram.getBuckets()) {
            Stats stat = bucket.getAggregations().get("stats");

            Statistics s = new Statistics();
            s.setTimestamp(bucket.getKeyAsDate().getMillis());
            s.setAverage(stat.getAvg());
            s.setMin(stat.getMin());
            s.setMax(stat.getMax());
            s.setCount(stat.getCount());

            stats.add(s);
        }

        return stats;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getAlertCount(java.lang.String, java.lang.String)
     */
    @Override
    public int getAlertCount(String tenantId, String name) {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#storeResponseTimes(java.lang.String, java.util.List)
     */
    @Override
    public void storeResponseTimes(String tenantId, List<ResponseTime> responseTimes) throws Exception {
        client.initTenant(tenantId);

        BulkRequestBuilder bulkRequestBuilder = client.getElasticsearchClient().prepareBulk();

        for (int i = 0; i < responseTimes.size(); i++) {
            ResponseTime rt = responseTimes.get(i);
            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    RESPONSE_TIME_TYPE, rt.getId()).setSource(mapper.writeValueAsString(rt)));
        }

        BulkResponse bulkItemResponses = bulkRequestBuilder.execute().actionGet();

        if (bulkItemResponses.hasFailures()) {

            // TODO: Candidate for retry??? HWKBTM-187
            msgLog.error("Failed to store response times: " + bulkItemResponses.buildFailureMessage());

            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Failed to store response times to elasticsearch: "
                        + bulkItemResponses.buildFailureMessage());
            }
        } else {
            if (msgLog.isTraceEnabled()) {
                msgLog.trace("Success storing response times to elasticsearch");
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
            bulkRequestBuilder.add(client.getElasticsearchClient().prepareIndex(client.getIndex(tenantId),
                    COMPLETION_TIME_TYPE, ct.getId()).setSource(mapper.writeValueAsString(ct)));
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
