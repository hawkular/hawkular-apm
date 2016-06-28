/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.apm.instrumenter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.instrumentation.Instrumentation;
import org.hawkular.apm.api.services.ConfigurationService;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.TraceCollector;
import org.hawkular.apm.instrumenter.rules.RuleTransformer;
import org.jboss.byteman.agent.Retransformer;

/**
 * This class provides the ByteMan manager implementation for APM.
 *
 * @author gbrown
 */
public class ClientManager {

    /** The number of seconds to wait before trying again to retrieve the collector config */
    private static final int DEFAULT_CONFIG_RETRY_INTERVAL = 10;

    private static final Logger log = Logger.getLogger(ClientManager.class.getName());

    private static Retransformer transformer;

    private static RuleTransformer ruleTransformer = new RuleTransformer();

    private static TraceCollector collector;
    private static ConfigurationService configService;

    private static Integer configRetryInterval =
            PropertyUtil.getPropertyAsInteger(PropertyUtil.HAWKULAR_APM_CONFIG_REFRESH, DEFAULT_CONFIG_RETRY_INTERVAL);

    /**
     * This method initializes the manager.
     *
     * @param trans The ByteMan retransformer
     */
    public static void initialize(Retransformer trans) {
        // NOTE: Using stdout/err as jul had a side effect initializing jboss logging
        if (log.isLoggable(Level.FINER)) {
            log.finer("Initializing Client Manager");
        }

        transformer = trans;

        // Obtain collector
        collector = ServiceResolver.getSingletonService(TraceCollector.class);

        if (log.isLoggable(Level.FINER)) {
            log.finer("Trace Collector: " + collector);
        }

        // Obtain the configuration service
        configService = ServiceResolver.getSingletonService(ConfigurationService.class);

        if (log.isLoggable(Level.FINER)) {
            log.finer("Configuration Service: " + configService);
        }

        if (configService != null) {
            initConfig();
        } else {
            log.info("APM has not been configured");
        }
    }

    protected static void initConfig() {
        // Read configuration
        CollectorConfiguration config = configService.getCollector(null, null, null, null);

        if (config != null) {
            try {
                updateInstrumentation(config);
            } catch (Exception e) {
                log.severe("Failed to update instrumentation rules: " + e);
            }
        } else {
            log.warning("Unable to obtain APM configuration, will retry ...");

            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            }).schedule(new Runnable() {
                @Override
                public void run() {
                    initConfig();
                }
            }, configRetryInterval, TimeUnit.SECONDS);
        }

    }
    /**
     * This method updates the instrumentation instructions.
     *
     * @param config The collector configuration
     * @throws Exception Failed to update instrumentation rules
     */
    public static void updateInstrumentation(CollectorConfiguration config) throws Exception {
        List<String> scripts = new ArrayList<String>();
        List<String> scriptNames = new ArrayList<String>();
        Map<String, Instrumentation> instrumentTypes=config.getInstrumentation();

        for (Map.Entry<String, Instrumentation> stringInstrumentationEntry : instrumentTypes.entrySet()) {
            Instrumentation types = stringInstrumentationEntry.getValue();
            String rules = ruleTransformer.transform(stringInstrumentationEntry.getKey(), types,
                            config.getProperty("version."+ stringInstrumentationEntry.getKey(), null));

            if (log.isLoggable(Level.FINER)) {
                log.finer("Update instrumentation script name=" + stringInstrumentationEntry.getKey() + " rules=" + rules);
            }

            if (rules != null) {
                scriptNames.add(stringInstrumentationEntry.getKey());
                scripts.add(rules);
            }
        }

        PrintWriter writer = new PrintWriter(new StringWriter());

        transformer.installScript(scripts, scriptNames, writer);

        writer.close();
    }
}
