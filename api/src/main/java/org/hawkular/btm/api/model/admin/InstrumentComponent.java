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
package org.hawkular.btm.api.model.admin;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This type represents instrumentation of a component.
 *
 * @author gbrown
 */
public class InstrumentComponent extends CollectorAction {

    @JsonInclude
    private String componentTypeExpression;

    @JsonInclude
    private String operationExpression;

    @JsonInclude
    private String uriExpression;

    /**
     * @return the componentTypeExpression
     */
    public String getComponentTypeExpression() {
        return componentTypeExpression;
    }

    /**
     * @param componentTypeExpression the componentTypeExpression to set
     */
    public void setComponentTypeExpression(String componentTypeExpression) {
        this.componentTypeExpression = componentTypeExpression;
    }

    /**
     * @return the operationExpression
     */
    public String getOperationExpression() {
        return operationExpression;
    }

    /**
     * @param operationExpression the operationExpression to set
     */
    public void setOperationExpression(String operationExpression) {
        this.operationExpression = operationExpression;
    }

    /**
     * @return the uriExpression
     */
    public String getUriExpression() {
        return uriExpression;
    }

    /**
     * @param uriExpression the uriExpression to set
     */
    public void setUriExpression(String uriExpression) {
        this.uriExpression = uriExpression;
    }

}
