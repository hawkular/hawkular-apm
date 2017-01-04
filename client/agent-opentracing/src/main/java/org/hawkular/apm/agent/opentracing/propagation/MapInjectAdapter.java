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

import java.util.Iterator;
import java.util.Map;

import io.opentracing.propagation.TextMap;

/**
 * @author gbrown
 */
public final class MapInjectAdapter implements TextMap {
    private final Map<String, String> map;

    public MapInjectAdapter(Map<String, String> map) {
        this.map = map;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("MapInjectAdapter should only be used with Tracer.inject()");
    }

    @Override
    public void put(String key, String value) {
        this.map.put(key, value);
    }
}
