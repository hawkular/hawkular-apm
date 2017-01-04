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
package org.hawkular.apm.instrumenter;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class to help build a list of parameters.
 *
 * @author gbrown
 */
public class ArrayBuilder {

    private List<Object> parameters = new ArrayList<Object>();

    /**
     * The default constructor.
     */
    protected ArrayBuilder() {
    }

    /**
     * This method creates a new parameter array builder.
     *
     * @return The parameter array builder
     */
    public static ArrayBuilder create() {
        return (new ArrayBuilder());
    }

    /**
     * This method returns the list of parameters.
     *
     * @return The parameters
     */
    public Object[] get() {
        return parameters.toArray();
    }

    /**
     * This method adds a parameter to the list.
     *
     * @param parameter The parameter to add
     * @return The builder
     */
    public ArrayBuilder add(Object parameter) {
        parameters.add(parameter);
        return this;
    }
}
