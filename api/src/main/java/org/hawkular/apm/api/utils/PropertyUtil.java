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
package org.hawkular.apm.api.utils;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

/**
 * This class provides access to properties.
 *
 * @author gbrown
 */
public class PropertyUtil {

    private static final Logger LOG = Logger.getLogger(PropertyUtil.class.getName());

    /**  */
    public static final String HAWKULAR_TENANT = "HAWKULAR_TENANT";

    /**  */
    public static final String HAWKULAR_APM_URI = "HAWKULAR_APM_URI";

    /**  */
    public static final String HAWKULAR_APM_USERNAME = "HAWKULAR_APM_USERNAME";

    /**  */
    public static final String HAWKULAR_APM_PASSWORD = "HAWKULAR_APM_PASSWORD";

    /**  */
    public static final String HAWKULAR_APM_CONFIG_REFRESH = "HAWKULAR_APM_CONFIG_REFRESH";

    /**  */
    public static final String HAWKULAR_APM_LOG_LEVEL = "HAWKULAR_APM_LOG_LEVEL";

    /**  */
    public static final String HAWKULAR_APM_LOG_JUL = "HAWKULAR_APM_LOG_JUL";

    /**
     * The maximum number of traces to batch before sending to the server.
     */
    public static final String HAWKULAR_APM_COLLECTOR_BATCHSIZE = "HAWKULAR_APM_COLLECTOR_BATCHSIZE";

    /**
     * The maximum time (in milliseconds) before sending a batch of traces to the server.
     */
    public static final String HAWKULAR_APM_COLLECTOR_BATCHTIME = "HAWKULAR_APM_COLLECTOR_BATCHTIME";

    /**
     * The thread pool size for reporting a batch of traces to the server.
     */
    public static final String HAWKULAR_APM_COLLECTOR_BATCHTHREADS = "HAWKULAR_APM_COLLECTOR_BATCHTHREADS";

    /**
     * This method returns the named property, first checking the system properties
     * and if not found, checking the environment.
     *
     * @param name The name
     * @return The property, or null if not found
     */
    public static String getProperty(String name) {
        return System.getProperty(name, System.getenv(name));
    }

    /**
     * This method returns the named property, first checking the system properties
     * and if not found, checking the environment.
     *
     * @param name The name
     * @param def The default if not found
     * @return The property, or the default value if not found
     */
    public static String getProperty(String name, String def) {
        String ret = System.getProperty(name, System.getenv(name));
        if (ret != null) {
            return ret;
        }
        return def;
    }

    /**
     * This method returns the property as an integer value.
     *
     * @return The property as an integer, or null if not found
     */
    public static Integer getPropertyAsInteger(String name) {
        String value = getProperty(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Failed to convert property value '" + value + "' to integer", e);
            }
        }
        return null;
    }

    /**
     * This method returns the property as an integer value.
     *
     * @return The property as a boolean, or false if not found
     */
    public static boolean getPropertyAsBoolean(String name) {
        String value = getProperty(name);
        if (value != null && value.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

}
