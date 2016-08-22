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
package org.hawkular.apm.client.opentracing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.hawkular.apm.api.model.Property;

import io.opentracing.APMSpan;
import io.opentracing.APMTracer;

/**
 * This node processor implementation provides the default mapping behaviour from
 * the span's tags.
 *
 * @author gbrown
 */
public class DefaultNodeProcessor implements NodeProcessor {

    @Override
    public void process(TraceContext context, APMSpan span, NodeBuilder nodeBuilder) {
        for (Map.Entry<String, Object> entry : span.getTags().entrySet()) {
            nodeBuilder.addProperty(new Property(entry.getKey(), entry.getValue().toString()));

            if (entry.getKey().endsWith(".url") || entry.getKey().endsWith(".uri")) {
                try {
                    URL url = new URL(entry.getValue().toString());
                    nodeBuilder.setUri(url.getPath());
                } catch (MalformedURLException e) {
                    nodeBuilder.setUri(entry.getValue().toString());
                }
                nodeBuilder.setEndpointType(entry.getKey().substring(0, entry.getKey().length() - 4));
            } else if (entry.getKey().equals("component")) {
                nodeBuilder.setComponentType(entry.getValue().toString());
            } else if (entry.getKey().contains("fault")) {
                nodeBuilder.setFault(entry.getValue().toString());
            } else if (entry.getKey().contains(APMTracer.TRANSACTION_NAME)) {
                // Check if business transaction name already defined - if not then set on the trace context
                if (context.getBusinessTransaction() == null) {
                    context.setBusinessTransaction(entry.getValue().toString());
                }
            }
        }
    }

}
