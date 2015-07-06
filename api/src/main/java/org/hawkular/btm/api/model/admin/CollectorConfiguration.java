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
package org.hawkular.btm.api.model.admin;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This is the top level configuration object used to define how information should
 * be collected from a business transaction execution environment.
 *
 * @author gbrown
 */
public class CollectorConfiguration {

    @JsonInclude
    private Map<String,Instrumentation> instrumentation = new HashMap<String,Instrumentation>();

    @JsonInclude
    private Map<String,BusinessTxnConfig> businessTransactions = new HashMap<String,BusinessTxnConfig>();

    /**
     * @return the instrumentation
     */
    public Map<String,Instrumentation> getInstrumentation() {
        return instrumentation;
    }

    /**
     * @param instrumentation the instrumentation to set
     */
    public void setInstrumentation(Map<String,Instrumentation> instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     * @return the businessTransactions
     */
    public Map<String, BusinessTxnConfig> getBusinessTransactions() {
        return businessTransactions;
    }

    /**
     * @param businessTransactions the businessTransactions to set
     */
    public void setBusinessTransactions(Map<String, BusinessTxnConfig> businessTransactions) {
        this.businessTransactions = businessTransactions;
    }

    /**
     * This method merges the supplied configuration into this configuration. If
     * a conflict is found, if overwrite is true then the supplied config element
     * will be used, otherwise an exception will be raised.
     *
     * @param config The configuration to merge
     * @param overwrite Whether to overwrite when conflict found
     * @throws IllegalArgumentException Failed to merge due to a conflict
     */
    public void merge(CollectorConfiguration config, boolean overwrite)
                        throws IllegalArgumentException {
        for (String key : config.getInstrumentation().keySet()) {
            if (getInstrumentation().containsKey(key) && !overwrite) {
                throw new IllegalArgumentException("Instrumentation for '"+key+"' already exists");
            }
            getInstrumentation().put(key, config.getInstrumentation().get(key));
        }
        for (String key : config.getBusinessTransactions().keySet()) {
            if (getBusinessTransactions().containsKey(key) && !overwrite) {
                throw new IllegalArgumentException("Business Transaction config for '"+key+"' already exists");
            }
            getBusinessTransactions().put(key, config.getBusinessTransactions().get(key));
        }
    }
}
