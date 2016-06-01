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
package org.hawkular.btm.api.utils;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;

/**
 * This class provides access to properties.
 *
 * @author gbrown
 */
public class PropertyUtil {

    private static final Logger LOG = Logger.getLogger(PropertyUtil.class.getName());

    /**  */
    public static final String HAWKULAR_APM_URI = "HAWKULAR_APM_URI";

    /**  */
    public static final String HAWKULAR_APM_USERNAME = "HAWKULAR_APM_USERNAME";

    /**  */
    public static final String HAWKULAR_APM_PASSWORD = "HAWKULAR_APM_PASSWORD";

    /**  */
    public static final String HAWKULAR_APM_CONFIG_REFRESH = "HAWKULAR_APM_CONFIG_REFRESH";

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
            } catch (Exception e) {
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
