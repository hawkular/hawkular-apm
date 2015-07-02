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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.btm.api.client.HeadersAccessor;
import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;

/**
 * The headers accessor implementation for javax servlets.
 *
 * @author gbrown
 */
public class JavaxServletHeadersAccessor implements HeadersAccessor {

    private static final Logger log=Logger.getLogger(JavaxServletHeadersAccessor.class.getName());

    /**  */
    private static final String TARGET_TYPE = "javax.servlet.http.HttpServletRequest";

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
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> getHeaders(Object target) {
        try {
            Class<?> cls=Thread.currentThread().getContextClassLoader().
                    loadClass(TARGET_TYPE);
            Method getHeaderNamesMethod=cls.getMethod("getHeaderNames");
            Method getHeaderMethod=cls.getMethod("getHeader",String.class);

            // Copy header values for now, but may be more efficient to create proxy onto request
            Map<String,String> ret=new HashMap<String,String>();

            Enumeration<String> iter=(Enumeration<String>) getHeaderNamesMethod.invoke(target);
            while (iter.hasMoreElements()) {
                String key=iter.nextElement();
                String value=(String)getHeaderMethod.invoke(target, key);
                ret.put(key, value);
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
