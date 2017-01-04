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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;

/**
 * This class manages the filtering of URIs.
 *
 * @author gbrown
 */
public class FilterManager {

    private static final Logger log = Logger.getLogger(FilterManager.class.getName());

    private Map<String, FilterProcessor> filterMap = new HashMap<String, FilterProcessor>();
    private List<FilterProcessor> globalExclusionFilters = new ArrayList<FilterProcessor>();
    private List<FilterProcessor> btxnFilters = new ArrayList<FilterProcessor>();

    private boolean onlyNamedTransactions = false;

    private static final FilterProcessor unnamedBTxn = new FilterProcessor();

    /**
     * This constructor initialises the filter manager with the configuration.
     *
     * @param config The configuration
     */
    public FilterManager(CollectorConfiguration config) {
        init(config);
    }

    /**
     * This method initialises the filter manager.
     *
     * @param config The configuration
     */
    protected void init(CollectorConfiguration config) {
        for (String btxn : config.getTransactions().keySet()) {
            TransactionConfig btc = config.getTransactions().get(btxn);
            init(btxn, btc);
        }

        onlyNamedTransactions = new Boolean(config.getProperty(
                "HAWKULAR_APM_COLLECTOR_ONLYNAMED", Boolean.FALSE.toString()));
    }

    /**
     * This method initialises the filter manager with the supplied
     * transaction configuration.
     *
     * @param txn The transaction name
     * @param btc The configuration
     */
    public void init(String txn, TransactionConfig btc) {
        FilterProcessor fp = null;

        if (btc.getFilter() != null) {
            fp = new FilterProcessor(txn, btc);
        }

        synchronized (filterMap) {
            // Check if old filter processor needs to be removed
            FilterProcessor oldfp = filterMap.get(txn);
            if (oldfp != null) {
                globalExclusionFilters.remove(oldfp);
                btxnFilters.remove(oldfp);
            }

            if (fp != null) {
                // Add new filter processor
                filterMap.put(txn, fp);
                if (fp.isIncludeAll()) {
                    globalExclusionFilters.add(fp);
                } else {
                    btxnFilters.add(fp);
                }
            } else {
                filterMap.remove(txn);
            }
        }
    }

    /**
     * This method removes the transaction.
     *
     * @param txn The name of the transaction
     */
    public void remove(String txn) {
        synchronized (filterMap) {
            // Check if old filter processor needs to be removed
            FilterProcessor oldfp = filterMap.get(txn);
            if (oldfp != null) {
                globalExclusionFilters.remove(oldfp);
                btxnFilters.remove(oldfp);
            }
        }
    }

    /**
     * This method determines whether the supplied endpoint is associated with
     * a defined transaction, or valid due to global inclusion
     * criteria.
     *
     * @param endpoint The endpoint
     * @return The filter processor, with empty txn name if endpoint globally valid,
     *                  or null if endpoint should be excluded
     */
    public FilterProcessor getFilterProcessor(String endpoint) {
        FilterProcessor ret = (onlyNamedTransactions ? null : unnamedBTxn);

        synchronized (filterMap) {
            // First check if a global exclusion filter applies
            for (int i = 0; i < globalExclusionFilters.size(); i++) {
                if (globalExclusionFilters.get(i).isExcluded(endpoint)) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Excluding endpoint=" + endpoint);
                    }
                    return null;
                }
            }

            // Check if transaction specific applies
            for (int i = 0; i < btxnFilters.size(); i++) {
                if (btxnFilters.get(i).isIncluded(endpoint)) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Endpoint has passed inclusion filter: endpoint=" + endpoint);
                    }
                    if (btxnFilters.get(i).isExcluded(endpoint)) {
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("Endpoint has failed exclusion filter: endpoint=" + endpoint);
                        }
                        return null;
                    }
                    ret = btxnFilters.get(i);

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Endpoint belongs to transaction '" + ret + ": endpoint=" + endpoint);
                    }
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * @return the filterMap
     */
    protected Map<String, FilterProcessor> getFilterMap() {
        return filterMap;
    }

    /**
     * @param filterMap the filterMap to set
     */
    protected void setFilterMap(Map<String, FilterProcessor> filterMap) {
        this.filterMap = filterMap;
    }

    /**
     * @return the globalExclusionFilters
     */
    protected List<FilterProcessor> getGlobalExclusionFilters() {
        return globalExclusionFilters;
    }

    /**
     * @param globalExclusionFilters the globalExclusionFilters to set
     */
    protected void setGlobalExclusionFilters(List<FilterProcessor> globalExclusionFilters) {
        this.globalExclusionFilters = globalExclusionFilters;
    }

    /**
     * @return the btxnFilters
     */
    protected List<FilterProcessor> getBtxnFilters() {
        return btxnFilters;
    }

    /**
     * @param btxnFilters the btxnFilters to set
     */
    protected void setBtxnFilters(List<FilterProcessor> btxnFilters) {
        this.btxnFilters = btxnFilters;
    }

}
