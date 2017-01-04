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
import org.hawkular.apm.api.logging.Logger.Level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

/**
 * @author gbrown
 */
public class JSON {

    private static final Logger log = Logger.getLogger(JSON.class.getName());

    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * This method serializes the supplied object to a JSON document.
     *
     * @param data The object
     * @return The JSON representation
     */
    public static String serialize(Object data) {
        String ret=null;

        if (data != null) {
            if (data.getClass() == String.class) {
                ret = (String)data;
            } else if (data instanceof byte[]) {
                ret = new String((byte[])data);
            } else {
                try {
                    ret = mapper.writeValueAsString(data);
                } catch (JsonProcessingException e) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.log(Level.FINEST, "Failed to serialize object into json", e);
                    }
                }
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Serialized '"+data+"' to json: "+ret);
        }

        return ret;
    }

    /**
     * This method evaluates the predicate based on the jsonpath
     * expression on the supplied node.
     *
     * @param jsonpath The jsonpath expression
     * @param data The json data
     * @return The result, or false if the data was invalid
     */
    public static boolean predicate(String jsonpath, Object data) {
        // No jsonpath means return false
        if (jsonpath == null || jsonpath.trim().isEmpty()) {
            return false;
        }

        String json = serialize(data);
        if (json != null) {
            Object result = JsonPath.parse(json).read(jsonpath);
            if (result != null) {
                if (result.getClass() == Boolean.class) {
                    return (Boolean)result;
                } else if (result.getClass() == String.class) {
                    return Boolean.valueOf((String)result);
                } else if (result.getClass() == JSONArray.class) {
                    return !((JSONArray) result).isEmpty();
                }
            }
        }
        return false;
    }

    /**
     * This method evaluates the jsonpath expression on the supplied
     * node.
     *
     * @param jsonpath The jsonpath expression
     * @param data The json data
     * @return The result, or null if not found (which may be due to an expression error)
     */
    public static String evaluate(String jsonpath, Object data) {
        String json = serialize(data);

        // No jsonpath means return serialized form
        if (jsonpath == null || jsonpath.trim().isEmpty()) {
            return json;
        }

        if (json != null) {
            Object result = JsonPath.parse(json).read(jsonpath);
            if (result != null) {
                if (result.getClass() == JSONArray.class) {
                    JSONArray arr=(JSONArray)result;
                    if (arr.isEmpty()) {
                        result = null;
                    } else if (arr.size() == 1) {
                        result = arr.get(0);
                    }
                }
                return serialize(result);
            }
        }
        return null;
    }

}
