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
package org.hawkular.apm.instrumenter.headers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

/**
 * The headers accessor implementation for Iterable<Map.Entry>.
 *
 * @author gbrown
 */
public class IterableMapEntryHeadersAccessor implements HeadersAccessor {

    private static final Logger log = Logger.getLogger(IterableMapEntryHeadersAccessor.class.getName());

    private static final String TARGET_TYPE = "java.lang.Iterable<Map.Entry>";

    @Override
    public String getTargetType() {
        return TARGET_TYPE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getHeaders(Object target) {
        try {
            Iterable<Map.Entry<String, ?>> iterable = (Iterable<Map.Entry<String, ?>>) target;
            Iterator<Map.Entry<String, ?>> iter = iterable.iterator();

            // Copy header values for now, but may be more efficient to create proxy onto request
            Map<String, String> ret = new HashMap<String, String>();

            while (iter.hasNext()) {
                Map.Entry<String, ?> entry = iter.next();
                ret.put(entry.getKey(), entry.getValue().toString());
            }

            return ret;
        } catch (Throwable t) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Failed to obtain headers", t);
            }
        }

        return null;
    }
}
