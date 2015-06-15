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
public abstract class CollectorAction extends InstrumentAction {

    @JsonInclude
    private String headersExpression;

    @JsonInclude
    private List<String> valueExpressions = new ArrayList<String>();

    @JsonInclude
    private Direction direction=Direction.Request;

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
     * @return the valueExpressions
     */
    public List<String> getValueExpressions() {
        return valueExpressions;
    }

    /**
     * @param valueExpressions the valueExpressions to set
     */
    public void setValueExpressions(List<String> valueExpressions) {
        this.valueExpressions = valueExpressions;
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

    /**
     * This enum represents whether the action is associated with a request
     * or response.
     *
     * @author gbrown
     */
    public enum Direction {
        Request,
        Response
    }
}
