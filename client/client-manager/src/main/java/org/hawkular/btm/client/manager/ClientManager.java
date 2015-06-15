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
package org.hawkular.btm.client.manager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.hawkular.btm.api.client.BusinessTransactionCollector;
import org.hawkular.btm.api.client.ConfigurationManager;
import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;
import org.hawkular.btm.api.model.admin.Instrumentation;
import org.hawkular.btm.api.util.ServiceResolver;
import org.hawkular.btm.client.manager.config.Transformer;
import org.jboss.byteman.agent.Retransformer;

/**
 * This class provides the ByteMan manager implementation for BTM.
 *
 * @author gbrown
 */
public class ClientManager {

    private static final Logger log = Logger.getLogger(ClientManager.class.getName());

    private static Retransformer transformer;

    private static Transformer ruleTransformer = new Transformer();

    private static BusinessTransactionCollector collector;
    private static ConfigurationManager configManager;

    /**
     * This method initializes the manager.
     *
     * @param trans The ByteMan retransformer
     */
    public static void initialize(Retransformer trans) {
        // NOTE: Using stdout/err as jul had a side effect initializing jboss logging
        log.info("BTM: Initializing Client Manager");

        transformer = trans;

        // Obtain collector
        CompletableFuture<BusinessTransactionCollector> colFuture =
                ServiceResolver.getSingletonService(BusinessTransactionCollector.class);

        colFuture.whenComplete(new BiConsumer<BusinessTransactionCollector, Throwable>() {

            @Override
            public void accept(BusinessTransactionCollector c, Throwable t) {
                log.info("BTM: Initialising Business Transaction Collector: " + c + " exception=" + t);

                if (c != null) {
                    collector = c;
                } else if (t != null) {
                    System.err.println("Failed to locate Business Transaction Collector: " + t);
                    t.printStackTrace();
                }
            }
        });

        // Obtain the configuration manager
        CompletableFuture<ConfigurationManager> cmFuture =
                ServiceResolver.getSingletonService(ConfigurationManager.class);

        cmFuture.whenComplete(new BiConsumer<ConfigurationManager, Throwable>() {

            @Override
            public void accept(ConfigurationManager cm, Throwable t) {
                log.info("BTM: Initialising Configuration Manager: " + cm + " exception=" + t);

                configManager = cm;

                if (configManager == null) {
                    System.err.println("Unable to locate Configuration Manager: " + t);
                    if (t != null) {
                        t.printStackTrace();
                    }
                } else {
                    // Read configuration
                    CollectorConfiguration config = configManager.getConfiguration();

                    if (config != null) {
                        try {
                            updateInstrumentation(config.getInstrumentation());
                        } catch (Exception e) {
                            System.err.println("Failed to update instrumentation rules: " + e);
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    /**
     * This method returns the collector.
     *
     * @return The collector
     */
    public static BusinessTransactionCollector collector() {
        return collector;
    }

    /**
     * This method updates the instrumentation instructions.
     *
     * @param instrumentTypes The instrumentation types
     * @throws Exception Failed to update instrumentation rules
     */
    public static void updateInstrumentation(Map<String, Instrumentation> instrumentTypes) throws Exception {
        List<String> scripts = new ArrayList<String>();
        List<String> scriptNames = new ArrayList<String>();

        for (String name : instrumentTypes.keySet()) {
            Instrumentation types = instrumentTypes.get(name);
            String rules = ruleTransformer.transform(types);

            if (log.isLoggable(Level.FINER)) {
                log.finer("Update instrumentation script name=" + name + " rules=" + rules);
            }

            if (rules != null) {
                scriptNames.add(name);
                scripts.add(rules);
            }
        }

        PrintWriter writer = new PrintWriter(new StringWriter());

        transformer.installScript(scripts, scriptNames, writer);

        writer.close();
    }
}
