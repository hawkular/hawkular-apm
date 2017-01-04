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
package org.hawkular.apm.instrumenter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.client.collector.SessionManager;
import org.hawkular.apm.client.collector.TraceCollector;
import org.hawkular.apm.instrumenter.faults.FaultDescriptor;
import org.hawkular.apm.instrumenter.headers.HeadersAccessor;
import org.hawkular.apm.instrumenter.io.InstrumentedInputStream;
import org.hawkular.apm.instrumenter.io.InstrumentedOutputStream;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * This class provides utility functions for use in byteman conditions.
 *
 * @author gbrown
 */
public class RuleHelper extends Helper implements SessionManager {

    public static final String BINARY_SQL_MARKER = "<binary>";

    private static final Logger log = Logger.getLogger(RuleHelper.class.getName());

    private static Map<String, HeadersAccessor> headersAccessors = new HashMap<String, HeadersAccessor>();

    private static List<FaultDescriptor> faultDescriptors;

    private static TraceCollector collector;

    static {
        List<HeadersAccessor> accessors = ServiceResolver.getServices(HeadersAccessor.class);

        for (HeadersAccessor accessor : accessors) {
            headersAccessors.put(accessor.getTargetType(), accessor);
        }

        faultDescriptors = ServiceResolver.getServices(FaultDescriptor.class);

        // Obtain collector
        collector = ServiceResolver.getSingletonService(TraceCollector.class);
    }

    /**
     * @param rule
     */
    protected RuleHelper(Rule rule) {
        super(rule);
    }

    /**
     * This method returns the name of the instrumentation rule.
     *
     * @return The rule name
     */
    public String getRuleName() {
        return (rule.getName());
    }

    /**
     * This method returns the trace collector.
     *
     * @return The trace collector
     */
    public TraceCollector collector() {
        return collector;
    }

    /**
     * This method creates a unique id.
     *
     * @return The unique id
     */
    public String createUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * This method returns an ID associated with the supplied
     * type and object.
     *
     * @param type The type represents the use (or context) of the object
     * @param obj The object
     * @return The id
     */
    public String getID(String type, Object obj) {
        return type + obj.hashCode();
    }

