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
package org.hawkular.btm.api.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        try {
            Path path=Paths.get(uri);

            File file=path.toFile();

            if (file.isDirectory()) {
                config = loadConfig(file, path);
            } else {
                String json = new String(Files.readAllBytes(path));

                config = mapper.readValue(json, CollectorConfiguration.class);
            }
        } catch (IOException e) {
            System.err.println("Failed to load BTM configuration: " + e);
            e.printStackTrace();
        }

        return config;
    }

    /**
     * This method loads the configuration from the supplied directory.
     *
     * @param dir The directory
     * @param parent The parent path
     * @return The configuration
     */
    protected CollectorConfiguration loadConfig(File dir, Path parent) {
        CollectorConfiguration config = null;

        for (File f : dir.listFiles()) {
            Path child=parent.resolve(f.getName());
            CollectorConfiguration childConfig=null;

            if (f.isDirectory()) {
                childConfig = loadConfig(f, child);
            } else {
                try {
                    String json = new String(Files.readAllBytes(child));

                    childConfig = mapper.readValue(json, CollectorConfiguration.class);
                } catch (IOException e) {
                    System.err.println("Failed to load BTM configuration: " + e);
                    e.printStackTrace();
                }
            }

            if (childConfig != null) {
                if (config == null) {
                    config = childConfig;
                } else {
                    config.merge(childConfig, false);
                }
            }
        }

        return config;
    }
}
