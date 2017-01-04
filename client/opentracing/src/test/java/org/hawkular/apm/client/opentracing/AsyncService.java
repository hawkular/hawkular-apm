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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class AsyncService extends AbstractService {

    public AsyncService(Tracer tracer) {
        super(tracer);
    }

    public void handle(Message message, Handler handler) {
        SpanContext spanCtx = getTracer().extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(message.getHeaders()));

        // Top level, so create Tracer and root span
        Span serverSpan = getTracer().buildSpan("Server")
                .asChildOf(spanCtx)
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL,
                        "http://localhost:8080/inbound?orderId=123&verbose=true")
                .withTag("orderId", "1243343456455")
                .start();

        delay(500);

        callService(serverSpan, obj -> {

            serverSpan.finish();

            handler.handle(obj);
        });
    }

    public void callService(Span span, Handler handler) {
        Span clientSpan = getTracer().buildSpan("Client")
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL, "http://localhost:8080/outbound")
                .withTag(Constants.PROP_TRANSACTION_NAME, "AnotherTxnName")     // Should not overwrite the existing name
                .asChildOf(span).start();
        Message mesg = createMessage();
        getTracer().inject(clientSpan.context(), Format.Builtin.TEXT_MAP,
                new TextMapInjectAdapter(mesg.getHeaders()));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            delay(500);

            // Explicit finish
            clientSpan.finish();

            handler.handle("My Response");
        });
    }

    public interface Handler {

        void handle(Object obj);

    }
}
