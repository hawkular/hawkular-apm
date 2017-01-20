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
package org.hawkular.apm.agent.opentracing.propagation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

import io.opentracing.propagation.TextMap;

/**
 * @author gbrown
 */
public final class HttpServletRequestExtractAdapter implements TextMap {

    private static final Logger log = Logger.getLogger(HttpServletRequestExtractAdapter.class.getName());

    private final Object request;

    private static Method reqGetHeaderNames;
    private static Method reqGetHeader;

    public HttpServletRequestExtractAdapter(final Object request) {
        this.request = request;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        Map<String,String> map = new HashMap<>();
        try {
            if (reqGetHeaderNames == null) {
                reqGetHeaderNames = request.getClass().getMethod("getHeaderNames");
            }
            if (reqGetHeader == null) {
                reqGetHeader = request.getClass().getMethod("getHeader", String.class);
            }
            Enumeration<String> names = (Enumeration<String>)reqGetHeaderNames.invoke(request);
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = (String)reqGetHeader.invoke(request, name);
                map.put(name, value);
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException t) {
            log.log(Level.WARNING, "Failed to get headers", t);
        }
        return map.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("HttpServletRequestExtractAdapter should only be used with Tracer.extract()");
    }
}
