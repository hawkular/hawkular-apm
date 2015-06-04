/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.client.manager.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.admin.InstrumentType;
import org.hawkular.btm.api.model.admin.Instrumentation;
import org.hawkular.btm.api.util.ServiceResolver;

/**
 * @author gbrown
 */
public class Transformer {

    private static Map<Class<? extends InstrumentType>, InstrumentTypeTransformer> transformers =
            new HashMap<Class<? extends InstrumentType>, InstrumentTypeTransformer>();

    static {
        List<InstrumentTypeTransformer> trms = ServiceResolver.getServices(InstrumentTypeTransformer.class);
        trms.forEach(t -> transformers.put(t.getInstrumentType(), t));
    }

    /**
     * This method transforms the list of instrument types into a
     * ByteMan rule script.
     *
     * @param types The instrument types
     * @return The rule script
     */
    public String transform(Instrumentation types) {
        StringBuilder builder = new StringBuilder();

        for (InstrumentType type : types.getTypes()) {
            InstrumentTypeTransformer transformer = transformers.get(type.getClass());

            if (transformer != null) {
                if (builder.length() > 0) {
                    builder.append("\r\n");
                }
                builder.append(transformer.convertToRule(type));
            } else {
                System.err.println("Transformer for type '" + type.getClass() + "' not found");
            }
        }

        return builder.toString();
    }
}
