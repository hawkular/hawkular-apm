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
package org.hawkular.apm.tools.instrumenter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.instrumentation.Instrumentation;
import org.hawkular.apm.instrumenter.rules.RuleTransformer;
import org.jboss.byteman.agent.Transformer;
import org.jboss.byteman.check.RuleCheck;
import org.jboss.byteman.modules.NonModuleSystem;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides utility functions for instrumenting an archive at build time.
 *
 * @author gbrown
 */
public class InstrumenterUtil {

    private static final Logger log = Logger.getLogger(InstrumenterUtil.class.getName());

    private static final RuleTransformer ruleTransformer = new RuleTransformer();

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The main function for the instrumenter util.
     *
     * @param args The archive path and config json path
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: InstrumenterUtil archive config output");
            System.exit(1);
        }

        File archiveFile = new File(args[0]);

        if (!archiveFile.exists()) {
            System.err.println("Archive file does not exist - must be a jar");
            System.exit(1);
        }

        if (!archiveFile.isAbsolute()) {
            System.err.println("Archive file must have an absolute path");
            System.exit(1);
        }

        File configFile = new File(args[1]);

        if (!configFile.exists()) {
            System.err.println("Config file does not exist");
            System.exit(1);
        }

        JavaArchive archive = ShrinkWrap.createFromZipFile(JavaArchive.class, archiveFile);

        try {
            CollectorConfiguration config = mapper.readValue(configFile, CollectorConfiguration.class);

            if (instrument(archive, config)) {

                // Write back the modified jar
                archive.as(ZipExporter.class).exportTo(
                        new File(args[2]), true);
                System.out.println("Archive instrumented");
            } else {
                System.out.println("No classes were instrumented");
            }
        } catch (Exception e) {
            System.err.println("Failed to instrument: " + e);
        }
    }

    /**
     * This method attempts to instrument the classes within the supplied archive using
     * the instrumentation rules associated with the supplied configuration. If the
     * classes are instrumented, then the return value will be true.
     *
     * @param archive The archive to be instrumented
     * @param config The instrumentation rules
     * @return Whether the archive was instrumented (changed)
     */
    public static boolean instrument(JavaArchive archive, CollectorConfiguration config) {
        boolean modified = false;

        Map<ArchivePath, Node> content = archive.getContent(new Filter<ArchivePath>() {

            @Override
            public boolean include(ArchivePath path) {
                return path.get().endsWith(".class");
            }

        });

        List<String> scripts = new ArrayList<String>();
        List<String> scriptNames = new ArrayList<String>();
        Map<String, Instrumentation> instrumentTypes = config.getInstrumentation();

        for (Map.Entry<String, Instrumentation> stringInstrumentationEntry : instrumentTypes.entrySet()) {
            Instrumentation types = stringInstrumentationEntry.getValue();
            String rules = ruleTransformer.transform(stringInstrumentationEntry.getKey(), types,
                    config.getProperty("version." + stringInstrumentationEntry.getKey(), null));

            if (rules != null) {
                scriptNames.add(stringInstrumentationEntry.getKey());
                scripts.add(rules);
            }
        }

        try {
            NonModuleSystem moduleSystem = new NonModuleSystem();

            Transformer transformer = new Transformer(null, moduleSystem, scriptNames, scripts, false);

            for (Map.Entry<ArchivePath, Node> archivePathNodeEntry : content.entrySet()) {
                Node node = archivePathNodeEntry.getValue();

                InputStream is = node.getAsset().openStream();
                byte[] cls = new byte[is.available()];
                is.read(cls);
                is.close();

                String clsName = archivePathNodeEntry.getKey().get();
                clsName = clsName.replace(java.io.File.separatorChar, '.').substring(1, clsName.length() - 6);

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Check whether to instrument class '" + clsName + "' length=" + cls.length);
                }

                byte[] newcls = transformer.transform(null, clsName, null, null, cls);

                if (newcls != null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Instrumented class '" + clsName + "' length=" + newcls.length);
                    }

                    archive.delete(archivePathNodeEntry.getKey());

                    Asset asset = new Asset() {
                        @Override
                        public InputStream openStream() {
                            return new ByteArrayInputStream(newcls);
                        }
                    };
                    archive.add(asset, archivePathNodeEntry.getKey());

                    modified = true;
                }
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to instrument archive", e);
        }

        return modified;
    }

    /**
     * This method returns the complete collector configuration for the requested version.
     *
     * @param type The client environment type
     * @param version The version
     * @param offline Whether to obtain the config offline (otherwise contacts remove maven repo)
     * @return The collector configuration, or null if not found
     * @throws Exception Failed to get collector configuration
     */
    public static CollectorConfiguration getCollectorConfiguration(String type, String version, boolean offline)
            throws Exception {
        CollectorConfiguration config = null;

        JavaArchive[] jars = null;

        if (offline) {
            jars = Maven.configureResolver().workOffline()
                .resolve("org.hawkular.apm:hawkular-apm-instrumentation-" + type + ":" + version)
                .withoutTransitivity().as(JavaArchive.class);
        } else {
            jars = Maven.resolver()
                .resolve("org.hawkular.apm:hawkular-apm-instrumentation-" + type + ":" + version)
                .withoutTransitivity().as(JavaArchive.class);
        }

        if (jars != null && jars.length > 0) {
            Map<ArchivePath, Node> content = jars[0].getContent(new Filter<ArchivePath>() {

                @Override
                public boolean include(ArchivePath path) {
                    return path.get().endsWith(".json");
                }

            });

            for (Map.Entry<ArchivePath, Node> archivePathNodeEntry : content.entrySet()) {
                Node node = archivePathNodeEntry.getValue();

                try (InputStream is = node.getAsset().openStream()) {
                    CollectorConfiguration subconfig = mapper.readValue(is, CollectorConfiguration.class);
                    if (config == null) {
                        config = subconfig;
                    } else {
                        config.merge(subconfig, true);
                    }
                }
            }
        }

        return config;
    }

    /**
     * This method initializes the instrumentation rules at runtime.
     *
     * @param config The collector configuration
     */
    public static void initRuntime(CollectorConfiguration config) {
        RuleTransformer ruleTransformer = new RuleTransformer();
        RuleCheck ruleCheck = new RuleCheck();

        Map<String, Instrumentation> instrumentTypes = config.getInstrumentation();

        for (Map.Entry<String, Instrumentation> stringInstrumentationEntry : instrumentTypes.entrySet()) {
            Instrumentation types = stringInstrumentationEntry.getValue();
            String rules = ruleTransformer.transform(stringInstrumentationEntry.getKey(), types,
                    config.getProperty("version." + stringInstrumentationEntry.getKey(), null));

            if (rules != null) {
                ruleCheck.addRule(stringInstrumentationEntry.getKey(), rules);
            }
        }

        ruleCheck.checkRules();
    }
}
