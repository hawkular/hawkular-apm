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

import java.util.concurrent.TimeUnit;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.Criteria.Operator;
import org.hawkular.apm.api.services.Criteria.PropertyCriteria;

/**
 * This class provides utility functions for working with Elasticsearch.
 *
 * @author gbrown
 */
public class ElasticsearchUtil {

    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String DURATION_FIELD = "duration";
    public static final String NODES_FIELD = "nodes";
    public static final String URI_FIELD = "uri";
    public static final String OPERATION_FIELD = "operation";
    public static final String TRACE_ID_FIELD = "traceId";
    public static final String FRAGMENT_ID_FIELD = "fragmentId";
    public static final String TYPE_FIELD = "type";
    public static final String COMPONENT_TYPE_FIELD = "componentType";
    public static final String HOST_NAME_FIELD = "hostName";
    public static final String HOST_ADDRESS_FIELD = "hostAddress";
    public static final String TRANSACTION_FIELD = "transaction";
    public static final String PROPERTIES_FIELD = "properties";
    public static final String PROPERTIES_NAME_FIELD = "properties.name";
    public static final String PROPERTIES_VALUE_FIELD = "properties.value";
    public static final String PROPERTIES_NUMBER_FIELD = "properties.number";
    public static final String ELAPSED_FIELD = "elapsed";
    public static final String ACTUAL_FIELD = "actual";
    public static final String LATENCY_FIELD = "latency";
    public static final String SOURCE_FIELD = "source";
    public static final String TARGET_FIELD = "target";

    /**
     * This method builds the Elasticsearch query based on the supplied
     * criteria.
     *
     * @param criteria the criteria
     * @param txnProperty The name of the transaction property
     * @param targetClass The class being queried
     * @return The query
     */
    public static BoolQueryBuilder buildQuery(Criteria criteria, String txnProperty, Class<?> targetClass) {
        /**
         * Internally all time units are stored in microseconds
         * Criteria API accepts milliseconds, therefore range adjustment is needed
         */
        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery(TIMESTAMP_FIELD)
                        .from(TimeUnit.MILLISECONDS.toMicros(criteria.calculateStartTime()))
                        .to(TimeUnit.MILLISECONDS.toMicros(criteria.calculateEndTime())));

        if (criteria.getTransaction() != null
                && !criteria.getTransaction().trim().isEmpty()) {
            query = query.must(QueryBuilders.termQuery(txnProperty, criteria.getTransaction()));
        }

        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.getOperator() == Operator.HAS
                        || pc.getOperator() == Operator.HASNOT
                        || pc.getOperator() == Operator.EQ
                        || pc.getOperator() == Operator.NE) {
                    BoolQueryBuilder nestedQuery = QueryBuilders.boolQuery()
                            .must(QueryBuilders.matchQuery(PROPERTIES_NAME_FIELD, pc.getName()))
                            .must(QueryBuilders.matchQuery(PROPERTIES_VALUE_FIELD, pc.getValue()));
                    if (pc.getOperator() == Operator.HASNOT
                            || pc.getOperator() == Operator.NE) {
                        query = query.mustNot(QueryBuilders.nestedQuery(PROPERTIES_FIELD, nestedQuery));
                    } else {
                        query = query.must(QueryBuilders.nestedQuery(PROPERTIES_FIELD, nestedQuery));
                    }
                } else {
                    // Numerical query
                    RangeQueryBuilder rangeQuery = null;
                    if (pc.getOperator() == Operator.GTE) {
                        rangeQuery = QueryBuilders.rangeQuery(PROPERTIES_NUMBER_FIELD).gte(pc.getValue());
                    } else if (pc.getOperator() == Operator.GT) {
                        rangeQuery = QueryBuilders.rangeQuery(PROPERTIES_NUMBER_FIELD).gt(pc.getValue());
                    } else if (pc.getOperator() == Operator.LTE) {
                        rangeQuery = QueryBuilders.rangeQuery(PROPERTIES_NUMBER_FIELD).lte(pc.getValue());
                    } else if (pc.getOperator() == Operator.LT) {
                        rangeQuery = QueryBuilders.rangeQuery(PROPERTIES_NUMBER_FIELD).lt(pc.getValue());
                    } else {
                        throw new IllegalArgumentException("Unknown property criteria operator: "+pc);
                    }
                    BoolQueryBuilder nestedQuery = QueryBuilders.boolQuery()
                            .must(QueryBuilders.matchQuery(PROPERTIES_NAME_FIELD, pc.getName()))
                            .must(rangeQuery);
                    query = query.must(QueryBuilders.nestedQuery(PROPERTIES_FIELD, nestedQuery));
                }
            }
        }

        if (criteria.getHostName() != null && !criteria.getHostName().trim().isEmpty()) {
            query = query.must(QueryBuilders.matchQuery("hostName", criteria.getHostName()));
        }

        if (!criteria.getCorrelationIds().isEmpty()) {
            for (CorrelationIdentifier id : criteria.getCorrelationIds()) {
                query.must(QueryBuilders.termQuery("value", id.getValue()));
                /* HWKBTM-186
                b2 = b2.must(QueryBuilders.nestedQuery("nodes.correlationIds", // Path
                        QueryBuilders.boolQuery()
                                .must(QueryBuilders.matchQuery("correlationIds.scope", id.getScope()))
                                .must(QueryBuilders.matchQuery("correlationIds.value", id.getValue()))));
                 */
            }
        }

        if (criteria.getLowerBound() > 0
                || criteria.getUpperBound() > 0) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(DURATION_FIELD);
            if (criteria.getLowerBound() > 0) {
                rangeQuery.gte(criteria.getLowerBound());
            }
            if (criteria.getUpperBound() > 0) {
                rangeQuery.lte(criteria.getUpperBound());
            }
            query = query.must(rangeQuery);
        }

        // Querying uri and operation are only relevant to NodeDetails and CompletionTime
        if (targetClass == NodeDetails.class || targetClass == CompletionTime.class) {
            if (criteria.getUri() != null && !criteria.getUri().trim().isEmpty()) {
                query = query.must(QueryBuilders.matchQuery(URI_FIELD, criteria.getUri()));
            }
            if (criteria.getOperation() != null && !criteria.getOperation().trim().isEmpty()) {
                query = query.must(QueryBuilders.matchQuery(OPERATION_FIELD, criteria.getOperation()));
            }
        }

        return query;
    }

    /**
     * This method returns a filter associated with the supplied criteria.
     *
     * @param criteria The criteria
     * @return The filter, or null if not relevant
     */
    public static FilterBuilder buildFilter(Criteria criteria) {
        if (criteria.getTransaction() != null && criteria.getTransaction().trim().isEmpty()) {
            return FilterBuilders.missingFilter(TRANSACTION_FIELD);
        }
        return null;
    }

}
