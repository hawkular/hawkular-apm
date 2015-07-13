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

import org.hawkular.btm.api.model.admin.BusinessTxnConfig;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;

/**
 * This class manages the filtering of URIs.
 *
 * @author gbrown
 */
public class FilterManager {

    private List<FilterProcessor> globalExclusionFilters = new ArrayList<FilterProcessor>();
    private List<FilterProcessor> btxnFilters = new ArrayList<FilterProcessor>();

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
        for (String btxn : config.getBusinessTransactions().keySet()) {
            BusinessTxnConfig btc = config.getBusinessTransactions().get(btxn);
            FilterProcessor fp = new FilterProcessor(btxn, btc.getFilter());

            if (fp.isIncludeAll()) {
                globalExclusionFilters.add(fp);
            } else {
                btxnFilters.add(fp);
            }
        }
    }

    /**
     * This method determines whether the supplied URI is associated with
     * a defined business transaction, or valid due to global inclusion
     * criteria.
     *
     * @param uri The URI
     * @return The business transaction name, empty if URI globally valid,
     *                  or null if URI should be excluded
     */
    public String getBusinessTransactionName(String uri) {
        String ret = "";

        // First check if a global exclusion filter applies
        for (int i = 0; i < globalExclusionFilters.size(); i++) {
            if (globalExclusionFilters.get(i).isExcluded(uri)) {
                return null;
            }
        }

        // Check if business transaction specific applies
        for (int i = 0; i < btxnFilters.size(); i++) {
            if (btxnFilters.get(i).isIncluded(uri)) {
                if (btxnFilters.get(i).isExcluded(uri)) {
                    return null;
                }
                ret = btxnFilters.get(i).getBusinessTransaction();
                break;
            }
        }

        return ret;
    }
}
