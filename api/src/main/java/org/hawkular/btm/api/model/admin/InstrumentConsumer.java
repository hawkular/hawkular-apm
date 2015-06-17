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
 * This type represents instrumentation of a consumer.
 *
 * @author gbrown
 */
public class InstrumentConsumer extends CollectorAction {

    @JsonInclude
    private String endpointTypeExpression;

    @JsonInclude
    private String uriExpression;

    @JsonInclude
    private String idExpression;

    /**
     * @return the endpointTypeExpression
     */
    public String getEndpointTypeExpression() {
        return endpointTypeExpression;
    }

    /**
     * @param endpointTypeExpression the endpointTypeExpression to set
     */
    public void setEndpointTypeExpression(String endpointTypeExpression) {
        this.endpointTypeExpression = endpointTypeExpression;
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

    /**
     * @return the idExpression
     */
    public String getIdExpression() {
        return idExpression;
    }

    /**
     * @param idExpression the idExpression to set
     */
    public void setIdExpression(String idExpression) {
        this.idExpression = idExpression;
    }

}
