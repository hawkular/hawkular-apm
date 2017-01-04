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
 * This type represents the session action for correlating the current
 * thread of execution with another, identified by an id. Once correlated
 * the correlation id becomes inactive, no longer being available for use
 * (i.e. one time correlation).
 *
 * @author gbrown
 */
public class CompleteCorrelation extends SessionAction {

    @JsonInclude
    private String idExpression;

    @JsonInclude
    private boolean allowSpawn;

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

    /**
     * @return the allowSpawn
     */
    public boolean isAllowSpawn() {
        return allowSpawn;
    }

    /**
     * @param allowSpawn the allowSpawn to set
     */
    public void setAllowSpawn(boolean allowSpawn) {
        this.allowSpawn = allowSpawn;
    }

 }
