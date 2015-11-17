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

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.services.BaseCriteria;
import org.hawkular.btm.api.services.BaseCriteria.PropertyCriteria;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.CompletionTimeCriteria;
import org.hawkular.btm.api.services.CompletionTimeCriteria.FaultCriteria;

/**
 * This class provides utility functions for working with Elasticsearch.
 *
 * @author gbrown
 */
public class ElasticsearchUtil {

    /**
     * This method builds the Elasticsearch query based on the supplied
     * business transaction criteria.
     *
     * @param timeProperty The name of the time property
     * @param businessTxnProperty The name of the business transaction property
     * @param criteria the criteria
     * @return The query
     */
    public static BoolQueryBuilder buildQuery(BusinessTransactionCriteria criteria, String timeProperty,
            String businessTxnProperty) {
        BoolQueryBuilder query = buildBaseQuery(criteria, timeProperty, businessTxnProperty);

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

        return query;
    }

    /**
     * This method builds the Elasticsearch query based on the supplied
     * completion time criteria.
     *
     * @param timeProperty The name of the time property
     * @param businessTxnProperty The name of the business transaction property
     * @param criteria the criteria
     * @return The query
     */
    public static BoolQueryBuilder buildQuery(CompletionTimeCriteria criteria, String timeProperty,
            String businessTxnProperty) {
        BoolQueryBuilder query = buildBaseQuery(criteria, timeProperty, businessTxnProperty);

        if (!criteria.getFaults().isEmpty()) {
            for (FaultCriteria fc : criteria.getFaults()) {
                if (fc.isExcluded()) {
                    query = query.mustNot(QueryBuilders.matchQuery("fault", fc.getValue()));
                } else {
                    query = query.must(QueryBuilders.matchQuery("fault", fc.getValue()));
                }
            }
        }

        if (criteria.getLowerBound() > 0 || criteria.getUpperBound() > 0) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("duration");
            if (criteria.getLowerBound() > 0) {
                rangeQuery.gte(criteria.getLowerBound());
            }
            if (criteria.getUpperBound() > 0) {
                rangeQuery.lte(criteria.getUpperBound());
            }
            query = query.must(rangeQuery);
        }

        return query;
    }

    /**
     * This method builds the Elasticsearch query based on the supplied
     * business transaction criteria.
     *
     * @param timeProperty The name of the time property
     * @param businessTxnProperty The name of the business transaction property
     * @param criteria the criteria
     * @return The query
     */
    private static BoolQueryBuilder buildBaseQuery(BaseCriteria criteria, String timeProperty,
            String businessTxnProperty) {
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

        BoolQueryBuilder query = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery(timeProperty).from(startTime).to(endTime));

        if (criteria.getBusinessTransaction() != null
                && criteria.getBusinessTransaction().trim().length() > 0) {
            query = query.must(QueryBuilders.termQuery(businessTxnProperty, criteria.getBusinessTransaction()));
        }

        if (!criteria.getProperties().isEmpty()) {
            for (PropertyCriteria pc : criteria.getProperties()) {
                if (pc.isExcluded()) {
                    query = query.mustNot(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                } else {
                    query = query.must(QueryBuilders.matchQuery("properties." + pc.getName(), pc.getValue()));
                }
            }
        }

        return query;
    }

    /**
     * This method returns a filter associated with the supplied business transaction
     * criteria.
     *
     * @param criteria The business transaction criteria
     * @return The filter, or null if not relevant
     */
    public static FilterBuilder buildFilter(BusinessTransactionCriteria criteria) {
        if (criteria.getBusinessTransaction() != null && criteria.getBusinessTransaction().trim().length() == 0) {
            return FilterBuilders.missingFilter("name");
        }
        return null;
    }

}
