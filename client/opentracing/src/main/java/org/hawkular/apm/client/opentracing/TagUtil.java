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
package org.hawkular.apm.client.opentracing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * This class provides utility functions for processing tags.
 *
 * @author gbrown
 */
public class TagUtil {

    /**
     * This method determines if the supplied key relates to
     * a URI.
     *
     * @param key The key
     * @return Whether the key relates to a URI
     */
    public static boolean isUriKey(String key) {
        return key.endsWith(".url") || key.endsWith(".uri");
    }

    /**
     * This method extracts a 'type' from the key used
     * to identify the URI. This assumes that the key structure
     * is '<type>.uri' or '<type>.url'.
     *
     * @param key The key
     * @return The type
     */
    public static String getTypeFromUriKey(String key) {
        return key.substring(0, key.length() - 4);
    }

    /**
     * This method extracts the 'path' component of a
     * URL. If the supplied value is not a valid URL
     * format, then it will simply return the supplied
     * value.
     *
     * @param value The URI value
     * @return The path of a URL, or the value returned as is
     */
    public static String getUriPath(String value) {
        try {
            URL url = new URL(value);
            return url.getPath();
        } catch (MalformedURLException e) {
            return value;
        }
    }

    /**
     * This method returns the URI value from a set of
     * supplied tags, by first identifying which tag
     * relates to a URI and then returning its path value.
     *
     * @param tags The tags
     * @return The URI path, or null if not found
     */
    public static String getUriPath(Map<String, Object> tags) {
        for (Map.Entry<String,Object> entry : tags.entrySet()) {
            if (isUriKey(entry.getKey())) {
                return getUriPath(entry.getValue().toString());
            }
        }
        return null;
    }

}
