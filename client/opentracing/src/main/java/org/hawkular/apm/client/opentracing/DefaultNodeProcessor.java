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
package org.hawkular.apm.client.opentracing;

import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;

import io.opentracing.impl.APMSpan;

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
            if (entry.getKey() != null && entry.getValue() != null) {

                if (TagUtil.isUriKey(entry.getKey())) {
                    nodeBuilder.setUri(TagUtil.getUriPath(entry.getValue().toString()));
                    String type = TagUtil.getTypeFromUriKey(entry.getKey());
                    nodeBuilder.setEndpointType(type);
                    nodeBuilder.setComponentType(type);
                } else if (entry.getKey().equals("component")) {
                    nodeBuilder.setComponentType(entry.getValue().toString());
                } else if (entry.getKey().contains(Constants.PROP_TRANSACTION_NAME)) {
                    // Check if transaction name already defined - if not then set on the trace context
                    if (context.getTransaction() == null) {
                        context.setTransaction(entry.getValue().toString());
                    }
                } else {
                    nodeBuilder.addProperty(new Property(entry.getKey(), entry.getValue()));
                }
            }
        }
    }

}
