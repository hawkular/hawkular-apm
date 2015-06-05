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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This type represents instrumentation of an invocation.
 *
 * @author gbrown
 */
public abstract class InstrumentInvocation extends InstrumentType {

    @JsonInclude
    private String methodName;

    @JsonInclude
    private List<String> parameterTypes = new ArrayList<String>();

    @JsonInclude
    private List<String> requestValueExpressions = new ArrayList<String>();

    @JsonInclude
    private List<String> responseValueExpressions = new ArrayList<String>();

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param methodName the method to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return the parameterTypes
     */
    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * @param parameterTypes the parameterTypes to set
     */
    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /**
     * @return the requestValueExpressions
     */
    public List<String> getRequestValueExpressions() {
        return requestValueExpressions;
    }

    /**
     * @param requestValueExpressions the requestValueExpressions to set
     */
    public void setRequestValueExpressions(List<String> requestValueExpressions) {
        this.requestValueExpressions = requestValueExpressions;
    }

    /**
     * @return the responseValueExpressions
     */
    public List<String> getResponseValueExpressions() {
        return responseValueExpressions;
    }

    /**
     * @param responseValueExpressions the responseValueExpressions to set
     */
    public void setResponseValueExpressions(List<String> responseValueExpressions) {
        this.responseValueExpressions = responseValueExpressions;
    }

}
