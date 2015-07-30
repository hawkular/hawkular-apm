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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.hawkular.btm.api.services.ServiceResolver;
import org.hawkular.btm.client.api.BusinessTransactionCollector;
import org.hawkular.btm.client.api.HeadersAccessor;
import org.hawkular.btm.client.manager.faults.FaultDescriptor;
import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * This class provides utility functions for use in byteman conditions.
 *
 * @author gbrown
 */
public class RuleHelper extends Helper {

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
}
