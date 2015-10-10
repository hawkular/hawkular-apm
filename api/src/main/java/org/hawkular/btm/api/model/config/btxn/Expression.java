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
package org.hawkular.btm.api.model.config.btxn;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This abstract class represents the base type for all expressions.
 *
 * @author gbrown
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = FreeForm.class), @Type(value = Literal.class),
    @Type(value = Text.class), @Type(value = XML.class) })
@ApiModel(subTypes = { FreeForm.class, Literal.class, Text.class, XML.class },
        discriminator = "type")
public abstract class Expression {

    /**
     * This method returns the textual representation of the predicate.
     *
     * @return The text representation of the predicate
     */
    public abstract String predicateText();

    /**
     * This method returns the textual representation of the expression.
     *
     * @return The text representation of the expression
     */
    public abstract String evaluateText();

}
