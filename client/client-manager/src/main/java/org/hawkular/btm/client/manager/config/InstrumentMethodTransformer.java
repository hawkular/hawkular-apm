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

import org.hawkular.btm.api.model.admin.InstrumentMethod;
import org.hawkular.btm.api.model.admin.InstrumentType;
import org.hawkular.btm.client.manager.ClientManager;

/**
 * This class transforms the InstrumentMethod type.
 *
 * @author gbrown
 */
public class InstrumentMethodTransformer implements InstrumentTypeTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.byteman.InstrumentTypeTransformer#getInstrumentType()
     */
    @Override
    public Class<? extends InstrumentType> getInstrumentType() {
        return InstrumentMethod.class;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.byteman.InstrumentTypeTransformer#convertToRule(
     *                  org.hawkular.btm.api.model.admin.InstrumentType)
     */
    @Override
    public String convertToRule(InstrumentType type) {
        StringBuilder builder = new StringBuilder();

        builder.append("RULE ");
        builder.append(type.getRuleName());
        builder.append("\r\n");

        builder.append("CLASS ");
        builder.append(type.getClassName());
        builder.append("\r\n");

        builder.append("METHOD ");
        builder.append(((InstrumentMethod) type).getMethodName());
        builder.append('(');

        for (int i = 0; i < ((InstrumentMethod) type).getParameterTypes().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(((InstrumentMethod) type).getParameterTypes().get(i));
        }

        builder.append(')');
        builder.append("\r\n");

        builder.append("IF true\r\n");

        builder.append("DO "+ClientManager.class.getName()+".collector().print(\"Hello BTM\")\r\n");
        builder.append("ENDRULE\r\n");

        return builder.toString();
    }

}
