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
package org.hawkular.apm.api.internal.actions.helpers;

import org.hawkular.apm.api.logging.Logger;

/**
 * This class represents helper functions for processing Text.
 *
 * @author gbrown
 */
public class Text {

    private static final Logger log = Logger.getLogger(Text.class.getName());

    /**
     * This method converts the supplied object to a string.
     *
     * @param value The value
     * @return The string, or null if an error occurred
     */
    public static String serialize(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof byte[]) {
            return new String((byte[]) value);
        } else {
            log.severe("Unable to convert value '" + value + "' to string");
        }
        return null;
    }

}
