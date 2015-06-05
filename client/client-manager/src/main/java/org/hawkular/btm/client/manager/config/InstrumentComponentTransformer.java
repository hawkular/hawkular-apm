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

import org.hawkular.btm.api.model.admin.InstrumentComponent;
import org.hawkular.btm.api.model.admin.InstrumentInvocation;
import org.hawkular.btm.api.model.admin.InstrumentType;

/**
 * This class transforms the InstrumentComponent type.
 *
 * @author gbrown
 */
public class InstrumentComponentTransformer extends InstrumentInvocationTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.byteman.InstrumentTypeTransformer#getInstrumentType()
     */
    @Override
    public Class<? extends InstrumentType> getInstrumentType() {
        return InstrumentComponent.class;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentInvocationTransformer#getEntity()
     */
    @Override
    protected String getEntity() {
        return "component";
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentInvocationTransformer#getParameters()
     */
    @Override
    protected String[] getParameters(InstrumentInvocation invocation) {
        String[] ret=new String[3];
        ret[0] = ((InstrumentComponent)invocation).getComponentTypeExpression();
        ret[1] = ((InstrumentComponent)invocation).getOperationExpression();
        ret[2] = ((InstrumentComponent)invocation).getUriExpression();
        return (ret);
    }

}