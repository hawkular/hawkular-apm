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
 * This type represents instrumentation of a service.
 *
 * @author gbrown
 */
public class InstrumentService extends InstrumentInvocation {

    @JsonInclude
    private String serviceTypeExpression;

    @JsonInclude
    private String operationExpression;

    /**
     * @return the serviceTypeExpression
     */
    public String getServiceTypeExpression() {
        return serviceTypeExpression;
    }

    /**
     * @param serviceTypeExpression the serviceTypeExpression to set
     */
    public void setServiceTypeExpression(String serviceTypeExpression) {
        this.serviceTypeExpression = serviceTypeExpression;
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

}
