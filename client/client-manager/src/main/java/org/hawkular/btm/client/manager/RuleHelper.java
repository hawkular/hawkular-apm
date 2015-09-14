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
package org.hawkular.btm.client.manager;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.model.admin.Direction;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.services.ServiceResolver;
import org.hawkular.btm.client.api.BusinessTransactionCollector;
import org.hawkular.btm.client.api.HeadersAccessor;
import org.hawkular.btm.client.api.SessionManager;
import org.hawkular.btm.client.manager.faults.FaultDescriptor;
import org.hawkular.btm.client.manager.io.InstrumentedInputStream;
import org.hawkular.btm.client.manager.io.InstrumentedOutputStream;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * This class provides utility functions for use in byteman conditions.
 *
 * @author gbrown
 */
public class RuleHelper extends Helper implements SessionManager {

    private static final Logger log = Logger.getLogger(RuleHelper.class.getName());

    private static Map<String, HeadersAccessor> headersAccessors = new HashMap<String, HeadersAccessor>();

    private static List<FaultDescriptor> faultDescriptors;

    static {
        List<HeadersAccessor> accessors = ServiceResolver.getServices(HeadersAccessor.class);

        for (HeadersAccessor accessor : accessors) {
            headersAccessors.put(accessor.getTargetType(), accessor);
        }

        faultDescriptors = ServiceResolver.getServices(FaultDescriptor.class);
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
     * This method returns the business transaction collector.
     *
     * @return The business transaction collector
     */
    public BusinessTransactionCollector collector() {
        return ClientManager.collector();
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
     * This method attempts to locate a descriptor for the fault.
     *
     * @param fault The fault
     * @return The descriptor, or null if not found
     */
    protected FaultDescriptor getFaultDescriptor(Object fault) {
        for (int i=0; i < faultDescriptors.size(); i++) {
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
        FaultDescriptor fd=getFaultDescriptor(fault);
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
        FaultDescriptor fd=getFaultDescriptor(fault);
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

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#activate(java.lang.String, java.lang.String)
     */
    @Override
    public boolean activate(String uri, String id) {
        return collector().session().activate(uri, id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#activate(java.lang.String)
     */
    @Override
    public boolean activate(String uri) {
        return collector().session().activate(uri);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#isActive()
     */
    @Override
    public boolean isActive() {
        return collector().session().isActive();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#retainNode(java.lang.String)
     */
    @Override
    public void retainNode(String id) {
        collector().session().retainNode(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#releaseNode(java.lang.String)
     */
    @Override
    public void releaseNode(String id) {
        collector().session().releaseNode(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#retrieveNode(java.lang.String)
     */
    @Override
    public Node retrieveNode(String id) {
        return collector().session().retrieveNode(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#initiateLink(java.lang.String)
     */
    @Override
    public void initiateCorrelation(String id) {
        collector().session().initiateCorrelation(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#isLinkActive(java.lang.String)
     */
    @Override
    public boolean isCorrelated(String id) {
        return collector().session().isCorrelated(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#joinLink(java.lang.String)
     */
    @Override
    public void correlate(String id) {
        collector().session().correlate(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#completeLink(java.lang.String)
     */
    @Override
    public void completeCorrelation(String id) {
        collector().session().completeCorrelation(id);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#unlink()
     */
    @Override
    public void unlink() {
        collector().session().unlink();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#suppress()
     */
    @Override
    public void suppress() {
        collector().session().suppress();
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.api.SessionManager#assertComplete()
     */
    @Override
    public void assertComplete() {
        collector().session().assertComplete();
    }

    /**
     * This method returns the business transaction name.
     *
     * @return The business transaction name
     */
    public String getBusinessTransactionName() {
        return collector().getName();
    }

    /**
     * This method initialises a data buffer associated with the supplied request object.
     *
     * @param obj The object associated with the buffer
     */
    public void initRequestBuffer(Object obj) {
        collector().initRequestBuffer(getRuleName(), obj);
    }

    /**
     * This method determines if there is an active request data buffer for
     * the supplied object.
     *
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    public boolean isRequestBufferActive(Object obj) {
        return collector().isRequestBufferActive(getRuleName(), obj);
    }

    /**
     * This method appends data to the buffer associated with the supplied request object.
     *
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     * @param close Whether to close the buffer after appending the data
     */
    public void appendRequestBuffer(Object obj, byte[] data, int offset, int len, boolean close) {
        if (len > 0) {
            collector().appendRequestBuffer(getRuleName(), obj, data, offset, len);
        }
        if (close) {
            collector().recordRequestBuffer(getRuleName(), obj);
        }
    }

    /**
     * This method records the data within a buffer associated with the supplied request
     * object.
     *
     * @param obj The object associated with the buffer
     */
    public void recordRequestBuffer(Object obj) {
        collector().recordRequestBuffer(getRuleName(), obj);
    }

    /**
     * This method initialises a data buffer associated with the supplied response object.
     *
     * @param obj The object associated with the buffer
     */
    public void initResponseBuffer(Object obj) {
        collector().initResponseBuffer(getRuleName(), obj);
    }

    /**
     * This method determines if there is an active response data buffer for
     * the supplied object.
     *
     * @param obj The object associated with the buffer
     * @return Whether there is an active data buffer
     */
    public boolean isResponseBufferActive(Object obj) {
        return collector().isResponseBufferActive(getRuleName(), obj);
    }

    /**
     * This method appends data to the buffer associated with the supplied response object.
     *
     * @param obj The object associated with the buffer
     * @param data The data to be appended
     * @param offset The offset of the data
     * @param len The length of data
     * @param close Whether to close the buffer after appending the data
     */
    public void appendResponseBuffer(Object obj, byte[] data, int offset, int len, boolean close) {
        if (len > 0) {
            collector().appendResponseBuffer(getRuleName(), obj, data, offset, len);
        }
        if (close) {
            collector().recordResponseBuffer(getRuleName(), obj);
        }
    }

    /**
     * This method records the data within a buffer associated with the supplied response
     * object.
     *
     * @param obj The object associated with the buffer
     */
    public void recordResponseBuffer(Object obj) {
        collector().recordResponseBuffer(getRuleName(), obj);
    }

    /**
     * This method determines if the request headers or content is processed.
     *
     * @return Whether the request headers or content is processed
     */
    public boolean isRequestProcessed() {
        return (collector().isRequestProcessed(getRuleName()));
    }

    /**
     * This method determines if the request content is processed.
     *
     * @return Whether the request content is processed
     */
    public boolean isRequestContentProcessed() {
        return (collector().isRequestContentProcessed(getRuleName()));
    }

    /**
     * This method determines if the response headers or content is processed.
     *
     * @return Whether the response headers or content is processed
     */
    public boolean isResponseProcessed() {
        return (collector().isResponseProcessed(getRuleName()));
    }

    /**
     * This method determines if the response content is processed.
     *
     * @return Whether the response content is processed
     */
    public boolean isResponseContentProcessed() {
        return (collector().isResponseContentProcessed(getRuleName()));
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param os The original output stream
     * @return The instrumented output stream
     */
    public OutputStream createRequestOutputStream(OutputStream os) {
        return new InstrumentedOutputStream(collector(), Direction.Request, os, null);
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data. The
     * optional link id can be used to initiate a link with the specified
     * id (and disassociate the business txn from the current thread).
     *
     * @param os The original output stream
     * @param linkId The optional link id
     * @return The instrumented output stream
     */
    public OutputStream createRequestOutputStream(OutputStream os, String linkId) {
        return new InstrumentedOutputStream(collector(), Direction.Request, os, linkId);
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param os The original output stream
     * @return The instrumented output stream
     */
    public OutputStream createResponseOutputStream(OutputStream os) {
        return new InstrumentedOutputStream(collector(), Direction.Response, os, null);
    }

    /**
     * This method returns an instrumented proxy output stream, to wrap
     * the supplied output stream, which will record the written data. The
     * optional link id can be used to initiate a link with the specified
     * id (and disassociate the business txn from the current thread).
     *
     * @param os The original output stream
     * @param linkId The optional link id
     * @return The instrumented output stream
     */
    public OutputStream createResponseOutputStream(OutputStream os, String linkId) {
        return new InstrumentedOutputStream(collector(), Direction.Response, os, linkId);
    }

    /**
     * This method returns an instrumented proxy input stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param is The original input stream
     * @return The instrumented input stream
     */
    public InputStream createRequestInputStream(InputStream is) {
        return new InstrumentedInputStream(collector(), Direction.Request, is);
    }

    /**
     * This method returns an instrumented proxy input stream, to wrap
     * the supplied output stream, which will record the written data.
     *
     * @param is The original input stream
     * @return The instrumented input stream
     */
    public InputStream createResponseInputStream(InputStream is) {
        return new InstrumentedInputStream(collector(), Direction.Response, is);
    }
}
