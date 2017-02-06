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

import org.hawkular.apm.api.model.config.Direction;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This type represents instrumentation of an invocation.
 *
 * @author gbrown
 */
public abstract class CollectorAction extends InstrumentAction {

    @JsonInclude
    private String uriExpression;

    @JsonInclude
    private String idExpression;

    @JsonInclude
    private Direction direction = Direction.In;

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

    /**
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
}
