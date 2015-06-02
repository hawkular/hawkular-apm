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
package org.hawkular.btm.btxn.client.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hawkular.btm.api.client.ConfigurationManager;
import org.hawkular.btm.api.model.admin.CollectorConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the 'standalone' implementation of the ConfigurationManager
 * API, that obtains the configuration from a file referenced on the command line.
 *
 * @author gbrown
 */
public class StandaloneConfigurationManagerImpl implements ConfigurationManager {

    /**  */
    private static final String HAWKULAR_BTM_CONFIG = "hawkular-btm.config";

    private static ObjectMapper mapper = new ObjectMapper();

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.ConfigurationManager#getConfiguration()
     */
    @Override
    public CollectorConfiguration getConfiguration() {

        // Check if instrumentation rules are provided on the command line
        if (System.getProperties().containsKey(HAWKULAR_BTM_CONFIG)) {
            return loadConfig(System.getProperty(HAWKULAR_BTM_CONFIG));
        }

        return null;
    }

    /**
     * This method loads the configuration from the supplied URI.
     *
     * @param uri The URI
     * @return The configuration
     */
    protected CollectorConfiguration loadConfig(String uri) {
        CollectorConfiguration config = null;

        try {
            String json = new String(Files.readAllBytes(Paths.get(uri)));

            config = mapper.readValue(json, CollectorConfiguration.class);

        } catch (IOException e) {
            System.err.println("Failed to load BTM configuration: " + e);
            e.printStackTrace();
        }

        return config;
    }

}
