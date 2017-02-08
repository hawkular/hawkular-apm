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
package org.hawkular.apm.client.opentracing.refactor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hawkular.apm.api.model.Constants;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
public class HawkularSpan implements Span {

    // not defined in OT api, however used in ot-impl artifact
    private static final String DEFAULT_LOG_EVENT_NAME = "event";
    private static final String DEFAULT_LOG_PAYLOAD_NAME = "payload";

    private String operationName;
    private final Map<String, Object> tags;
    private final HawkularSpanContext spanContext;

    public List<LogData> logs = Collections.synchronizedList(new ArrayList<>());

    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final long startMicros;
    private long finishMicros;

    HawkularSpan(HawkularSpanContext spanContext, String operationName, Map<String, Object> tags, long startMicros) {
        this.spanContext = spanContext;
        this.tags = Collections.synchronizedMap(tags);
        this.startMicros = startMicros;
        this.operationName = operationName;
    }

    @Override
    public SpanContext context() {
        return spanContext;
    }

    @Override
    public void finish() {
        finish(HawkularTracer.nowMicros());
    }

    @Override
    public void finish(long finishMicros) {
        if (!finished.getAndSet(true)) {
            this.finishMicros = finishMicros;
            spanContext.traceFragmentState().finish();
        }
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public Span setTag(String key, String value) {
        return setObjectTag(key, value);
    }

    @Override
    public Span setTag(String key, boolean value) {
        return setObjectTag(key, value);
    }

    @Override
    public Span setTag(String key, Number value) {
        return setObjectTag(key, value);
    }

    private Span setObjectTag(String key, Object object) {
        tags.put(key, object);

        if (Constants.PROP_TRANSACTION_NAME.equals(key) && object != null) {
            if (spanContext.traceFragmentState().getTransaction() == null) {
                this.spanContext.traceFragmentState().setNamedTransaction(object.toString());
            }
        }

        return this;
    }

    @Override
    public Span log(String event) {
        return log(HawkularTracer.nowMicros(), event);
    }

    @Override
    public Span log(long timestampMicroseconds, String event) {
        return log(timestampMicroseconds, Collections.singletonMap(DEFAULT_LOG_EVENT_NAME, event));
    }

    @Override
    public Span log(Map<String, ?> fields) {
        return log(HawkularTracer.nowMicros(), fields);
    }

    @Override
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        logs.add(new LogData(timestampMicroseconds, fields));
        return this;
    }

    @Override
    public Span log(String eventName, Object payload) {
        return log(HawkularTracer.nowMicros(), eventName, payload);
    }

    @Override
    public Span log(long timestampMicroseconds, String eventName, Object payload) {
        Map<String, Object> fields = new HashMap<>(2);
        fields.put(DEFAULT_LOG_EVENT_NAME, eventName);
        if (payload != null) {
            fields.put(DEFAULT_LOG_PAYLOAD_NAME, payload);
        }
        return log(timestampMicroseconds, fields);
    }

    @Override
    public Span setBaggageItem(String key, String value) {
        spanContext.setBaggageItem(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return spanContext.getBaggageItem(key);
    }

    @Override
    public Span setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    long startMicros() {
        return startMicros;
    }

    long finishMicros() {
        return finishMicros;
    }

    /**
     * Returns string url from span's tags.
     *
     * @return url or null if not found or not valid url.
     */
    String uri() {
        Object httpUrl = tags.get(Tags.HTTP_URL.getKey());
        if (httpUrl != null) {
            try {
                URL url = new URL(httpUrl.toString());
                return url.getPath();
            } catch (MalformedURLException e) {
            }
        }

        return null;
    }

    String operationName() {
        return operationName;
    }

    Map<String, Object> tags() {
        return tags;
    }
}
