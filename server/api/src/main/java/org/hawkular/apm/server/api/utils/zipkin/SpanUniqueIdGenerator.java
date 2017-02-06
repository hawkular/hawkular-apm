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

package org.hawkular.apm.server.api.utils.zipkin;

import org.hawkular.apm.server.api.model.zipkin.Span;

/**
 * Spans generally have an unique id, except when there are two spans representing the client and server
 * side of a communication, in which case they share the same span id. Therefore, it is necessary to
 * provide an utility class for changing the id to avoid overriding entities in a database, cache...
 * To the id of the client spans is added a string suffix.
 *
 * @author Pavol Loffay
 */
public class SpanUniqueIdGenerator {

    protected static final String CLIENT_ID_SUFFIX = "-client";

    /**
     * Utility method to get unique id of the span. Note that method
     * does not change the span id.
     *
     * @param span Span.
     * @return Unique id of the span.
     */
    public static String toUnique(Span span) {

        String id = span.getId();
        if (span.clientSpan()) {
            id = getClientId(span.getId());
        }

        return id;
    }

    /**
     * Reverse method to the {@link SpanUniqueIdGenerator#toUnique(Span)} to generate original
     * id from the unique id.
     *
     * @param span Span with unique id.
     * @return Original id of the span.
     */
    public static String toOriginal(Span span) {

        String id = span.getId();

        if (span.clientSpan()) {
            int suffixIndex = id.lastIndexOf(CLIENT_ID_SUFFIX);
            if (suffixIndex > 0) {
                id = id.substring(0, suffixIndex);
            }
        }

        return id;
    }

    public static String getClientId(String id) {
        if (id.endsWith(CLIENT_ID_SUFFIX)) {
            throw new IllegalStateException("Id already contains suffix: " + CLIENT_ID_SUFFIX);
        }

        return id + CLIENT_ID_SUFFIX;
    }
}
