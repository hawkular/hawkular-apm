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
package org.hawkular.btm.client.collector.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.hawkular.btm.api.model.config.btxn.Filter;

/**
 * This class is responsible for applying a filter to a supplied node.
 *
 * @author gbrown
 */
public class FilterProcessor {

    private String businessTransaction;

    private Filter filter;

    private List<Predicate<String>> inclusions = new ArrayList<Predicate<String>>();
    private List<Predicate<String>> exclusions = new ArrayList<Predicate<String>>();

    /**
     * This constructor initialises the processor with the business
     * transaction name and filter configuration.
     *
     * @param btxn The business transaction name
     * @param filter The filter configuration
     */
    public FilterProcessor(String btxn, Filter filter) {
        this.setBusinessTransaction(btxn);
        this.setFilter(filter);
        init();
    }

    /**
     * This method initialises the filter.
     */
    protected void init() {
        for (int i = 0; i < filter.getInclusions().size(); i++) {
            inclusions.add(Pattern.compile(filter.getInclusions().get(i)).asPredicate());
        }
        for (int i = 0; i < filter.getExclusions().size(); i++) {
            exclusions.add(Pattern.compile(filter.getExclusions().get(i)).asPredicate());
        }
    }

    /**
     * @return the businessTransaction
     */
    public String getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * @param businessTransaction the businessTransaction to set
     */
    public void setBusinessTransaction(String businessTransaction) {
        this.businessTransaction = businessTransaction;
    }

    /**
     * @return the filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * @param filter the filter to set
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * This method determines whether all URIs are included.
     *
     * @return Whether 'all' URIs are included
     */
    public boolean isIncludeAll() {
        return (inclusions.isEmpty());
    }

    /**
     * This method determines whether the supplied URI should be
     * included.
     *
     * @param uri The URI to check
     * @return Whether the supplied URI should be included
     */
    public boolean isIncluded(String uri) {
        for (int i = 0; i < inclusions.size(); i++) {
            if (inclusions.get(i).test(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method determines whether the supplied URI should be
     * excluded.
     *
     * @param uri The URI to check
     * @return Whether the supplied URI should be excluded
     */
    public boolean isExcluded(String uri) {
        for (int i = 0; i < exclusions.size(); i++) {
            if (exclusions.get(i).test(uri)) {
                return true;
            }
        }
        return false;
    }
}
