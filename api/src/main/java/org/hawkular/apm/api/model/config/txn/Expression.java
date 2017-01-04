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
package org.hawkular.apm.api.model.config.txn;

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
@JsonSubTypes({ @Type(value = LiteralExpression.class, name = "Literal"),
    @Type(value = TextExpression.class, name = "Text"),
    @Type(value = XMLExpression.class, name = "XML"),
    @Type(value = JSONExpression.class, name = "JSON") })
@ApiModel(subTypes = { LiteralExpression.class, TextExpression.class,
        XMLExpression.class, JSONExpression.class },
        discriminator = "type")
public abstract class Expression {

}
