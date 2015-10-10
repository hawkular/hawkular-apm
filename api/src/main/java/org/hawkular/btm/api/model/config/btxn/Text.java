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

/**
 * This class represents expression that can be applied to Text data.
 *
 * @author gbrown
 */
public class Text extends DataExpression {

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.model.config.btxn.Expression#predicateText()
     */
    @Override
    public String predicateText() {
        throw new IllegalStateException("Text expression should not be used for predicate");
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.model.config.btxn.Expression#expressionText()
     */
    @Override
    public String evaluateText() {
        StringBuffer buf = new StringBuffer();
        buf.append("Text.serialize(");
        buf.append(dataSourceText());
        buf.append(")");
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Text [getSource()=" + getSource() + ", getKey()=" + getKey()
                + "]";
    }

}
