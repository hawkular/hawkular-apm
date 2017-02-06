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

import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentAction;

/**
 * This interface represents a transformer to convert the instrumentation
 * action into a ByteMan rule action.
 *
 * @author gbrown
 */
public interface InstrumentActionTransformer {

    /**
     * This method returns the action type associated with the
     * transformer.
     *
     * @return The action type
     */
    Class<? extends InstrumentAction> getActionType();

    /**
     * This method converts the supplied instrumentation action into a
     * ByteMan rule action.
     *
     * @param action The instrument action
     * @return The ByteMan rule action
     */
    String convertToRuleAction(InstrumentAction action);

}
