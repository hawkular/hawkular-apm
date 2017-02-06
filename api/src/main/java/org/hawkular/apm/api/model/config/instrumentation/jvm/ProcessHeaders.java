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
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This type represents processing of a request/in or response/out headers.
 *
 * @author gbrown
 */
public class ProcessHeaders extends InstrumentAction {

    @JsonInclude
    private String headersExpression;

    @JsonInclude(Include.NON_EMPTY)
    private String originalType;

    @JsonInclude
    private Direction direction = Direction.In;

    /**
     * @return the headersExpression
     */
    public String getHeadersExpression() {
        return headersExpression;
    }

    /**
     * @param headersExpression the headersExpression to set
     */
    public void setHeadersExpression(String headersExpression) {
        this.headersExpression = headersExpression;
    }

    /**
     * @return the originalType
     */
    public String getOriginalType() {
        return originalType;
    }

    /**
     * @param originalType the originalType to set
     */
    public void setOriginalType(String originalType) {
        this.originalType = originalType;
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
