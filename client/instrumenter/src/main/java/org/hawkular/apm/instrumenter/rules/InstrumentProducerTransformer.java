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
package org.hawkular.apm.instrumenter.rules;

import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.CollectorAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentProducer;

/**
 * This class transforms the InstrumentProducer type.
 *
 * @author gbrown
 */
public class InstrumentProducerTransformer extends CollectorActionTransformer {

    @Override
    public Class<? extends InstrumentAction> getActionType() {
        return InstrumentProducer.class;
    }

    @Override
    protected String getEntity() {
        return "producer";
    }

    @Override
    protected String[] getParameters(CollectorAction invocation) {
        String[] ret = new String[invocation.getDirection() == Direction.In ? 4 : 3];

        ret[0] = ((InstrumentProducer) invocation).getUriExpression();
        ret[1] = ((InstrumentProducer) invocation).getEndpointTypeExpression();
        ret[2] = ((InstrumentProducer) invocation).getOperationExpression();

        if (invocation.getDirection() == Direction.In) {
            ret[3] = ((InstrumentProducer) invocation).getIdExpression();
        }

        return (ret);
    }

}
