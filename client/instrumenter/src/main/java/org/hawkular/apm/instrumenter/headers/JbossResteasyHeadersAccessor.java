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

import java.lang.reflect.Method;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

/**
 * The headers accessor implementation for JBoss RESTEasy.
 *
 * @author gbrown
 */
public class JbossResteasyHeadersAccessor implements HeadersAccessor {

    private static final Logger log = Logger.getLogger(JbossResteasyHeadersAccessor.class.getName());

    private static final String TARGET_TYPE = "org.jboss.resteasy.client.jaxrs.internal.ClientInvocation";

    @Override
    public String getTargetType() {
        return TARGET_TYPE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getHeaders(Object target) {
        try {
            Class<?> cls = Thread.currentThread().getContextClassLoader().
                    loadClass(TARGET_TYPE);
            Class<?> headerscls = Thread.currentThread().getContextClassLoader().
                    loadClass("org.jboss.resteasy.client.jaxrs.internal.ClientRequestHeaders");
            Method getHeadersMethod = cls.getMethod("getHeaders");
            Method asMapMethod = headerscls.getMethod("asMap");

            Object headers = getHeadersMethod.invoke(target);

            return (Map<String, String>) asMapMethod.invoke(headers);
        } catch (Throwable t) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Failed to obtain headers", t);
            }
        }

        return null;
    }
}
