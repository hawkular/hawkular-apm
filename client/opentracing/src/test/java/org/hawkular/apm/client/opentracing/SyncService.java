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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.propagation.TextMapInjectAdapter;

/**
 * @author gbrown
 */
public class SyncService extends AbstractService {

    public static final String SYNC_TXN_NAME_1 = "This is the sync service";
    public static final String SYNC_TXN_NAME_2 = "This is the other sync service";
    public static final String MY_FAULT = "MyFault";
    public static final String ORDER_ID_NAME = "orderId";
    public static final String ORDER_ID_VALUE = "1243343456455";

    public SyncService(Tracer tracer) {
        super(tracer);
    }

    public void handle1(Message message) {
        SpanContext spanCtx = getTracer().extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(message.getHeaders()));

        // Top level, so create Tracer and root span
        Span serverSpan = getTracer().buildSpan("Server")
                .asChildOf(spanCtx)
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL, "http://localhost:8080/inbound?orderId=123&verbose=true")
                .withTag(Constants.PROP_TRANSACTION_NAME, SYNC_TXN_NAME_1)
                .withTag(ORDER_ID_NAME, ORDER_ID_VALUE)
                .start();

        delay(500);

        component(serverSpan);

        serverSpan.setTag("fault", MY_FAULT);

        delay(500);

        serverSpan.finish();

        serverSpan.close();
    }

    public void handle2(Message message) {
        SpanContext spanCtx = getTracer().extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(message.getHeaders()));

        // Top level, so create Tracer and root span
        Span serverSpan = getTracer().buildSpan("Server")
                .asChildOf(spanCtx)
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL, "http://localhost:8080/inbound?orderId=123&verbose=true")
                .withTag(ORDER_ID_NAME, ORDER_ID_VALUE)
                .start();

        delay(500);

        component(serverSpan);

        serverSpan.setTag("fault", MY_FAULT);

        delay(500);

        serverSpan.finish();

        serverSpan.close();
    }

    public void component(Span span) {
        // Span will auto-close and automatically call finish
        try (Span componentSpan = getTracer().buildSpan("Component")
                .asChildOf(span)
                .withTag(Constants.PROP_DATABASE_STATEMENT, "INSERT order INTO Orders")
                .withTag("component", Constants.COMPONENT_DATABASE)
                .start()) {

            delay(500);

            callService(componentSpan);

            delay(500);

        }
    }

    public void callService(Span span) {
        try (Span clientSpan = getTracer().buildSpan("Client")
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL, "http://localhost:8080/outbound")
                .withTag(Constants.PROP_TRANSACTION_NAME, SYNC_TXN_NAME_2)
                .asChildOf(span).start()) {
            Message mesg = createMessage();
            getTracer().inject(clientSpan.context(), Format.Builtin.TEXT_MAP,
                    new TextMapInjectAdapter(mesg.getHeaders()));

            delay(500);

            // Explicit finish
            clientSpan.finish();
        }
    }

}
