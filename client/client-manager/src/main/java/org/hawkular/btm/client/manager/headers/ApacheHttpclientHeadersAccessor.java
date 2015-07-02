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
package org.hawkular.btm.client.manager.headers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hawkular.btm.api.client.HeadersAccessor;
import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;

/**
 * The headers accessor implementation for Apache HttpClient.
 *
 * @author gbrown
 */
public class ApacheHttpclientHeadersAccessor implements HeadersAccessor {

    private static final Logger log=Logger.getLogger(ApacheHttpclientHeadersAccessor.class.getName());

    /**  */
    private static final String TARGET_TYPE = "org.apache.http.client.methods.HttpUriRequest";

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.HeadersFactory#getTargetType()
     */
    @Override
    public String getTargetType() {
        return TARGET_TYPE;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.client.HeadersFactory#getHeaders(java.lang.Object)
     */
    @Override
    public Map<String, String> getHeaders(Object target) {
        try {
            Class<?> cls=Thread.currentThread().getContextClassLoader().
                    loadClass(TARGET_TYPE);
            Class<?> headercls=Thread.currentThread().getContextClassLoader().
                    loadClass("org.apache.http.Header");
            Method getHeaderNamesMethod=cls.getMethod("headerIterator");
            Method getNameMethod=headercls.getMethod("getName");
            Method getValueMethod=headercls.getMethod("getValue");

            // Copy header values for now, but may be more efficient to create proxy onto request
            Map<String,String> ret=new HashMap<String,String>();

            Iterator<?> iter=(Iterator<?>) getHeaderNamesMethod.invoke(target);
            while (iter.hasNext()) {
                Object header=iter.next();
                String name=(String)getNameMethod.invoke(header);
                String value=(String)getValueMethod.invoke(header);
                ret.put(name, value);
            }

            return ret;
        } catch (Throwable t) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Failed to obtain headers", t);
            }
        }

        return null;
    }
}
