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

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Constants;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

/**
 * @author gbrown
 */
public class ForkJoinService extends AbstractService {

    public ForkJoinService(Tracer tracer) {
        super(tracer);
    }

    public void handle(Message message) {
        SpanContext spanCtx = getTracer().extract(Format.Builtin.TEXT_MAP,
                new TextMapExtractAdapter(message.getHeaders()));

        // Top level, so create Tracer and root span
        Span serverSpan = getTracer().buildSpan("Server")
                .asChildOf(spanCtx)
                .withTag(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL, "http://localhost:8080/inbound?orderId=123&verbose=true")
                .withTag("orderId", "1243343456455")
                .start();

        delay(500);

        ForkJoinPool pool = new ForkJoinPool();
        for (int i = 0; i < 5; i++) {
            int pos = i;
            pool.execute(() -> component(serverSpan, pos));
        }

        pool.awaitQuiescence(5, TimeUnit.SECONDS);

        serverSpan.finish();

        serverSpan.close();
    }

    public void component(Span span, int num) {
        // Span will auto-close and automatically call finish
        try (Span component1Span = getTracer().buildSpan("Component" + num)
                .asChildOf(span)
                .start()) {

            delay((long) (100 + (900 * Math.random())));
        }
    }

}
