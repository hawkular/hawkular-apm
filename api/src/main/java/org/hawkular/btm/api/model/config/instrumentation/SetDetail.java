/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.model.config.instrumentation;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This type represents the action for setting the detail name and value.
 *
 * @author gbrown
 */
public class SetDetail extends InstrumentAction {

    @JsonInclude
    private String name;

    @JsonInclude
    private String valueExpression;

    @JsonInclude
    private String nodeType;

    @JsonInclude
    private boolean onStack;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

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

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * @param nodeType the nodeType to set
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * @return the onStack
     */
    public boolean isOnStack() {
        return onStack;
    }

    /**
     * @param onStack the onStack to set
     */
    public void setOnStack(boolean onStack) {
        this.onStack = onStack;
    }

}
