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

import org.hawkular.apm.api.model.Constants;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;

/**
 * @author gbrown
 */
public class ClientService extends AbstractService {

    public static final String ORDER_ID_NAME = "orderId";

    public static final String ORDER_ID_VALUE = "1243343456455";

    private String myTag;

    public ClientService(Tracer tracer, String myTag) {
        super(tracer);
        this.myTag = myTag;
    }

    public void handle() {
        // Top level, so create Tracer and root span
        try (Span componentSpan = getTracer().buildSpan("Component")
                .withTag(ORDER_ID_NAME, ORDER_ID_VALUE)
                .start()) {

            delay(500);

            callService(componentSpan);

            delay(500);
        }
    }

    public void callService(Span span) {
        try (Span clientSpan = getTracer().buildSpan("Client")
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL, "http://localhost:8080/outbound")
                .withTag("myTag", myTag)
                .asChildOf(span).start()) {
            Message mesg = createMessage();
            getTracer().inject(clientSpan.context(), Format.Builtin.TEXT_MAP,
                    new TextMapInjectAdapter(mesg.getHeaders()));

            delay(500);
        }
    }

}
