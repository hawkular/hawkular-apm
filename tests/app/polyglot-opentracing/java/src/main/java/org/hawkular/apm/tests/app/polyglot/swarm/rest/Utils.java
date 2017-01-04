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

package org.hawkular.apm.tests.app.polyglot.swarm.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

/**
 * @author Pavol Loffay
 */
public class Utils {

    private Utils() {}

    public static Map<String, String> extractHeaders(HttpHeaders httpHeaders) {
        Map<String, String> headers = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : httpHeaders.getRequestHeaders().entrySet()) {
            if (entry.getValue() != null && entry.getValue().size() > 0) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        return headers;
    }

}
