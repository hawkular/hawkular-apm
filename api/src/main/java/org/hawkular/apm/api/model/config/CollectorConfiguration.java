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
package org.hawkular.apm.api.model.config;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.instrumentation.Instrumentation;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.utils.PropertyUtil;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This is the top level configuration object used to define how information should
 * be collected from a trace execution environment.
 *
 * @author gbrown
 */
public class CollectorConfiguration {

    private static final Logger log=Logger.getLogger(CollectorConfiguration.class.getName());

    @JsonInclude
    private Map<String, String> properties = new HashMap<String, String>();

    @JsonInclude
    private Map<String, Instrumentation> instrumentation = new HashMap<String, Instrumentation>();

    @JsonInclude
    private Map<String, TransactionConfig> transactions = new HashMap<String, TransactionConfig>();

    /**
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * This method returns the property associated with the supplied name. The
     * system properties will be checked first, and if not available, then the
     * collector configuration properties.
     *
     * @param name The name of the required property
     * @param def The optional default value
     * @return The property value, or if not found then the default
     */
    public String getProperty(String name, String def) {
        String ret=PropertyUtil.getProperty(name, null);

        if (ret == null) {
            if (getProperties().containsKey(name)) {
                ret = getProperties().get(name);
            } else {
                ret = def;
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get property '"+name+"' (default="+def+") = "+ret);
        }
        return ret;
    }

    /**
     * @return the instrumentation
     */
    public Map<String, Instrumentation> getInstrumentation() {
        return instrumentation;
    }

    /**
     * @param instrumentation the instrumentation to set
     */
    public void setInstrumentation(Map<String, Instrumentation> instrumentation) {
        this.instrumentation = instrumentation;
    }

    /**
     * @return the transactions
     */
    public Map<String, TransactionConfig> getTransactions() {
        return transactions;
    }

    /**
     * @param transactions the transactions to set
     */
    public void setTransactions(Map<String, TransactionConfig> transactions) {
        this.transactions = transactions;
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
                throw new IllegalArgumentException("Instrumentation for '" + key + "' already exists");
            }
            getInstrumentation().put(key, config.getInstrumentation().get(key));
        }
        for (String key : config.getTransactions().keySet()) {
            if (getTransactions().containsKey(key) && !overwrite) {
                throw new IllegalArgumentException("Transaction config for '" + key + "' already exists");
            }
            getTransactions().put(key, config.getTransactions().get(key));
        }
        getProperties().putAll(config.getProperties());
    }
}
