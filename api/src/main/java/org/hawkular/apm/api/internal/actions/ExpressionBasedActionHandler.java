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
package org.hawkular.apm.api.internal.actions;

import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.Severity;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.ExpressionBasedAction;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * @author gbrown
 */
public abstract class ExpressionBasedActionHandler extends ProcessorActionHandler {

    private static final Logger log = Logger.getLogger(ExpressionBasedActionHandler.class.getName());

    private ExpressionHandler expression = null;

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public ExpressionBasedActionHandler(ProcessorAction action) {
        super(action);
    }

    /**
     * This method initialises the process action handler.
     *
     * @param processor The processor
     */
    @Override
    public List<ConfigMessage> init(Processor processor) {
        List<ConfigMessage> configMessages = super.init(processor);
        if (((ExpressionBasedAction) getAction()).getExpression() != null) {
            try {
                expression = ExpressionHandlerFactory.getHandler(
                        ((ExpressionBasedAction) getAction()).getExpression());

                expression.init(processor, getAction(), false);

                if (!isUsesHeaders()) {
                    setUsesHeaders(expression.isUsesHeaders());
                }
                if (!isUsesContent()) {
                    setUsesContent(expression.isUsesContent());
                }
            } catch (Throwable t) {
                log.severe(processor.getDescription() + ":" + getAction().getDescription()
                     + ":Failed to compile expression");
                ConfigMessage configMessage = new ConfigMessage();
                configMessage.setSeverity(Severity.Error);
                configMessage.setMessage(t.getMessage());
                configMessage.setField("expression");
                configMessage.setProcessor(processor.getDescription());
                configMessage.setAction(getAction().getDescription());
                configMessages.add(configMessage);
            }
        } else {
            String message = "Expression has not been defined";
            log.severe(processor.getDescription() + ":" + getAction().getDescription() + ":" + message);
            ConfigMessage configMessage = new ConfigMessage();
            configMessage.setSeverity(Severity.Error);
            configMessage.setMessage(message);
            configMessage.setField("expression");
            configMessage.setProcessor(processor.getDescription());
            configMessage.setAction(getAction().getDescription());
            configMessages.add(configMessage);
        }

        return configMessages;
    }

    /**
     * This method returns the value, associated with the expression, for the
     * supplied data.
     *
     * @param trace The trace
     * @param node The node
     * @param direction The direction
     * @param headers The optional headers
     * @param values The values
     * @return The result of the expression
     */
    protected String getValue(Trace trace, Node node, Direction direction,
            Map<String, ?> headers, Object[] values) {
        if (expression != null) {
            return expression.evaluate(trace, node, direction, headers, values);
        }

        return null;
    }
}
