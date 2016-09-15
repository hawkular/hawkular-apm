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

package org.hawkular.apm.example.dropwizard.rest;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Request;

/**
 * @author Pavol Loffay
 */
public class URLUtils {

    private static final String HOST = "http://127.0.0.1:";

    private URLUtils() {}

    /**
     * Constructs URL for in service requests (e.g. calls for endpoint of this application)
     */
    public static String getInServiceURL(HttpServletRequest request, String path) {
        String contextPath = ((Request) request).getContext().getContextPath();
        int port = request.getServerPort();

        return getInServiceURL(port, contextPath, path);
    }

    public static String getInServiceURL(int port, String contextPath, String path) {
        return HOST + port + contextPath + path;
    }
}
