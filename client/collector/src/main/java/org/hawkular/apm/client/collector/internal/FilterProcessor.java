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
package org.hawkular.apm.client.collector.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.hawkular.apm.api.model.config.txn.TransactionConfig;

/**
 * This class is responsible for applying a filter to a supplied node.
 *
 * @author gbrown
 */
public class FilterProcessor {

    private String transaction;

    private TransactionConfig config;

    private List<Predicate<String>> inclusions = new ArrayList<Predicate<String>>();
    private List<Predicate<String>> exclusions = new ArrayList<Predicate<String>>();

    /**
     * The default constructor.
     */
    protected FilterProcessor() {
    }

    /**
     * This constructor initialises the processor with the
     * transaction name and configuration.
     *
     * @param transaction The transaction name
     * @param config The configuration
     */
    public FilterProcessor(String transaction, TransactionConfig config) {
        this.transaction = transaction;
        this.config = config;
        init();
    }

    /**
     * This method initialises the filter.
     */
    protected void init() {
        for (int i = 0; i < config.getFilter().getInclusions().size(); i++) {
            inclusions.add(Pattern.compile(config.getFilter().getInclusions().get(i)).asPredicate());
        }
        for (int i = 0; i < config.getFilter().getExclusions().size(); i++) {
            exclusions.add(Pattern.compile(config.getFilter().getExclusions().get(i)).asPredicate());
        }
    }

    /**
     * @return the transaction
     */
    public String getTransaction() {
        return transaction;
    }

    /**
     * @param transaction the transaction to set
     */
    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    /**
     * @return the config
     */
    public TransactionConfig getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(TransactionConfig config) {
        this.config = config;
    }

    /**
     * This method determines whether all endpoints are included.
     *
     * @return Whether 'all' endpoints are included
     */
    public boolean isIncludeAll() {
        return (inclusions.isEmpty());
    }

    /**
     * This method determines whether the supplied endpoint should be
     * included.
     *
     * @param endpoint The endpoint to check
     * @return Whether the supplied endpoint should be included
     */
    public boolean isIncluded(String endpoint) {
        for (int i = 0; i < inclusions.size(); i++) {
            if (inclusions.get(i).test(endpoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method determines whether the supplied endpoint should be
     * excluded.
     *
     * @param endpoint The endpoint to check
     * @return Whether the supplied endpoint should be excluded
     */
    public boolean isExcluded(String endpoint) {
        for (int i = 0; i < exclusions.size(); i++) {
            if (exclusions.get(i).test(endpoint)) {
                return true;
            }
        }
        return false;
    }
}
