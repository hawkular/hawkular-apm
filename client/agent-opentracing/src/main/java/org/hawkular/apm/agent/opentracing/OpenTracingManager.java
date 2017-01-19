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
package org.hawkular.apm.agent.opentracing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * This class provides helper capabilities to the byteman rules.
 *
 * @author gbrown
 */
public class OpenTracingManager extends Helper {

    private static final Logger log = Logger.getLogger(OpenTracingManager.class.getName());

    private Tracer tracer = OpenTracingTracer.getSingleton();

    private static final ThreadLocal<TraceState> traceState = new ThreadLocal<>();

    private static final Map<String, TraceState> suspendedState = new HashMap<>();
    private static final ReentrantLock suspendedStateLock = new ReentrantLock();

    private static long expiryInterval = 60000;

    static {
        String time = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_AGENT_STATE_EXPIRY_INTERVAL);
        if (time != null) {
            expiryInterval = Long.parseLong(time);
        }

        // Create scheduled task
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        }).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                cleanup();
            }
        }, expiryInterval, expiryInterval, TimeUnit.MILLISECONDS);
    }

    public OpenTracingManager(Rule rule) {
        super(rule);
    }

    public Format<TextMap> textMapFormat() {
        return io.opentracing.propagation.Format.Builtin.TEXT_MAP;
    }

    /**
     * This method returns the OpenTracing tracer.
     *
     * @return The tracer
     */
    public Tracer getTracer() {
        return tracer;
    }

    /**
     * This method starts the span associated with the supplied
     * span builder, and adds the supplied parent span as a
     * 'child of' relationship.
     *
     * @param spanBuilder The span builder
     * @param parent The parent span
     */
    public void startSpanWithParent(SpanBuilder spanBuilder, Span parent) {
        startSpanWithParent(spanBuilder, parent, null);
    }

    /**
     * This is a convenience method for situations where we don't know
     * if a parent span is available. If we try to add a childOf relationship
     * to a null parent, it would cause a null pointer exception.
     *
     * The optional id is associated with the started span.
     *
     * @param spanBuilder The span builder
     * @param parent The parent span
     * @param id The optional id to associate with the span
     */
    public void startSpanWithParent(SpanBuilder spanBuilder, Span parent, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Start span as child of span = " + parent);
        }

        if (parent != null) {
            spanBuilder.asChildOf(parent.context());
        }

        doStartSpan(spanBuilder, id);
    }

    /**
     * This method starts the span associated with the supplied
     * span builder, and adds the supplied span context as a
     * 'child of' relationship.
     *
     * @param spanBuilder The span builder
     * @param context The span context
     */
    public void startSpanWithContext(SpanBuilder spanBuilder, SpanContext context) {
        startSpanWithContext(spanBuilder, context, null);
    }

    /**
     * This is a convenience method for situations where we don't know
     * if a parent span is available. If we try to add a childOf relationship
     * to a null context, it would cause a null pointer exception.
     *
     * The optional id is associated with the started span.
     *
     * @param spanBuilder The span builder
     * @param context The span context
     * @param id The optional id to associate with the span
     */
    public void startSpanWithContext(SpanBuilder spanBuilder, SpanContext context, String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Start span as child of context = " + context);
        }

        if (context != null) {
            spanBuilder.asChildOf(context);
        }

        doStartSpan(spanBuilder, id);
    }

    /**
     * This is a convenience method for situations where we don't know
     * if a parent span is available. If we try to add a childOf relationship
     * to a null parent, it would cause a null pointer exception.
     *
     * @param spanBuilder The span builder
     * @param context The span context
     */
    public void startSpan(SpanBuilder spanBuilder) {
        doStartSpanWithParent(spanBuilder, null);
    }

    /**
     * This is a convenience method for situations where we don't know
     * if a parent span is available. If we try to add a childOf relationship
     * to a null parent, it would cause a null pointer exception.
     *
     * The optional id is associated with the started span.
     *
     * @param spanBuilder The span builder
     * @param context The span context
     * @param id The optional id to associate with the span
     */
    public void startSpan(SpanBuilder spanBuilder, String id) {
        doStartSpanWithParent(spanBuilder, id);
    }

    protected void doStartSpanWithParent(SpanBuilder spanBuilder, String id) {
        Span parentSpan = getSpan();

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Start span as child of = " + parentSpan);
        }

        if (parentSpan != null) {
            spanBuilder.asChildOf(parentSpan);
        }

        doStartSpan(spanBuilder, id);
    }

    protected void doStartSpan(SpanBuilder spanBuilder, String id) {
        TraceState ts = traceState.get();

        if (ts == null) {
            ts = new TraceState();
            traceState.set(ts);

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Created trace state = " + ts);
            }
        }

        Span span = spanBuilder.start();

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Started span = " + span + " id = " + id + " trace state = " + ts);
        }

        ts.pushSpan(span, id);
    }

    /**
     * This method calls the 'finish()' method on the current span.
     */
    public void finishSpan() {
        TraceState ts = traceState.get();

        if (ts != null) {
            Span span = ts.popSpan();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Finish span = " + span + " trace state = " + ts);
            }

            span.finish();

            if (ts.isFinished()) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Remove trace state = " + ts);
                }
                traceState.remove();
            }
        } else if (log.isLoggable(Level.FINEST)) {
            log.finest("Finish span requested but no trace state");
        }
    }

    /**
     * This method determines whether there is a current span available.
     *
     * @return Whether a current span exists
     */
    public boolean hasSpan() {
        return getSpan() != null;
    }

    /**
     * This method determines whether there is a current span available
     * associated with the supplied id.
     *
     * @return Whether a current span exists with the id
     */
    public boolean hasSpanWithId(String id) {
        TraceState ts = traceState.get();

        if (ts != null) {
            String currentId = ts.peekId();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Has span with id = " + id + "? " + currentId.equals(id));
            }
            return currentId.equals(id);
        }

        return false;
    }

    /**
     * This method retrieves the current span, if available.
     *
     * @return The current span, or null if not defined
     */
    public Span getSpan() {
        TraceState ts = traceState.get();

        if (ts != null) {
            Span span = ts.peekSpan();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Get span = " + span + " trace state = " + ts);
            }
            return span;
        }

        return null;
    }

    /**
     * This method suspends any current trace state, associated with
     * this thread, and associates it with the supplied id. This
     * state can then be re-activated using the resume method.
     *
     * @param id The id to associated with the suspended trace state
     */
    public void suspend(String id) {
        TraceState ts = traceState.get();

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Suspend trace state = " + ts + " id = " + id);
        }

        if (ts != null) {
            setExpire(ts);

            try {
                suspendedStateLock.lock();

                // Check if id already used
                if (suspendedState.containsKey(id) && log.isLoggable(Level.FINEST)) {
                    log.finest("WARNING: Overwriting previous suspended trace state = " + suspendedState.get(id)
                            + " id = " + id);
                }

                suspendedState.put(id, ts);

                traceState.remove();
            } finally {
                suspendedStateLock.unlock();
            }
        }
    }

    /**
     * This method attempts to resume a previously suspended trace state
     * associated with the supplied id. When resumed, the trace state is
     * associated with the current thread.
     *
     * @param id The id of the trace state to resume
     */
    public void resume(String id) {
        try {
            suspendedStateLock.lock();

            TraceState ts = suspendedState.get(id);

            if (ts != null) {
                clearExpire(ts);

                // Log after finding trace state, otherwise may generate alot of logging
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Resume trace state = " + ts + " id = " + id);
                }

                // Check if thread already used
                if (traceState.get() != null && log.isLoggable(Level.FINEST)) {
                    log.finest("WARNING: Overwriting previous trace state = " + traceState.get());
                }

                traceState.set(ts);

                suspendedState.remove(id);
            }
        } finally {
            suspendedStateLock.unlock();
        }
    }

    private static void setExpire(TraceState ts) {
        // If too inefficient getting timestamp each time, could have a set
        // of used trace states, and when schedule does its cycle, it could
        // assign a single timestamp to all the trace states that have been
        // accessed during that cycle
        ts.setExpire(System.currentTimeMillis() + expiryInterval);
    }

    private static void clearExpire(TraceState ts) {
        ts.setExpire(0);
    }

    private static void cleanup() {
        try {
            suspendedStateLock.lock();

            long timestamp = System.currentTimeMillis();
            Iterator<Map.Entry<String, TraceState>> iter = suspendedState.entrySet().iterator();

            while (iter.hasNext()) {
                Map.Entry<String, TraceState> entry = iter.next();
                TraceState ts = entry.getValue();
                if (ts.getExpire() > 0 && ts.getExpire() < timestamp) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Expired trace state = " + ts + " id = " + entry.getKey());
                    }
                    iter.remove();
                }
            }
        } finally {
            suspendedStateLock.unlock();
        }
    }

    /**
     * This method resets the current thread, and suspended trace states, for testing
     * purposes.
     */
    public static void reset() {
        traceState.remove();
        suspendedState.clear();
    }

    /**
     * This method determines if the supplied path should be monitored.
     *
     * @param path The path
     * @return Whether the path is valid for monitoring
     */
    public boolean includePath(String path) {
        // Determine if the path is NOT hawkular-apm related and
        // the final part of the path is NOT a filename (with extension)
        if (!path.startsWith("/hawkular/apm") && (path.lastIndexOf('.') <= path.lastIndexOf('/'))) {
            return true;
        }
        if (log.isLoggable(Level.FINER)) {
            log.finer("Path " + path + " skipped");
        }
        return false;
    }

    /**
     * This method retrieves a variable associated with the
     * current trace.
     *
     * @return The variable value, or null if not found
     */
    public String getVariableAsString(String name) {
        TraceState ts = traceState.get();

        if (ts != null) {
            Object variable = ts.getVariables().get(name);
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Get variable '" + name + "' = " + variable);
            }
            return variable == null ? null : variable.toString();
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get variable '" + name + "' requested, but no trace state");
        }
        return null;
    }

    /**
     * This method retrieves a variable associated with the
     * current trace.
     *
     * @return The variable value, or null if not found
     */
    public void setVariable(String name, Object value) {
        TraceState ts = traceState.get();

        if (ts != null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Set variable '" + name + "' value = " + value);
            }
            ts.getVariables().put(name, value);
        } else if (log.isLoggable(Level.FINEST)) {
            log.finest("Set variable '" + name + "' value = " + value + "' requested, but no trace state");
        }
    }

    /**
     * This method removes the end part of a string beginning
     * at a specified delimiter.
     *
     * @param original The original string
     * @param delim The delimiter identifying the point to truncate
     * @return The modified string
     */
    public String truncateAtDelimiter(String original, String delim) {
        int index = original.indexOf(delim);
        if (index != -1) {
            return original.substring(0, index);
        }
        return original;
    }

    /**
     * This method returns the simple class name of the supplied
     * object.
     *
     * @param obj The object
     * @return The simple class name
     */
    public String simpleClassName(Object obj) {
        return obj.getClass().getSimpleName();
    }

    /**
     * This class represents the state information being accumulated for a
     * trace instance.
     *
     * @author gbrown
     */
    public static class TraceState {

        private Deque<Span> spanStack = new ArrayDeque<>();
        private Deque<String> idStack = new ArrayDeque<>();
        private Map<String, Object> variables = new HashMap<>();

        private long expire;

        public void pushSpan(Span span, String id) {
            spanStack.push(span);
            idStack.push(id == null ? "" : id);
        }

        public Span popSpan() {
            idStack.pop();
            return spanStack.pop();
        }

        public Span peekSpan() {
            if (spanStack.isEmpty()) {
                return null;
            }
            return spanStack.peek();
        }

        public String peekId() {
            if (idStack.isEmpty()) {
                return null;
            }
            return idStack.peek();
        }

        public boolean isFinished() {
            return spanStack.isEmpty();
        }

        /**
         * This method sets the timestamp when this trace state should
         * be considered expired.
         *
         * @param timestamp (in milliseconds)
         */
        public void setExpire(long timestamp) {
            expire = timestamp;
        }

        /**
         * This method gets the timestamp when this trace state should
         * be considered expired.
         *
         * @return timestamp (in milliseconds)
         */
        public long getExpire() {
            return expire;
        }

        /**
         * This method provides access to variables associated with this
         * trace.
         *
         * @return The variables
         */
        public Map<String, Object> getVariables() {
            return variables;
        }
    }
}
