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

import org.hawkular.apm.api.model.Severity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a message returned following validation of a
 * transaction config.
 *
 * @author gbrown
 */
public class ConfigMessage {

    @JsonInclude
    private Severity severity = Severity.Error;

    @JsonInclude
    private String message;

    @JsonInclude(Include.NON_NULL)
    private String processor;

    @JsonInclude(Include.NON_NULL)
    private String action;

    @JsonInclude(Include.NON_NULL)
    private String field;

    /**
     * @return the severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @param severity the severity to set
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the processor
     */
    public String getProcessor() {
        return processor;
    }

    /**
     * @param processor the processor to set
     */
    public void setProcessor(String processor) {
        this.processor = processor;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public void setField(String field) {
        this.field = field;
    }

    @Override
    public String toString() {
        return "ConfigMessage [severity=" + severity + ", message=" + message + ", processor=" + processor
                + ", action=" + action + ", field=" + field + "]";
    }

}
