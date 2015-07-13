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
package org.hawkular.btm.api.services;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the abstract base implementation of the ConfigurationManager
 * API, that obtains the configuration from a file (or directory of files) referenced
 * by a system property.
 *
 * @author gbrown
 */
public abstract class AbstractConfigurationManager implements ConfigurationManager {

    /**  */
    private static final String HAWKULAR_BTM_CONFIG = "hawkular-btm.config";

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = Logger.getLogger(AbstractConfigurationManager.class.getName());

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.ConfigurationManager#getConfiguration()
     */
    @Override
    public final CollectorConfiguration getConfiguration() {

        // Check if instrumentation rules are provided on the command line
        if (System.getProperties().containsKey(HAWKULAR_BTM_CONFIG)) {
            return loadConfig(System.getProperty(HAWKULAR_BTM_CONFIG));
        }

        return doGetConfiguration();
    }

    /**
     * This method obtains the collector configuration from a derived
     * implementation.
     *
     * @return The collector configuration
     */
    protected abstract CollectorConfiguration doGetConfiguration();

    /**
     * This method loads the configuration from the supplied URI.
     *
     * @param uri The URI
     * @return The configuration
     */
    protected CollectorConfiguration loadConfig(String uri) {
        CollectorConfiguration config = null;

        File f = new File(uri);

        if (!f.isAbsolute()) {
            try {
                URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
                uri = url.getPath();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to get absolute path for uri '" + uri + "'", e);
                uri = null;
            }
        }

        if (uri != null) {
            int index = uri.indexOf(".jar!");
            if (index != -1) {
                int startInd = 0;
                if (uri.startsWith("file:")) {
                    startInd = 5;
                }
                config = loadConfigFromJar(uri.substring(startInd, index + 4));
            } else {
                config = loadConfig(new File(uri));
            }
        }

        return config;
    }

    /**
     * This method loads the configuration from a Jar file.
     *
     * @param file The Jar file
     * @return The configuration
     */
    protected CollectorConfiguration loadConfigFromJar(String uri) {
        CollectorConfiguration config = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("loadConfigFromJar uri=" + uri);
        }

        try {
            JarFile jarFile = new JarFile(uri);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".json")) {
                    java.io.InputStream is = jarFile.getInputStream(entry);

                    byte[] b = new byte[is.available()];
                    is.read(b);

                    is.close();

                    String json = new String(b);

                    CollectorConfiguration childConfig = mapper.readValue(json, CollectorConfiguration.class);
                    if (childConfig != null) {
                        if (config == null) {
                            config = childConfig;
                        } else {
                            config.merge(childConfig, false);
                        }
                    }
                }
            }

            jarFile.close();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to load BTM configuration from Jar '" + uri + "'", e);
        }

        return config;
    }

    /**
     * This method loads the configuration from the supplied file/directory.
     *
     * @param file The file/directory
     * @return The configuration
     */
    protected CollectorConfiguration loadConfig(File file) {
        CollectorConfiguration config = null;

        if (log.isLoggable(Level.FINEST)) {
            log.finest("loadConfig file=" + file + " file?=" + file.isFile() + " dir?=" + file.isDirectory());
        }

        if (file.isFile()) {
            try {
                java.io.FileInputStream fis = new java.io.FileInputStream(file);

                byte[] b = new byte[fis.available()];
                fis.read(b);

                fis.close();

                String json = new String(b);

                config = mapper.readValue(json, CollectorConfiguration.class);
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to load BTM configuration", e);
            }

        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                CollectorConfiguration childConfig = loadConfig(f);

                if (childConfig != null) {
                    if (config == null) {
                        config = childConfig;
                    } else {
                        config.merge(childConfig, false);
                    }
                }
            }
        }

        return config;
    }
}
