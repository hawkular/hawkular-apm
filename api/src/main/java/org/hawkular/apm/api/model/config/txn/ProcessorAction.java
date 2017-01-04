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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.annotations.ApiModel;

/**
 * This class represents a processing action to be performed on information associated
 * with an instrumentation location.
 *
 * @author gbrown
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "actionType")
@JsonSubTypes({ @Type(value = SetPropertyAction.class, name = "SetProperty"),
        @Type(value = AddContentAction.class, name = "AddContent"),
        @Type(value = AddCorrelationIdAction.class, name = "AddCorrelationId"),
        @Type(value = EvaluateURIAction.class, name = "EvaluateURI") })
@ApiModel(subTypes = { SetPropertyAction.class, AddContentAction.class, AddCorrelationIdAction.class,
        EvaluateURIAction.class },
        discriminator = "actionType")
public abstract class ProcessorAction {

    @JsonInclude(Include.NON_NULL)
    private String description;

    @JsonInclude(Include.NON_NULL)
    private Expression predicate;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the predicate
     */
    public Expression getPredicate() {
        return predicate;
    }

    /**
     * @param predicate the predicate to set
     */
    public void setPredicate(Expression predicate) {
        this.predicate = predicate;
    }

}
