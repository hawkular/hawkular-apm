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
package org.hawkular.apm.api.model.config.instrumentation.jvm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This type represents the action for setting the trace id.
 *
 * @author gbrown
 */
public class SetTraceId extends InstrumentAction {

    @JsonInclude
    private String valueExpression;

    /**
     * @return the valueExpression
     */
    public String getValueExpression() {
        return valueExpression;
    }

    /**
     * @param valueExpression the valueExpression to set
     */
    public void setValueExpression(String valueExpression) {
        this.valueExpression = valueExpression;
    }

}
