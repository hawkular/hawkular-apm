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
package org.hawkular.apm.api.model.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents summary statistics for nodes.
 *
 * @author gbrown
 */
public class NodeSummaryStatistics {

    /**
     * Actual time in microseconds
     */
    @JsonInclude
    private long actual;

    /**
     * Elapsed time in microseconds
     */
    @JsonInclude
    private long elapsed;

    @JsonInclude
    private long count;

    @JsonInclude
    private String componentType;

    @JsonInclude
    private String uri;

    @JsonInclude(Include.NON_EMPTY)
    private String operation;

    /**
     * @return the actual
     */
    public long getActual() {
        return actual;
    }

    /**
     * @param actual the actual to set
     */
    public void setActual(long actual) {
        this.actual = actual;
    }

    /**
     * @return the elapsed
     */
    public long getElapsed() {
        return elapsed;
    }

    /**
     * @param elapsed the elapsed to set
     */
    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * @return the componentType
     */
    public String getComponentType() {
        return componentType;
    }

    /**
     * @param componentType the componentType to set
     */
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "NodeSummaryStatistics [actual=" + actual + ", elapsed=" + elapsed + ", count=" + count
                + ", componentType=" + componentType + ", uri=" + uri + ", operation=" + operation + "]";
    }

}
