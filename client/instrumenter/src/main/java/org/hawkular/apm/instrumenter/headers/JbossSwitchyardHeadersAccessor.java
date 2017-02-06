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
import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

/**
 * The headers accessor implementation for JBoss Switchyard.
 *
 * @author gbrown
 */
public class JbossSwitchyardHeadersAccessor implements HeadersAccessor {

    private static final Logger log = Logger.getLogger(JbossSwitchyardHeadersAccessor.class.getName());

    private static final String TARGET_TYPE = "org.switchyard.Context";

    private static final String PROPERTY_CLASS = "org.switchyard.Property";

    @Override
    public String getTargetType() {
        return TARGET_TYPE;
    }

    @Override
    public Map<String, String> getHeaders(Object target) {
        try {
            Class<?> cls = Thread.currentThread().getContextClassLoader().
                    loadClass(TARGET_TYPE);
            Class<?> propertyClass = Thread.currentThread().getContextClassLoader().
                    loadClass(PROPERTY_CLASS);
            Method getPropertiesMethod = cls.getMethod("getProperties");
            Method nameMethod = propertyClass.getMethod("getName");
            Method valueMethod = propertyClass.getMethod("getValue");

            java.util.Set<?> properties = (java.util.Set<?>)getPropertiesMethod.invoke(target);

            Map<String,String> headers=new HashMap<String,String>();

            for (Object prop : properties) {
                String name=(String)nameMethod.invoke(prop);
                Object value=valueMethod.invoke(prop);
                if (value instanceof String) {
                    headers.put(name, (String)value);
                }
            }

            return headers;
        } catch (Throwable t) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Failed to obtain headers", t);
            }
        }

        return null;
    }
}
