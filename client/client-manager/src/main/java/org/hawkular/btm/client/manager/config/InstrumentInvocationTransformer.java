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
package org.hawkular.btm.client.manager.config;

import org.hawkular.btm.api.internal.client.ArrayBuilder;
import org.hawkular.btm.api.model.admin.InstrumentInvocation;
import org.hawkular.btm.api.model.admin.InstrumentType;
import org.hawkular.btm.client.manager.ClientManager;

/**
 * This class transforms the InstrumentInvocation type.
 *
 * @author gbrown
 */
public abstract class InstrumentInvocationTransformer implements InstrumentTypeTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.byteman.InstrumentTypeTransformer#convertToRule(
     *                  org.hawkular.btm.api.model.admin.InstrumentType)
     */
    @Override
    public String convertToRule(InstrumentType type) {
        // NOTE: This class could use a templating mechanism to provide the
        // boilerplate rule - however it will be executed on the client side,
        // so want to avoid any unnecessary dependencies

        InstrumentInvocation invocation=(InstrumentInvocation)type;
        StringBuilder builder = new StringBuilder();

        builder.append("RULE ");
        builder.append(type.getRuleName());
        builder.append("_entry");
        builder.append("\r\n");

        builder.append("CLASS ");
        builder.append(type.getClassName());
        builder.append("\r\n");

        builder.append("METHOD ");
        builder.append(invocation.getMethodName());
        builder.append('(');

        for (int i = 0; i < invocation.getParameterTypes().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(invocation.getParameterTypes().get(i));
        }

        builder.append(')');
        builder.append("\r\n");
        builder.append("AT ENTRY\r\n");

        builder.append("IF true\r\n");

        builder.append("DO "+ClientManager.class.getName()+".collector()."+getEntity()+"Start(");

        String[] params=getParameters(invocation);
        for (int i=0; i < params.length; i++) {
            builder.append(params[i]);
            builder.append(',');
        }

        builder.append(ArrayBuilder.class.getName());
        builder.append(".create()");

        for (String expr : invocation.getRequestValueExpressions()) {
            builder.append(".add(");
            builder.append(expr);
            builder.append(')');
        }

        builder.append(".get()");

        builder.append(")\r\n");

        builder.append("ENDRULE\r\n\r\n");

        builder.append("RULE ");
        builder.append(type.getRuleName());
        builder.append("_exit");
        builder.append("\r\n");

        builder.append("CLASS ");
        builder.append(type.getClassName());
        builder.append("\r\n");

        builder.append("METHOD ");
        builder.append(invocation.getMethodName());
        builder.append('(');

        for (int i = 0; i < invocation.getParameterTypes().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(invocation.getParameterTypes().get(i));
        }

        builder.append(')');
        builder.append("\r\n");
        builder.append("AT EXIT\r\n");

        builder.append("IF true\r\n");

        builder.append("DO "+ClientManager.class.getName()+".collector()."+getEntity()+"End(");

        for (int i=0; i < params.length; i++) {
            builder.append(params[i]);
            builder.append(',');
        }

        builder.append(ArrayBuilder.class.getName());
        builder.append(".create()");

        for (String expr : invocation.getResponseValueExpressions()) {
            builder.append(".add(");
            builder.append(expr);
            builder.append(')');
        }

        builder.append(".get()");

        builder.append(")\r\n");

        builder.append("ENDRULE\r\n");

        return builder.toString();
    }

    /**
     * This method identifies the entity being instrumented.
     *
     * @return The entity
     */
    protected abstract String getEntity();

    /**
     * This method supplies the array of fixed parameters.
     *
     * @param invocation The invocation details
     * @return The array of fixed parameters
     */
    protected abstract String[] getParameters(InstrumentInvocation invocation);

}
