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

import org.hawkular.btm.api.model.admin.CollectorAction;
import org.hawkular.btm.api.model.admin.InstrumentAction;
import org.hawkular.btm.api.model.admin.InstrumentProducer;
import org.hawkular.btm.api.model.admin.CollectorAction.Direction;

/**
 * This class transforms the InstrumentProducer type.
 *
 * @author gbrown
 */
public class InstrumentProducerTransformer extends CollectorActionTransformer {

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentActionTransformer#getActionType()
     */
    @Override
    public Class<? extends InstrumentAction> getActionType() {
        return InstrumentProducer.class;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentInvocationTransformer#getEntity()
     */
    @Override
    protected String getEntity() {
        return "producer";
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.manager.config.InstrumentInvocationTransformer#getParameters()
     */
    @Override
    protected String[] getParameters(CollectorAction invocation) {
        String[] ret = new String[invocation.getDirection() == Direction.Request ? 3 : 2];

        ret[0] = ((InstrumentProducer) invocation).getEndpointTypeExpression();
        ret[1] = ((InstrumentProducer) invocation).getUriExpression();

        if (invocation.getDirection() == Direction.Request) {
            ret[2] = ((InstrumentProducer) invocation).getIdExpression();
        }

        return (ret);
    }

}