    /**
     * This method determines whether the supplied object is an
     * instance of the supplied class/interface.
     *
     * @param obj The object
     * @param clz The class
     * @return Whether the object is an instance of the class
     */
    public boolean isInstanceOf(Object obj, Class<?> clz) {
        if (obj == null || clz == null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("isInstanceOf error: obj=" + obj + " clz=" + clz);
            }
            return false;
        }
        return clz.isAssignableFrom(obj.getClass());
    }

    /**
     * This method casts the supplied object to the nominated
     * class. If the object cannot be cast to the provided type,
     * then a null will be returned.
     *
     * @param obj The object
     * @param clz The class to cast to
     * @return The cast object, or null if the object cannot be cast
     */
    public <T> T cast(Object obj, Class<T> clz) {
        if (!clz.isAssignableFrom(obj.getClass())) {
            return null;
        }
        return clz.cast(obj);
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
     * This method returns the string representation of the
     * supplied object.
     *
     * @param obj The object
     * @return The string representation
     */
    public String toString(Object obj) {
        return obj.toString();
    }

    /**
     * This method returns the hash code for the supplied object.
     *
     * @param obj The object
     * @return The hash code
     */
    public int hashCode(Object obj) {
        return obj.hashCode();
    }

    /**
     * This method attempts to return a SQL statement. If an
     * expression is supplied, and is string, it will be used.
     * Otherwise the method will attempt to derive an expression
     * from the supplied object.
     *
     * @param obj The object
     * @param expr The optional expression to use
     * @return The SQL statement
     */
    public String formatSQL(Object obj, Object expr) {
        String sql = null;

        // Check whether an SQL statement has been provided
        if (expr instanceof String) {
            sql = (String)expr;

            if (log.isLoggable(Level.FINEST)) {
                log.finest("SQL retrieved from state = "+sql);
            }
        } else if (obj != null) {
            sql = toString(obj);

            if (sql != null) {
                if (sql.startsWith("prep")) {
                    sql = sql.replaceFirst("prep[0-9]*: ", "");
                }
                sql = sql.replaceAll("X'.*'", BINARY_SQL_MARKER);
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("SQL derived from context = "+sql);
            }
        }

        return sql;
    }

    /**
     * This method attempts to locate a descriptor for the fault.
     *
     * @param fault The fault
     * @return The descriptor, or null if not found
     */
    protected FaultDescriptor getFaultDescriptor(Object fault) {
        for (int i = 0; i < faultDescriptors.size(); i++) {
            if (faultDescriptors.get(i).isValid(fault)) {
                return faultDescriptors.get(i);
            }
        }
        return null;
    }

    /**
     * This method gets the name of the supplied fault.
     *
     * @param fault The fault
     * @return The name
     */
    public String faultName(Object fault) {
        FaultDescriptor fd = getFaultDescriptor(fault);
        if (fd != null) {
            return fd.getName(fault);
        }
        return fault.getClass().getSimpleName();
    }

    /**
     * This method gets the description of the supplied fault.
     *
     * @param fault The fault
     * @return The description
     */
    public String faultDescription(Object fault) {
        FaultDescriptor fd = getFaultDescriptor(fault);
        if (fd != null) {
            return fd.getDescription(fault);
        }
        return fault.toString();
    }

    /**
     * This method removes the supplied suffix (if it exists) in the
     * supplied 'original' string.
     *
     * @param original The original string
     * @param suffix The suffix to remove
     * @return The modified string
     */
    public String removeSuffix(String original, String suffix) {
        if (original.endsWith(suffix)) {
            return original.substring(0, original.length() - suffix.length());
        }
        return original;
    }

    /**
     * This method removes the end part of a string beginning
     * at a specified marker.
     *
     * @param original The original string
     * @param marker The marker identifying the point to remove from
     * @return The modified string
     */
    public String removeAfter(String original, String marker) {
        int index = original.indexOf(marker);
        if (index != -1) {
            return original.substring(0, index);
        }
        return original;
    }

    /**
     * This method creates a new parameter array builder.
     *
     * @return The parameter array builder
     */
    public ArrayBuilder createArrayBuilder() {
        return (new ArrayBuilder());
    }

    /**
     * This method attempts to provide headers for the supplied target
     * object.
     *
     * @param type The target type
     * @param target The target instance
     * @return The header map
     */
    public Map<String, String> getHeaders(String type, Object target) {
        HeadersAccessor accessor = getHeadersAccessor(type);
        if (accessor != null) {
            Map<String, String> ret = accessor.getHeaders(target);
            return ret;
        }
        return null;
    }

    /**
     * This method returns the headers accessor for the supplied type.
     *
     * @param type The type
     * @return The headers accessor, or null if not found
     */
    protected HeadersAccessor getHeadersAccessor(String type) {
        return (headersAccessors.get(type));
    }

    @Override
    public boolean activate(String uri, String operation, String id) {
        return collector().session().activate(uri, operation, id);
    }

    @Override
    public boolean activate(String uri, String operation) {
        return collector().session().activate(uri, operation);
    }

    @Override
    public boolean isActive() {
        return collector().session().isActive();
    }

    @Override
    public void deactivate() {
        collector().session().deactivate();
    }

    @Override
    public void retainNode(String id) {
        collector().session().retainNode(id);
    }

    @Override
    public void releaseNode(String id) {
        collector().session().releaseNode(id);
    }

    @Override
    public Node retrieveNode(String id) {
        return collector().session().retrieveNode(id);
    }

    @Override
    public void initiateCorrelation(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Initiate correlation location=[" + getRuleName() + "] id=[" + id + "]");
        }
        collector().session().initiateCorrelation(id);
    }

    @Override
    public boolean isCorrelated(String id) {
        return collector().session().isCorrelated(id);
    }

    @Override
    public void correlate(String id) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Correlate location=[" + getRuleName() + "] id=[" + id + "]");
        }
        collector().session().correlate(id);
    }

    @Override
    public void completeCorrelation(String id, boolean allowSpawn) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Complete correlation location=[" + getRuleName() + "] id=[" + id + "] "
                    + " allowSpawn=" + allowSpawn);
        }
        collector().session().completeCorrelation(id, allowSpawn);
    }

    @Override
    public void unlink() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Unlink location=[" + getRuleName() + "]");
        }
        collector().session().unlink();
    }

    @Override
    public void suppress() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Suppress location=[" + getRuleName() + "]");
        }
        collector().session().suppress();
    }

    @Override
    public void ignoreNode() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Ignore node location=[" + getRuleName() + "]");
        }
        collector().session().ignoreNode();
    }

    @Override
    public void assertComplete() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Assert complete location=[" + getRuleName() + "]");
        }
        collector().session().assertComplete();
    }

    /**
     * This method returns the trace id.
     *
     * @return The trace id
     */
    public String getTraceId() {
        return collector().getTraceId();
    }

    /**
     * This method returns the transaction name.
     *
     * @return The transaction name
     */
    public String getTransactionName() {
        return collector().getTransaction();
    }

    /**
     * This method returns the reporting level.
     *
     * @return The reporting level
     */
    public String getLevel() {
        return collector().getLevel();
    }

    /**
     * This method initialises a data buffer associated with the supplied object.
     *
     * @param obj The object associated with the buffer
     */
    public void initInBuffer(Object obj) {
        collector().initInBuffer(getRuleName(), obj);
    }

    /**
     * This method determines if there is an active in data buffer for
     * the supplied object.
     *
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    public boolean isInBufferActive(Object obj) {
        return collector().isInBufferActive(getRuleName(), obj);
    }

    /**
     * This method appends data to the buffer associated with the supplied object.
     *
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     * @param close Whether to close the buffer after appending the data
     */
    public void appendInBuffer(Object obj, byte[] data, int offset, int len, boolean close) {
        if (len > 0) {
            collector().appendInBuffer(getRuleName(), obj, data, offset, len);
        }
        if (close) {
            collector().recordInBuffer(getRuleName(), obj);
        }
    }

    /**
     * This method records the data within a buffer associated with the supplied
     * object.
     *
     * @param obj The object associated with the buffer
     */
    public void recordInBuffer(Object obj) {
        collector().recordInBuffer(getRuleName(), obj);
    }

    /**
     * This method initialises a data buffer associated with the supplied object.
     *
     * @param obj The object associated with the buffer
     */
    public void initOutBuffer(Object obj) {
        collector().initOutBuffer(getRuleName(), obj);
    }

    /**
     * This method determines if there is an active out data buffer for
     * the supplied object.
     *
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    public boolean isOutBufferActive(Object obj) {
        return collector().isOutBufferActive(getRuleName(), obj);
    }

    /**
     * This method appends data to the buffer associated with the supplied object.
     *
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     * @param close Whether to close the buffer after appending the data
     */
    public void appendOutBuffer(Object obj, byte[] data, int offset, int len, boolean close) {
        if (len > 0) {
            collector().appendOutBuffer(getRuleName(), obj, data, offset, len);
        }
        if (close) {
            collector().recordOutBuffer(getRuleName(), obj);
        }
    }

    /**
     * This method records the data within a buffer associated with the supplied
     * object.
     *
     * @param obj The object associated with the buffer
     */
    public void recordOutBuffer(Object obj) {
        collector().recordOutBuffer(getRuleName(), obj);
    }

    /**
     * This method determines if the in headers or content is processed.
     *
     * @return Whether the in headers or content is processed
     */
    public boolean isInProcessed() {
        return (collector().isInProcessed(getRuleName()));
    }

    /**
     * This method determines if the in content is processed.
     *
     * @return Whether the in content is processed
     */
    public boolean isInContentProcessed() {
        return (collector().isInContentProcessed(getRuleName()));
    }

    /**
     * This method determines if the out headers or content is processed.
     *
     * @return Whether the out headers or content is processed
     */
    public boolean isOutProcessed() {
        return (collector().isOutProcessed(getRuleName()));
    }

    /**
     * This method determines if the out content is processed.
     *
     * @return Whether the out content is processed
     */
    public boolean isOutContentProcessed() {
        return (collector().isOutContentProcessed(getRuleName()));
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param os The original output stream
     * @return The instrumented output stream
     */
    public OutputStream createInOutputStream(OutputStream os) {
        return new InstrumentedOutputStream(collector(), Direction.In, os, null);
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data. The
     * optional link id can be used to initiate a link with the specified
     * id (and disassociate the trace from the current thread).
     *
     * @param os The original output stream
     * @param linkId The optional link id
     * @return The instrumented output stream
     */
    public OutputStream createInOutputStream(OutputStream os, String linkId) {
        return new InstrumentedOutputStream(collector(), Direction.In, os, linkId);
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param os The original output stream
     * @return The instrumented output stream
     */
    public OutputStream createOutOutputStream(OutputStream os) {
        return new InstrumentedOutputStream(collector(), Direction.Out, os, null);
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data. The
     * optional link id can be used to initiate a link with the specified
     * id (and disassociate the trace from the current thread).
     *
     * @param os The original output stream
     * @param linkId The optional link id
     * @return The instrumented output stream
     */
    public OutputStream createOutOutputStream(OutputStream os, String linkId) {
        return new InstrumentedOutputStream(collector(), Direction.Out, os, linkId);
    }

    /**
     * This method returns an instrumented proxy input stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param is The original input stream
     * @return The instrumented input stream
     */
    public InputStream createInInputStream(InputStream is) {
        return new InstrumentedInputStream(collector(), Direction.In, is);
    }

    /**
     * This method returns an instrumented proxy input stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param is The original input stream
     * @return The instrumented input stream
     */
    public InputStream createOutInputStream(InputStream is) {
        return new InstrumentedInputStream(collector(), Direction.Out, is);
    }

    @Override
    public void setState(Object context, String name, Object value, boolean session) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Set state location=[" + getRuleName() + "] context=[" + context + "] name=[" + name +
                    "] value=[" + value + "] session=[" + session + "]");
        }
        collector().session().setState(context, name, value, session);
    }

    @Override
    public Object getState(Object context, String name, boolean session) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get state location=[" + getRuleName() + "] context=[" + context +
                    "] name=[" + name + "] session=[" + session + "]");
        }

        Object ret = collector().session().getState(context, name, session);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Returning state location=[" + getRuleName() + "] context=[" + context +
                    "] name=[" + name + "] session=[" + session + "] state=" + ret);
        }

        return ret;
    }
}
