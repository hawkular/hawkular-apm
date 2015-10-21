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

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This class represents an expression applied to a data source.
 *
 * @author gbrown
 */
public abstract class DataExpression extends Expression {

    @JsonInclude
    private DataSource source;

    @JsonInclude
    private String key;

    /**
     * @return the source
     */
    public DataSource getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(DataSource source) {
        this.source = source;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * This method returns the text related to accessing
     * the data source.
     *
     * @return The data source text
     */
    protected String dataSourceText() {
        StringBuffer buf = new StringBuffer();
        if (getSource() == DataSource.Content) {
            buf.append("values[");
            buf.append(getKey());
            buf.append("]");
        } else if (getSource() == DataSource.Header) {
            buf.append("headers.get(\"");
            buf.append(getKey());
            buf.append("\")");
        }
        return buf.toString();
    }

}
