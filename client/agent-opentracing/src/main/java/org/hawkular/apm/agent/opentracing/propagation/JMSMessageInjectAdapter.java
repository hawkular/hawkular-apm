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
import java.util.Iterator;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

import io.opentracing.propagation.TextMap;

/**
 * @author gbrown
 */
public final class JMSMessageInjectAdapter implements TextMap {

    private static final Logger log = Logger.getLogger(JMSMessageInjectAdapter.class.getName());

    private final Object message;

    private static Method setStringProperty;

    public JMSMessageInjectAdapter(Object message) {
        this.message = message;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("JMSMessageInjectAdapter should only be used with Tracer.inject()");
    }

    @Override
    public void put(String key, String value) {
        try {
            if (setStringProperty == null) {
                setStringProperty = message.getClass().getMethod("setStringProperty", String.class, String.class);
            }
            setStringProperty.invoke(message, key, value);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException t) {
            log.log(Level.WARNING, "Failed to set header '" + key + "'", t);
        }
    }
}
